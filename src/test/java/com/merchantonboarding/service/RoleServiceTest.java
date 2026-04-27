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

import com.merchantonboarding.dto.PermissionDTO;
import com.merchantonboarding.dto.RoleDTO;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.Permission;
import com.merchantonboarding.model.Role;
import com.merchantonboarding.repository.PermissionRepository;
import com.merchantonboarding.repository.RoleRepository;

// Unit test for RoleService — tests role CRUD, permission CRUD, and the permission-checking logic
// that controls what each user role can access in the system
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleService roleService;

    private Role testRole;
    private Permission testPermission;
    private Permission allModulesPermission;

    @BeforeEach
    void setUp() {
        testPermission = new Permission();
        testPermission.setId("case_creation");
        testPermission.setName("Case Creation");
        testPermission.setCategory("cases");
        testPermission.setActive(true);

        allModulesPermission = new Permission();
        allModulesPermission.setId("all_modules");
        allModulesPermission.setName("All Modules");
        allModulesPermission.setCategory("system");
        allModulesPermission.setActive(true);

        testRole = new Role();
        testRole.setId("onboarding_officer");
        testRole.setName("Onboarding Officer");
        testRole.setDescription("Handles case creation and management");
        testRole.setActive(true);
        testRole.setPermissions(Set.of(testPermission));
    }

    // ─── Role CRUD ──────────────────────────────────────────

    // Tests retrieving all roles from the database
    @Test
    void getAllRoles_ReturnsList() {
        when(roleRepository.findAll()).thenReturn(List.of(testRole));

        List<RoleDTO> result = roleService.getAllRoles();

        assertEquals(1, result.size());
        assertEquals("Onboarding Officer", result.get(0).getName());
    }

    // Tests that only active roles are returned (inactive roles are filtered out)
    @Test
    void getActiveRoles_ReturnsActiveOnly() {
        when(roleRepository.findByIsActiveTrue()).thenReturn(List.of(testRole));

        List<RoleDTO> result = roleService.getActiveRoles();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // Tests getting a role by ID — returns the role DTO with correct fields
    @Test
    void getRoleById_Found() {
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));

        RoleDTO result = roleService.getRoleById("onboarding_officer");

        assertEquals("onboarding_officer", result.getId());
        assertEquals("Onboarding Officer", result.getName());
    }

    // Tests that looking up a non-existent role ID throws ResourceNotFoundException
    @Test
    void getRoleById_NotFound() {
        when(roleRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.getRoleById("nonexistent"));
    }

    // Tests creating a new role with assigned permissions — verifies the role is saved with correct permission set
    @Test
    void createRole_Success() {
        RoleDTO dto = new RoleDTO();
        dto.setId("new_role");
        dto.setName("New Role");
        dto.setDescription("A new role");
        dto.setActive(true);
        dto.setPermissions(Set.of("case_creation"));

        when(permissionRepository.findById("case_creation")).thenReturn(Optional.of(testPermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleDTO result = roleService.createRole(dto);

        assertNotNull(result);
        assertEquals("New Role", result.getName());
        assertTrue(result.getPermissions().contains("case_creation"));
    }

    // Tests updating an existing role's name, description, and permissions
    @Test
    void updateRole_Success() {
        RoleDTO dto = new RoleDTO();
        dto.setName("Updated Officer");
        dto.setDescription("Updated description");
        dto.setActive(true);
        dto.setPermissions(Set.of("case_creation"));

        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));
        when(permissionRepository.findById("case_creation")).thenReturn(Optional.of(testPermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleDTO result = roleService.updateRole("onboarding_officer", dto);

        assertEquals("Updated Officer", result.getName());
    }

    // Tests successful deletion of a role by ID
    @Test
    void deleteRole_Success() {
        when(roleRepository.existsById("onboarding_officer")).thenReturn(true);

        assertDoesNotThrow(() -> roleService.deleteRole("onboarding_officer"));
        verify(roleRepository).deleteById("onboarding_officer");
    }

    // Tests that deleting a non-existent role throws ResourceNotFoundException
    @Test
    void deleteRole_NotFound() {
        when(roleRepository.existsById("nonexistent")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.deleteRole("nonexistent"));
    }

    // ─── userHasPermission() ──────────────────────────────────

    // Tests that the "admin" role automatically has ALL permissions regardless of what's assigned
    // This is a hardcoded business rule — admins can do everything
    @Test
    void userHasPermission_AdminHasAll() {
        Role adminRole = new Role();
        adminRole.setId("admin");
        adminRole.setPermissions(Set.of());
        when(roleRepository.findById("admin")).thenReturn(Optional.of(adminRole));

        assertTrue(roleService.userHasPermission("admin", "any_permission"));
    }

    // Tests that a role with a specific permission (case_creation) returns true when checked
    @Test
    void userHasPermission_HasSpecificPermission() {
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));

        assertTrue(roleService.userHasPermission("onboarding_officer", "case_creation"));
    }

    // Tests that checking for a permission the role doesn't have returns false
    @Test
    void userHasPermission_DoesNotHavePermission() {
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));

        assertFalse(roleService.userHasPermission("onboarding_officer", "user_management"));
    }

    // Tests that a role with the "all_modules" permission acts as a wildcard — has access to everything
    @Test
    void userHasPermission_AllModules() {
        Role superRole = new Role();
        superRole.setId("super_user");
        superRole.setPermissions(Set.of(allModulesPermission));
        when(roleRepository.findById("super_user")).thenReturn(Optional.of(superRole));

        assertTrue(roleService.userHasPermission("super_user", "any_permission_id"));
    }

    // Tests that checking permissions for a non-existent role returns false (not an error)
    @Test
    void userHasPermission_RoleNotFound() {
        when(roleRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertFalse(roleService.userHasPermission("nonexistent", "case_creation"));
    }

    // ─── Permission CRUD ──────────────────────────────────────

    // Tests creating a new permission with ID, name, description, and category
    @Test
    void createPermission_Success() {
        PermissionDTO dto = new PermissionDTO();
        dto.setId("new_perm");
        dto.setName("New Permission");
        dto.setDescription("A new permission");
        dto.setCategory("cases");
        dto.setActive(true);

        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        PermissionDTO result = roleService.createPermission(dto);

        assertNotNull(result);
        assertEquals("New Permission", result.getName());
    }

    // Tests updating an existing permission's name, description, and category
    @Test
    void updatePermission_Success() {
        PermissionDTO dto = new PermissionDTO();
        dto.setName("Updated Permission");
        dto.setDescription("Updated desc");
        dto.setCategory("cases");
        dto.setActive(true);

        when(permissionRepository.findById("case_creation")).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        PermissionDTO result = roleService.updatePermission("case_creation", dto);

        assertEquals("Updated Permission", result.getName());
    }

    // Tests successful deletion of a permission by ID
    @Test
    void deletePermission_Success() {
        when(permissionRepository.existsById("case_creation")).thenReturn(true);

        assertDoesNotThrow(() -> roleService.deletePermission("case_creation"));
        verify(permissionRepository).deleteById("case_creation");
    }

    // Tests that deleting a non-existent permission throws ResourceNotFoundException
    @Test
    void deletePermission_NotFound() {
        when(permissionRepository.existsById("nonexistent")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.deletePermission("nonexistent"));
    }

    // Tests filtering permissions by category (e.g., "cases", "system", "users")
    @Test
    void getPermissionsByCategory_ReturnsList() {
        when(permissionRepository.findByCategory("cases")).thenReturn(List.of(testPermission));

        List<PermissionDTO> result = roleService.getPermissionsByCategory("cases");

        assertEquals(1, result.size());
        assertEquals("case_creation", result.get(0).getId());
    }

    // Tests that only active permissions are returned (inactive ones are filtered out)
    @Test
    void getActivePermissions_ReturnsActiveOnly() {
        when(permissionRepository.findByIsActiveTrue()).thenReturn(List.of(testPermission));

        List<PermissionDTO> result = roleService.getActivePermissions();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }
}
