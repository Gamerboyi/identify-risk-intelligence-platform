package com.vedant.eurds.service;

import com.vedant.eurds.dto.AuthResponse;
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

import java.util.HashMap;
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

    @Value("${security.max-failed-attempts}")
    private int maxFailedAttempts;

    // ============================================================
    // REGISTER
    // ============================================================
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Check if username already taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(
                    "Username '" + request.getUsername() + "' is already taken");
        }

        // Check if email already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Email '" + request.getEmail() + "' is already registered");
        }

        // Get default role — every new user gets ROLE_USER
        var userRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        // Build user entity
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .failedAttemptCount(0)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        // Save to database
        User savedUser = userRepository.save(user);

        // Log audit event
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
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        // Find user — throws UsernameNotFoundException if not found
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            logAuditEvent("LOCKED_ACCOUNT_LOGIN_ATTEMPT", user.getId(), null,
                    Map.of("username", user.getUsername()));
            throw new AccountLockedException("Account is locked due to too many failed attempts");
        }

        // Verify password
        boolean passwordCorrect = passwordEncoder.matches(
                request.getPassword(), user.getPasswordHash());

        if (!passwordCorrect) {
            // Handle failed login
            handleFailedLogin(user, httpRequest);
            throw new BadCredentialsException("Invalid credentials");
        }

        // ---- Successful login ----

        // Reset failed attempts
        user.setFailedAttemptCount(0);
        userRepository.save(user);

        // Log the login attempt
        String ipAddress = getClientIp(httpRequest);
        String deviceInfo = httpRequest.getHeader("User-Agent");

        LoginLog loginLog = LoginLog.builder()
                .userId(user.getId())
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .successFlag(true)
                .riskScore(0) // Will be updated by ML service in Phase 4
                .build();

        loginLogRepository.save(loginLog);

        // Log audit event
        logAuditEvent("LOGIN_SUCCESS", user.getId(), null,
                Map.of("ip", ipAddress, "device", deviceInfo != null ? deviceInfo : "unknown"));

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        log.info("User logged in successfully: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration())
                .username(user.getUsername())
                .riskLevel("NORMAL")
                .riskScore(0)
                .message("Login successful")
                .build();
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private void handleFailedLogin(User user, HttpServletRequest httpRequest) {
        // Increment failed attempts
        int attempts = user.getFailedAttemptCount() + 1;
        user.setFailedAttemptCount(attempts);

        // Lock account if threshold reached
        if (attempts >= maxFailedAttempts) {
            user.setAccountLocked(true);
            logAuditEvent("ACCOUNT_LOCKED", user.getId(), null,
                    Map.of("reason", "Max failed attempts reached", "attempts", attempts));
            log.warn("Account locked for user: {}", user.getUsername());
        }

        userRepository.save(user);

        // Log failed attempt
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