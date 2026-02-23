package com.merchantonboarding.controller;

import com.merchantonboarding.dto.RoleDTO;
import com.merchantonboarding.dto.PermissionDTO;
import com.merchantonboarding.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "http://localhost:4200")
public class RoleController {

    @Autowired
    private RoleService roleService;

    // ─── Role Endpoints ───────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        List<RoleDTO> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Get active roles - public endpoint for login/registration forms
     */
    @GetMapping("/active")
    public ResponseEntity<List<RoleDTO>> getActiveRoles() {
        List<RoleDTO> roles = roleService.getActiveRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable String id) {
        RoleDTO role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<RoleDTO> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        RoleDTO createdRole = roleService.createRole(roleDTO);
        return ResponseEntity.ok(createdRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable String id,
                                              @Valid @RequestBody RoleDTO roleDTO) {
        RoleDTO updatedRole = roleService.updateRole(id, roleDTO);
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roleId}/has-permission/{permissionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> checkPermission(@PathVariable String roleId,
                                                    @PathVariable String permissionId) {
        boolean hasPermission = roleService.userHasPermission(roleId, permissionId);
        return ResponseEntity.ok(hasPermission);
    }

    // ─── Permission Endpoints ───────────────────────────────────────

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        List<PermissionDTO> permissions = roleService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/permissions/active")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGEMENT') or hasAuthority('ROLE_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<PermissionDTO>> getActivePermissions() {
        List<PermissionDTO> permissions = roleService.getActivePermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/permissions/category/{category}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<List<PermissionDTO>> getPermissionsByCategory(@PathVariable String category) {
        List<PermissionDTO> permissions = roleService.getPermissionsByCategory(category);
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<PermissionDTO> createPermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        PermissionDTO createdPermission = roleService.createPermission(permissionDTO);
        return ResponseEntity.ok(createdPermission);
    }

    @PutMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<PermissionDTO> updatePermission(@PathVariable String id,
                                                          @Valid @RequestBody PermissionDTO permissionDTO) {
        PermissionDTO updatedPermission = roleService.updatePermission(id, permissionDTO);
        return ResponseEntity.ok(updatedPermission);
    }

    @DeleteMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_MANAGEMENT') or hasAuthority('ALL_MODULES')")
    public ResponseEntity<Void> deletePermission(@PathVariable String id) {
        roleService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}

