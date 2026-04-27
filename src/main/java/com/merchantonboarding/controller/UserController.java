package com.merchantonboarding.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.dto.UserDTO;
import com.merchantonboarding.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current authenticated user's fresh data (for dynamic permission refresh)
     * Any authenticated user can access their own data
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserDTO user = userService.getCurrentUser(email);
        return ResponseEntity.ok(user);
    }

    /**
     * Get all users as list (for frontend compatibility)
     * Requires USER_MANAGEMENT permission
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsersAsList();
        return ResponseEntity.ok(users);
    }

    /**
     * Get all users with pagination
     * Requires USER_MANAGEMENT permission
     */
    @GetMapping("/paged")
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Page<UserDTO>> getAllUsersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserDTO> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * Create new user
     * Requires USER_MANAGEMENT permission
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        UserDTO createdUser = userService.createUser(userDTO);
        return ResponseEntity.ok(createdUser);
    }

    /**
     * Get user by ID
     * Requires USER_MANAGEMENT permission
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Update user
     * Requires USER_MANAGEMENT permission
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable String id, @Valid @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Delete user
     * Requires USER_MANAGEMENT permission
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get users by role
     * Requires USER_MANAGEMENT, CASE_CREATION, CASE_MANAGEMENT, or ALL_MODULES permission
     */
    @GetMapping("/by-role/{roleId}")
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('CASE_CREATION') or hasAuthority('CASE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String roleId) {
        List<UserDTO> users = userService.getUsersByRole(roleId);
        return ResponseEntity.ok(users);
    }

    /**
     * Search users
     * Requires USER_MANAGEMENT permission
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String keyword) {
        List<UserDTO> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    /**
     * Toggle user status (active/inactive)
     * Admins cannot be deactivated
     * Requires USER_MANAGEMENT permission
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('USER_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<UserDTO> toggleUserStatus(@PathVariable String id) {
        UserDTO updatedUser = userService.toggleUserStatus(id);
        return ResponseEntity.ok(updatedUser);
    }
}
