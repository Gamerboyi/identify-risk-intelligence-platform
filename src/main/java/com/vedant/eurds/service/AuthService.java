package com.vedant.eurds.service;

import com.vedant.eurds.dto.AuthResponse;
import com.vedant.eurds.dto.LoginFeatureDTO;
import com.vedant.eurds.dto.LoginRequest;
import com.vedant.eurds.dto.LoginResponse;
import com.vedant.eurds.dto.RegisterRequest;
import com.vedant.eurds.exception.AccountLockedException;
import com.vedant.eurds.exception.UserAlreadyExistsException;
import com.vedant.eurds.model.AuditEvent;
import com.vedant.eurds.model.LoginLog;
import com.vedant.eurds.model.User;
import com.vedant.eurds.repository.AuditEventRepository;
import com.vedant.eurds.repository.LoginLogRepository;
import com.vedant.eurds.repository.RoleRepository;
import com.vedant.eurds.repository.UserRepository;
import com.vedant.eurds.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final LoginLogRepository loginLogRepository;
        private final AuditEventRepository auditEventRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final UserDetailsService userDetailsService;
        private final FeatureEngineeringService featureEngineeringService;
        private final MlService mlService;

        @Value("${security.max-failed-attempts}")
        private int maxFailedAttempts;

        // ============================================================
        // REGISTER
        // ============================================================
        @Transactional
        public AuthResponse register(RegisterRequest request) {

                if (userRepository.existsByUsername(request.getUsername())) {
                        throw new UserAlreadyExistsException(
                                        "Username '" + request.getUsername() + "' is already taken");
                }

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new UserAlreadyExistsException(
                                        "Email '" + request.getEmail() + "' is already registered");
                }

                var userRole = roleRepository.findByRoleName("ROLE_USER")
                                .orElseThrow(() -> new RuntimeException("Default role not found"));

                User user = User.builder()
                                .username(request.getUsername())
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .accountLocked(false)
                                .failedAttemptCount(0)
                                .roles(new HashSet<>(Set.of(userRole)))
                                .build();

                User savedUser = userRepository.save(user);

                logAuditEvent("USER_REGISTERED", savedUser.getId(), null,
                                Map.of("username", savedUser.getUsername(),
                                                "email", savedUser.getEmail()));

                log.info("New user registered: {}", savedUser.getUsername());

                return AuthResponse.builder()
                                .success(true)
                                .message("Registration successful")
                                .username(savedUser.getUsername())
                                .email(savedUser.getEmail())
                                .build();
        }

        // ============================================================
        // LOGIN
        // ============================================================
        @Transactional(noRollbackFor = { BadCredentialsException.class, AccountLockedException.class })
        public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
                User user = userRepository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

                if (user.isAccountLocked()) {
                        logAuditEvent("LOCKED_ACCOUNT_LOGIN_ATTEMPT", user.getId(), null,
                                        Map.of("username", user.getUsername()));
                        throw new AccountLockedException("Account is locked due to too many failed attempts");
                }

                boolean passwordCorrect = passwordEncoder.matches(
                                request.getPassword(), user.getPasswordHash());

                if (!passwordCorrect) {
                        handleFailedLogin(user, httpRequest);
                        throw new BadCredentialsException("Invalid credentials");
                }

                // Successful login
                user.setFailedAttemptCount(0);
                userRepository.save(user);

                String ipAddress = getClientIp(httpRequest);
                String deviceInfo = httpRequest.getHeader("User-Agent");

                // Generate JWT token
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
                String token = jwtUtil.generateToken(userDetails);

                // Extract features BEFORE saving login log
                LoginFeatureDTO features = featureEngineeringService.extractFeatures(
                                user, ipAddress, deviceInfo != null ? deviceInfo : "unknown");

                // Call ML service for risk prediction
                MlService.MlPrediction prediction = mlService.predict(features);
                int riskScore = prediction.getRiskScore();
                String riskLevel = prediction.getRiskLevel();

                // Save login log with real risk score
                LoginLog loginLog = LoginLog.builder()
                                .userId(user.getId())
                                .ipAddress(ipAddress)
                                .deviceInfo(deviceInfo)
                                .successFlag(true)
                                .riskScore(riskScore)
                                .build();
                loginLogRepository.save(loginLog);

                // Log audit event
                logAuditEvent("LOGIN_SUCCESS", user.getId(), null,
                                Map.of("ip", ipAddress, "device", deviceInfo != null ? deviceInfo : "unknown"));

                // Log high risk logins
                if (riskScore >= 70) {
                        logAuditEvent("HIGH_RISK_LOGIN_DETECTED", user.getId(), null,
                                        Map.of("riskScore", riskScore, "ip", ipAddress, "newIp",
                                                        features.getIsNewIp()));
                        log.warn("High risk login for user: {} score: {}", user.getUsername(), riskScore);
                }

                log.info("User logged in successfully: {} risk={} model={}",
                                user.getUsername(), riskScore, prediction.getModelUsed());

                return LoginResponse.builder()
                                .token(token)
                                .tokenType("Bearer")
                                .expiresIn(jwtUtil.getExpiration())
                                .username(user.getUsername())
                                .riskLevel(riskLevel)
                                .riskScore(riskScore)
                                .message("Login successful")
                                .build();
        }

        // ============================================================
        // PRIVATE HELPERS
        // ============================================================
        private void handleFailedLogin(User user, HttpServletRequest httpRequest) {
                userRepository.incrementFailedAttempts(user.getUsername());

                // Read the count directly from DB after increment — avoids race condition
                int attempts = userRepository.getFailedAttemptCount(user.getUsername());
                log.info("Failed login attempt {} of {} for user: {}", attempts, maxFailedAttempts, user.getUsername());

                if (attempts >= maxFailedAttempts) {
                        userRepository.lockAccount(user.getUsername());
                        logAuditEvent("ACCOUNT_LOCKED", user.getId(), null,
                                        Map.of("reason", "Max failed attempts reached", "attempts", attempts));
                        log.warn("Account locked for user: {}", user.getUsername());
                }

                LoginLog failLog = LoginLog.builder()
                                .userId(user.getId())
                                .ipAddress(getClientIp(httpRequest))
                                .deviceInfo(httpRequest.getHeader("User-Agent"))
                                .successFlag(false)
                                .riskScore(null)
                                .build();
                loginLogRepository.save(failLog);

                logAuditEvent("LOGIN_FAILURE", user.getId(), null,
                                Map.of("attempts", attempts, "ip", getClientIp(httpRequest)));
        }

        private void logAuditEvent(String eventType, java.util.UUID actorId,
                        java.util.UUID targetId, Map<String, Object> data) {
                AuditEvent event = AuditEvent.builder()
                                .eventType(eventType)
                                .actorId(actorId)
                                .targetId(targetId)
                                .eventData(data)
                                .build();
                auditEventRepository.save(event);
        }

        private String getClientIp(HttpServletRequest request) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                        return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
        }
}