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
        testPermission.setId("case_view");
        testPermission.setName("Case View");
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

    @Test
    void getAllRoles_ReturnsList() {
        when(roleRepository.findAll()).thenReturn(List.of(testRole));

        List<RoleDTO> result = roleService.getAllRoles();

        assertEquals(1, result.size());
        assertEquals("Onboarding Officer", result.get(0).getName());
    }

    @Test
    void getActiveRoles_ReturnsActiveOnly() {
        when(roleRepository.findByIsActiveTrue()).thenReturn(List.of(testRole));

        List<RoleDTO> result = roleService.getActiveRoles();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void getRoleById_Found() {
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));

        RoleDTO result = roleService.getRoleById("onboarding_officer");

        assertEquals("onboarding_officer", result.getId());
        assertEquals("Onboarding Officer", result.getName());
    }

    @Test
    void getRoleById_NotFound() {
        when(roleRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.getRoleById("nonexistent"));
    }

    @Test
    void createRole_Success() {
        RoleDTO dto = new RoleDTO();
        dto.setId("new_role");
        dto.setName("New Role");
        dto.setDescription("A new role");
        dto.setActive(true);
        dto.setPermissions(Set.of("case_view"));

        when(permissionRepository.findById("case_view")).thenReturn(Optional.of(testPermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleDTO result = roleService.createRole(dto);

        assertNotNull(result);
        assertEquals("New Role", result.getName());
        assertTrue(result.getPermissions().contains("case_view"));
    }

    @Test
    void updateRole_Success() {
        RoleDTO dto = new RoleDTO();
        dto.setName("Updated Officer");
        dto.setDescription("Updated description");
        dto.setActive(true);
        dto.setPermissions(Set.of("case_view"));

        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));
        when(permissionRepository.findById("case_view")).thenReturn(Optional.of(testPermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleDTO result = roleService.updateRole("onboarding_officer", dto);

        assertEquals("Updated Officer", result.getName());
    }

    @Test
    void deleteRole_Success() {
        when(roleRepository.existsById("onboarding_officer")).thenReturn(true);

        assertDoesNotThrow(() -> roleService.deleteRole("onboarding_officer"));
        verify(roleRepository).deleteById("onboarding_officer");
    }

    @Test
    void deleteRole_NotFound() {
        when(roleRepository.existsById("nonexistent")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.deleteRole("nonexistent"));
    }

    // ─── userHasPermission() ──────────────────────────────────

    @Test
    void userHasPermission_AdminHasAll() {
        Role adminRole = new Role();
        adminRole.setId("admin");
        adminRole.setPermissions(Set.of());
        when(roleRepository.findById("admin")).thenReturn(Optional.of(adminRole));

        assertTrue(roleService.userHasPermission("admin", "any_permission"));
    }

    @Test
    void userHasPermission_HasSpecificPermission() {
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));

        assertTrue(roleService.userHasPermission("onboarding_officer", "case_view"));
    }

    @Test
    void userHasPermission_DoesNotHavePermission() {
        when(roleRepository.findById("onboarding_officer")).thenReturn(Optional.of(testRole));

        assertFalse(roleService.userHasPermission("onboarding_officer", "user_management"));
    }

    @Test
    void userHasPermission_AllModules() {
        Role superRole = new Role();
        superRole.setId("super_user");
        superRole.setPermissions(Set.of(allModulesPermission));
        when(roleRepository.findById("super_user")).thenReturn(Optional.of(superRole));

        assertTrue(roleService.userHasPermission("super_user", "any_permission_id"));
    }

    @Test
    void userHasPermission_RoleNotFound() {
        when(roleRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertFalse(roleService.userHasPermission("nonexistent", "case_view"));
    }

    // ─── Permission CRUD ──────────────────────────────────────

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

    @Test
    void updatePermission_Success() {
        PermissionDTO dto = new PermissionDTO();
        dto.setName("Updated Permission");
        dto.setDescription("Updated desc");
        dto.setCategory("cases");
        dto.setActive(true);

        when(permissionRepository.findById("case_view")).thenReturn(Optional.of(testPermission));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        PermissionDTO result = roleService.updatePermission("case_view", dto);

        assertEquals("Updated Permission", result.getName());
    }

    @Test
    void deletePermission_Success() {
        when(permissionRepository.existsById("case_view")).thenReturn(true);

        assertDoesNotThrow(() -> roleService.deletePermission("case_view"));
        verify(permissionRepository).deleteById("case_view");
    }

    @Test
    void deletePermission_NotFound() {
        when(permissionRepository.existsById("nonexistent")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> roleService.deletePermission("nonexistent"));
    }

    @Test
    void getPermissionsByCategory_ReturnsList() {
        when(permissionRepository.findByCategory("cases")).thenReturn(List.of(testPermission));

        List<PermissionDTO> result = roleService.getPermissionsByCategory("cases");

        assertEquals(1, result.size());
        assertEquals("case_view", result.get(0).getId());
    }

    @Test
    void getActivePermissions_ReturnsActiveOnly() {
        when(permissionRepository.findByIsActiveTrue()).thenReturn(List.of(testPermission));

        List<PermissionDTO> result = roleService.getActivePermissions();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }
}
