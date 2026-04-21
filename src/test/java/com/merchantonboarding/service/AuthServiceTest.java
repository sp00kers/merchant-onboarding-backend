package com.merchantonboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.merchantonboarding.dto.AuthResponse;
import com.merchantonboarding.dto.LoginRequest;
import com.merchantonboarding.dto.UserDTO;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.Permission;
import com.merchantonboarding.model.Role;
import com.merchantonboarding.model.User;
import com.merchantonboarding.repository.RoleRepository;
import com.merchantonboarding.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role testRole;
    private Permission testPermission;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testPermission = new Permission();
        testPermission.setId("case_view");
        testPermission.setName("Case View");
        testPermission.setActive(true);

        testRole = new Role();
        testRole.setId("onboarding_officer");
        testRole.setName("Onboarding Officer");
        testRole.setActive(true);
        testRole.setPermissions(Set.of(testPermission));

        testUser = new User();
        testUser.setId("USR001");
        testUser.setName("John Doe");
        testUser.setEmail("john.doe@bank.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(testRole);
        testUser.setStatus("active");
        testUser.setDepartment("Operations");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("john.doe@bank.com");
        loginRequest.setPassword("password123");
    }

    // ─── authenticate() ─────────────────────────────────────

    @Test
    void authenticate_Success() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByEmail("john.doe@bank.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken("john.doe@bank.com")).thenReturn("jwt-token-123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse response = authService.authenticate(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token-123", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("john.doe@bank.com", response.getUser().getEmail());
        verify(auditService).logAction(eq("LOGIN_SUCCESS"), eq("User"), eq("USR001"),
                eq("USR001"), eq("john.doe@bank.com"), any(), isNull(), isNull(), eq("SUCCESS"), anyString());
    }

    @Test
    void authenticate_UserNotFound() {
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail("john.doe@bank.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.authenticate(loginRequest));
    }

    @Test
    void authenticate_DeactivatedAccount() {
        testUser.setStatus("inactive");
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail("john.doe@bank.com")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.authenticate(loginRequest));
        assertTrue(ex.getMessage().contains("deactivated"));
        verify(auditService).logAction(eq("LOGIN_FAILED"), eq("User"), eq("USR001"),
                eq("USR001"), eq("john.doe@bank.com"), any(), isNull(), isNull(), eq("FAILURE"), eq("Account deactivated"));
    }

    @Test
    void authenticate_BadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.authenticate(loginRequest));
        verify(auditService).logAction(eq("LOGIN_FAILED"), eq("User"), isNull(),
                isNull(), eq("john.doe@bank.com"), any(), isNull(), isNull(), eq("FAILURE"), anyString());
    }

    // ─── register() ─────────────────────────────────────

    @Test
    void register_Success() {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("New User");
        userDTO.setEmail("new.user@bank.com");
        userDTO.setPassword("password123");
        userDTO.setRoleId("onboarding_officer");

        when(userRepository.findByEmail("new.user@bank.com")).thenReturn(Optional.empty());
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("new.user@bank.com")).thenReturn("new-jwt-token");

        AuthResponse response = authService.register(userDTO);

        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_InvalidEmailDomain() {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("user@gmail.com");

        assertThrows(IllegalArgumentException.class, () -> authService.register(userDTO));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicateEmail() {
        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("john.doe@bank.com");

        when(userRepository.findByEmail("john.doe@bank.com")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(userDTO));
        assertEquals("Email already exists", ex.getMessage());
    }

    @Test
    void register_DefaultRole() {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("Default User");
        userDTO.setEmail("default@bank.com");
        userDTO.setPassword("password123");
        userDTO.setRoleId(null); // no role specified

        when(userRepository.findByEmail("default@bank.com")).thenReturn(Optional.empty());
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("default@bank.com")).thenReturn("token");

        AuthResponse response = authService.register(userDTO);

        assertNotNull(response);
        verify(roleRepository).findById("onboarding_officer");
    }

    // ─── refreshToken() ─────────────────────────────────────

    @Test
    void refreshToken_Valid() {
        when(jwtService.extractUsername("valid-token")).thenReturn("john.doe@bank.com");
        when(userRepository.findByEmail("john.doe@bank.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid("valid-token", "john.doe@bank.com")).thenReturn(true);
        when(jwtService.generateToken("john.doe@bank.com")).thenReturn("new-token");

        AuthResponse response = authService.refreshToken("valid-token");

        assertNotNull(response);
        assertEquals("new-token", response.getToken());
    }

    @Test
    void refreshToken_Invalid() {
        when(jwtService.extractUsername("invalid-token")).thenReturn("john.doe@bank.com");
        when(userRepository.findByEmail("john.doe@bank.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid("invalid-token", "john.doe@bank.com")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.refreshToken("invalid-token"));
    }
}
