package com.merchantonboarding.service;

import com.merchantonboarding.dto.AuthResponse;
import com.merchantonboarding.dto.LoginRequest;
import com.merchantonboarding.dto.UserDTO;
import com.merchantonboarding.dto.RoleDTO;
import com.merchantonboarding.model.User;
import com.merchantonboarding.model.Role;
import com.merchantonboarding.model.Permission;
import com.merchantonboarding.repository.UserRepository;
import com.merchantonboarding.repository.RoleRepository;
import com.merchantonboarding.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Authenticate user and return JWT token
     */
    public AuthResponse authenticate(LoginRequest loginRequest) throws AuthenticationException {
        System.out.println("=== Starting authentication for: " + loginRequest.getUsername());
        
        try {
            // Authenticate user using email as username
            System.out.println("=== Calling AuthenticationManager.authenticate()");
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            System.out.println("=== Authentication successful: " + authentication.isAuthenticated());

            // Get user details by email
            System.out.println("=== Looking up user in database");
            User user = userRepository.findByEmail(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            System.out.println("=== User found: " + user.getEmail());

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate JWT token
            System.out.println("=== Generating JWT token");
            String token = jwtService.generateToken(user.getEmail());
            System.out.println("=== Token generated: " + (token != null ? "SUCCESS" : "NULL"));

            // Convert to DTO
            System.out.println("=== Converting to DTO");
            UserDTO userDTO = convertToDTO(user);
            
            AuthResponse response = new AuthResponse(token, userDTO);
            System.out.println("=== AuthResponse created successfully");
            return response;
            
        } catch (Exception e) {
            System.out.println("=== Authentication failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Register new user
     */
    public AuthResponse register(UserDTO userDTO) {
        // Check if email already exists
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setId("USR" + String.valueOf(System.currentTimeMillis()).substring(7));
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setDepartment(userDTO.getDepartment());
        user.setPhone(userDTO.getPhone());
        user.setStatus("active");
        user.setNotes(userDTO.getNotes());

        // Set role
        if (userDTO.getRoleId() != null) {
            Role role = roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + userDTO.getRoleId()));
            user.setRole(role);
        } else {
            // Default to onboarding_officer role
            Role defaultRole = roleRepository.findById("onboarding_officer")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));
            user.setRole(defaultRole);
        }

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtService.generateToken(savedUser.getEmail());

        // Convert to DTO
        UserDTO responseDTO = convertToDTO(savedUser);

        return new AuthResponse(token, responseDTO);
    }

    /**
     * Refresh JWT token
     */
    public AuthResponse refreshToken(String token) {
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate token
        if (!jwtService.isTokenValid(token, email)) {
            throw new RuntimeException("Invalid token");
        }

        String newToken = jwtService.generateToken(user.getEmail());
        UserDTO userDTO = convertToDTO(user);

        return new AuthResponse(newToken, userDTO);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getEmail()); // For compatibility
        dto.setRoleId(user.getRole() != null ? user.getRole().getId() : null);
        dto.setDepartment(user.getDepartment());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setLastLogin(user.getLastLogin() != null ? user.getLastLogin().format(DATETIME_FORMATTER) : "Never");
        dto.setNotes(user.getNotes());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

        // Set role details
        if (user.getRole() != null) {
            RoleDTO roleDTO = new RoleDTO();
            roleDTO.setId(user.getRole().getId());
            roleDTO.setName(user.getRole().getName());
            roleDTO.setDescription(user.getRole().getDescription());
            roleDTO.setActive(user.getRole().isActive());

            // Set permissions
            if (user.getRole().getPermissions() != null) {
                Set<String> permissionIds = user.getRole().getPermissions().stream()
                    .map(Permission::getId)
                    .collect(Collectors.toSet());
                roleDTO.setPermissions(permissionIds);
                dto.setPermissions(permissionIds);
            }
            dto.setRole(roleDTO);
        }

        return dto;
    }
}
