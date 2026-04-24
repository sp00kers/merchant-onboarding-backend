package com.merchantonboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.merchantonboarding.dto.UserDTO;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.Permission;
import com.merchantonboarding.model.Role;
import com.merchantonboarding.model.User;
import com.merchantonboarding.repository.RoleRepository;
import com.merchantonboarding.repository.UserRepository;

// Unit test for UserService using Mockito — tests user CRUD, Spring Security integration, and business rules
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        testPermission = new Permission();
        testPermission.setId("case_creation");
        testPermission.setName("Case Creation");
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
        testUser.setPhone("0121234567");
    }

    // ─── loadUserByUsername() ──────────────────────────────

    // Tests loading a user by email for Spring Security authentication
    // Verifies that both the ROLE_ authority (e.g., ROLE_ONBOARDING_OFFICER) and 
    // individual permission authorities (e.g., CASE_CREATION) are included
    @Test
    void loadUserByUsername_Found() {
        when(userRepository.findByEmail("john.doe@bank.com")).thenReturn(Optional.of(testUser));

        UserDetails details = userService.loadUserByUsername("john.doe@bank.com");

        assertNotNull(details);
        assertEquals("john.doe@bank.com", details.getUsername());
        // Should have ROLE_ authority + permission authorities
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ONBOARDING_OFFICER")));
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("CASE_CREATION")));
    }

    // Tests that looking up a non-existent email throws UsernameNotFoundException
    // This is required by Spring Security's UserDetailsService contract
    @Test
    void loadUserByUsername_NotFound() {
        when(userRepository.findByEmail("unknown@bank.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("unknown@bank.com"));
    }

    // ─── createUser() ──────────────────────────────────────

    // Tests creating a new user: verifies password is encoded (hashed) before saving,
    // and the correct role is assigned from the database
    @Test
    void createUser_Success() {
        UserDTO dto = new UserDTO();
        dto.setName("New User");
        dto.setEmail("new.user@bank.com");
        dto.setPassword("password123");
        dto.setRoleId("onboarding_officer");

        when(userRepository.findByEmail("new.user@bank.com")).thenReturn(Optional.empty());
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.createUser(dto);

        assertNotNull(result);
        assertEquals("New User", result.getName());
        assertEquals("new.user@bank.com", result.getEmail());
        verify(passwordEncoder).encode("password123");
    }

    // Tests that creating a user with a non-bank email domain (e.g., @gmail.com) is rejected
    // Security rule: only bank employees with @bank.com emails can be registered
    @Test
    void createUser_InvalidEmailDomain() {
        UserDTO dto = new UserDTO();
        dto.setEmail("user@gmail.com");

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(dto));
        verify(userRepository, never()).save(any());
    }

    // Tests that creating a user with an email that already exists is blocked
    @Test
    void createUser_DuplicateEmail() {
        UserDTO dto = new UserDTO();
        dto.setEmail("john.doe@bank.com");

        when(userRepository.findByEmail("john.doe@bank.com")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.createUser(dto));
        assertEquals("Email already exists", ex.getMessage());
    }

    // ─── updateUser() ──────────────────────────────────────

    // Tests updating user details (name, department, phone) successfully
    @Test
    void updateUser_Success() {
        UserDTO dto = new UserDTO();
        dto.setName("Updated Name");
        dto.setEmail("john.doe@bank.com");
        dto.setDepartment("IT");
        dto.setPhone("0129999999");
        dto.setStatus("active");
        dto.setRoleId("onboarding_officer");

        when(userRepository.findById("USR001")).thenReturn(Optional.of(testUser));
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.updateUser("USR001", dto);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
    }

    // Tests that changing a user's email to one already used by another user is blocked
    @Test
    void updateUser_EmailChangedToDuplicate() {
        User otherUser = new User();
        otherUser.setId("USR002");
        otherUser.setEmail("other@bank.com");

        UserDTO dto = new UserDTO();
        dto.setName("John Doe");
        dto.setEmail("other@bank.com"); // changing to existing email

        when(userRepository.findById("USR001")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("other@bank.com")).thenReturn(Optional.of(otherUser));

        assertThrows(RuntimeException.class, () -> userService.updateUser("USR001", dto));
    }

    // ─── deleteUser() ──────────────────────────────────────

    // Tests successful user deletion by ID
    @Test
    void deleteUser_Success() {
        when(userRepository.existsById("USR001")).thenReturn(true);

        assertDoesNotThrow(() -> userService.deleteUser("USR001"));
        verify(userRepository).deleteById("USR001");
    }

    // Tests that deleting a non-existent user throws ResourceNotFoundException
    @Test
    void deleteUser_NotFound() {
        when(userRepository.existsById("NONEXISTENT")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUser("NONEXISTENT"));
    }

    // ─── toggleUserStatus() ──────────────────────────────────

    // Tests toggling an active user to inactive status (deactivation)
    @Test
    void toggleUserStatus_ActiveToInactive() {
        testUser.setStatus("active");
        when(userRepository.findById("USR001")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.toggleUserStatus("USR001");

        assertEquals("inactive", result.getStatus());
    }

    // Tests toggling an inactive user back to active status (reactivation)
    @Test
    void toggleUserStatus_InactiveToActive() {
        testUser.setStatus("inactive");
        when(userRepository.findById("USR001")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.toggleUserStatus("USR001");

        assertEquals("active", result.getStatus());
    }

    // Tests that admin users CANNOT be deactivated — business rule to prevent locking out all admins
    @Test
    void toggleUserStatus_AdminCannotBeDeactivated() {
        Role adminRole = new Role();
        adminRole.setId("admin");
        adminRole.setName("System Administrator");
        testUser.setRole(adminRole);
        testUser.setStatus("active");

        when(userRepository.findById("USR001")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.toggleUserStatus("USR001"));
        assertTrue(ex.getMessage().contains("Admin"));
    }

    // ─── getUsersByRole() & searchUsers() ──────────────────

    // Tests filtering users by their assigned role (e.g., get all onboarding officers)
    @Test
    void getUsersByRole_ReturnsList() {
        when(userRepository.findUsersByRole("onboarding_officer")).thenReturn(List.of(testUser));

        List<UserDTO> result = userService.getUsersByRole("onboarding_officer");

        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
    }

    // Tests searching users by keyword (searches across name, email, department)
    @Test
    void searchUsers_ByKeyword() {
        when(userRepository.searchUsers("John")).thenReturn(List.of(testUser));

        List<UserDTO> result = userService.searchUsers("John");

        assertEquals(1, result.size());
    }
}
