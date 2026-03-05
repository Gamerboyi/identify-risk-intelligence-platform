package com.vedant.eurds.service;

import com.vedant.eurds.dto.RegisterRequest;
import com.vedant.eurds.exception.UserAlreadyExistsException;
import com.vedant.eurds.model.Role;
import com.vedant.eurds.model.User;
import com.vedant.eurds.repository.AuditEventRepository;
import com.vedant.eurds.repository.LoginLogRepository;
import com.vedant.eurds.repository.RoleRepository;
import com.vedant.eurds.repository.UserRepository;
import com.vedant.eurds.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private LoginLogRepository loginLogRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FeatureEngineeringService featureEngineeringService;
    @Mock private MlService mlService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private Role userRole;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setUsername("testuser");
        validRequest.setEmail("test@test.com");
        validRequest.setPassword("password123");

        userRole = Role.builder()
                .id(2)
                .roleName("ROLE_USER")
                .build();
    }

    // ============================================================
    // REGISTER TESTS
    // ============================================================

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(auditEventRepository.save(any())).thenReturn(null);

        // Act
        var response = authService.register(validRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("testuser", response.getUsername());
        assertEquals("Registration successful", response.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(validRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(validRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_PasswordIsHashed() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedvalue");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User saved = i.getArgument(0);
            // Verify password is hashed not plain text
            assertNotEquals("password123", saved.getPasswordHash());
            assertEquals("$2a$12$hashedvalue", saved.getPasswordHash());
            return saved;
        });
        when(auditEventRepository.save(any())).thenReturn(null);

        // Act
        authService.register(validRequest);

        // Assert
        verify(passwordEncoder).encode("password123");
    }
}