package com.merchantonboarding.service;

import com.merchantonboarding.dto.AuthResponse;
import com.merchantonboarding.dto.LoginRequest;
import com.merchantonboarding.dto.UserDTO;
import com.merchantonboarding.model.User;
import com.merchantonboarding.model.Role;
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

    /**
     * Authenticate user and return JWT token
     */
    public AuthResponse authenticate(LoginRequest loginRequest) throws AuthenticationException {
        System.out.println("=== Starting authentication for: " + loginRequest.getUsername());
        
        try {
            // Authenticate user
            System.out.println("=== Calling AuthenticationManager.authenticate()");
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            System.out.println("=== Authentication successful: " + authentication.isAuthenticated());


            // Get user details
            System.out.println("=== Looking up user in database");
            User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            System.out.println("=== User found: " + user.getUsername());


            // Generate JWT token
            System.out.println("=== Generating JWT token");
            String token = jwtService.generateToken(user.getUsername());
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
        // Check if username already exists
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setFullName(userDTO.getFullName());
        user.setEmail(userDTO.getEmail());
        user.setEnabled(true);

        // Set default role if no roles specified
        Set<Role> roles;
        if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
            roles = userDTO.getRoles().stream()
                .map(roleName -> roleRepository.findByName(Role.RoleName.valueOf(roleName))
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
        } else {
            // Default to ONBOARDING_OFFICER role
            Role defaultRole = roleRepository.findByName(Role.RoleName.ONBOARDING_OFFICER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));
            roles = Set.of(defaultRole);
        }
        user.setRoles(roles);

        // Save user
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String token = jwtService.generateToken(savedUser.getUsername());

        // Convert to DTO
        UserDTO responseDTO = convertToDTO(savedUser);

        return new AuthResponse(token, responseDTO);
    }

    /**
     * Refresh JWT token
     */
    public AuthResponse refreshToken(String token) {
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate token
        if (!jwtService.isTokenValid(token, username)) {
            throw new RuntimeException("Invalid token");
        }

        String newToken = jwtService.generateToken(user.getUsername());
        UserDTO userDTO = convertToDTO(user);

        return new AuthResponse(newToken, userDTO);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setRoles(user.getRoles().stream()
            .map(role -> role.getName().toString())
            .collect(Collectors.toSet()));
        return dto;
    }
}
