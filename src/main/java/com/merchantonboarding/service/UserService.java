package com.merchantonboarding.service;

import com.merchantonboarding.dto.UserDTO;
import com.merchantonboarding.dto.RoleDTO;
import com.merchantonboarding.model.User;
import com.merchantonboarding.model.Role;
import com.merchantonboarding.model.Permission;
import com.merchantonboarding.repository.UserRepository;
import com.merchantonboarding.repository.RoleRepository;
import com.merchantonboarding.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Build authorities list with role and permissions
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

        // Add role authority (prefixed with ROLE_)
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getId().toUpperCase()));

            // Add all permissions as authorities
            if (user.getRole().getPermissions() != null) {
                user.getRole().getPermissions().forEach(permission -> {
                    authorities.add(new SimpleGrantedAuthority(permission.getId().toUpperCase()));
                });
            }
        }

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(!"active".equals(user.getStatus()))
            .credentialsExpired(false)
            .disabled(!"active".equals(user.getStatus()))
            .build();
    }

    public Page<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(this::convertToDTO);
    }

    public List<UserDTO> getAllUsersAsList() {
        return userRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public UserDTO createUser(UserDTO userDTO) {
        // Check if email already exists
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setId(userDTO.getId() != null ? userDTO.getId() : "USR" + String.valueOf(System.currentTimeMillis()).substring(7));
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword() != null ? userDTO.getPassword() : "password123"));
        user.setDepartment(userDTO.getDepartment());
        user.setPhone(userDTO.getPhone());
        user.setStatus(userDTO.getStatus() != null ? userDTO.getStatus() : "active");
        user.setNotes(userDTO.getNotes());

        // Set role
        if (userDTO.getRoleId() != null) {
            Role role = roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + userDTO.getRoleId()));
            user.setRole(role);
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    public UserDTO getUserById(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    public UserDTO updateUser(String id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        existingUser.setName(userDTO.getName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setDepartment(userDTO.getDepartment());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setStatus(userDTO.getStatus());
        existingUser.setNotes(userDTO.getNotes());

        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        // Update role
        if (userDTO.getRoleId() != null) {
            Role role = roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + userDTO.getRoleId()));
            existingUser.setRole(role);
        }

        User updatedUser = userRepository.save(existingUser);
        return convertToDTO(updatedUser);
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public List<UserDTO> getUsersByRole(String roleId) {
        List<User> users = userRepository.findUsersByRole(roleId);
        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<UserDTO> searchUsers(String keyword) {
        List<User> users = userRepository.searchUsers(keyword);
        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Toggle user status (active/inactive)
     * Admins cannot be deactivated
     */
    public UserDTO toggleUserStatus(String id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Prevent deactivating admin users
        if (user.getRole() != null && "admin".equals(user.getRole().getId()) && "active".equals(user.getStatus())) {
            throw new RuntimeException("Admin users cannot be deactivated");
        }

        // Toggle status
        String newStatus = "active".equals(user.getStatus()) ? "inactive" : "active";
        user.setStatus(newStatus);

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
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

