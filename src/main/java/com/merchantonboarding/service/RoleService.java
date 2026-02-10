package com.merchantonboarding.service;

import com.merchantonboarding.dto.RoleDTO;
import com.merchantonboarding.dto.PermissionDTO;
import com.merchantonboarding.model.Role;
import com.merchantonboarding.model.Permission;
import com.merchantonboarding.repository.RoleRepository;
import com.merchantonboarding.repository.PermissionRepository;
import com.merchantonboarding.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    // ─── Role Methods ───────────────────────────────────────

    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
            .map(this::convertRoleToDTO)
            .collect(Collectors.toList());
    }

    public List<RoleDTO> getActiveRoles() {
        return roleRepository.findByIsActiveTrue().stream()
            .map(this::convertRoleToDTO)
            .collect(Collectors.toList());
    }

    public RoleDTO getRoleById(String id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        return convertRoleToDTO(role);
    }

    public RoleDTO createRole(RoleDTO roleDTO) {
        Role role = new Role();
        role.setId(roleDTO.getId() != null ? roleDTO.getId() : "role_" + System.currentTimeMillis());
        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());
        role.setActive(roleDTO.isActive());

        // Set permissions
        if (roleDTO.getPermissions() != null) {
            Set<Permission> permissions = roleDTO.getPermissions().stream()
                .map(permId -> permissionRepository.findById(permId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permId)))
                .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        Role savedRole = roleRepository.save(role);
        return convertRoleToDTO(savedRole);
    }

    public RoleDTO updateRole(String id, RoleDTO roleDTO) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));

        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());
        role.setActive(roleDTO.isActive());

        // Update permissions
        if (roleDTO.getPermissions() != null) {
            Set<Permission> permissions = roleDTO.getPermissions().stream()
                .map(permId -> permissionRepository.findById(permId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permId)))
                .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        Role savedRole = roleRepository.save(role);
        return convertRoleToDTO(savedRole);
    }

    public void deleteRole(String id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role not found: " + id);
        }
        roleRepository.deleteById(id);
    }

    public boolean userHasPermission(String roleId, String permissionId) {
        Role role = roleRepository.findById(roleId).orElse(null);
        if (role == null) return false;

        // Admin has all permissions
        if ("admin".equals(roleId)) return true;

        return role.getPermissions().stream()
            .anyMatch(p -> p.getId().equals(permissionId) || p.getId().equals("all_modules"));
    }

    // ─── Permission Methods ───────────────────────────────────────

    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
            .map(this::convertPermissionToDTO)
            .collect(Collectors.toList());
    }

    public List<PermissionDTO> getActivePermissions() {
        return permissionRepository.findByIsActiveTrue().stream()
            .map(this::convertPermissionToDTO)
            .collect(Collectors.toList());
    }

    public List<PermissionDTO> getPermissionsByCategory(String category) {
        return permissionRepository.findByCategory(category).stream()
            .map(this::convertPermissionToDTO)
            .collect(Collectors.toList());
    }

    public PermissionDTO createPermission(PermissionDTO permissionDTO) {
        Permission permission = new Permission();
        permission.setId(permissionDTO.getId() != null ? permissionDTO.getId() : "perm_" + System.currentTimeMillis());
        permission.setName(permissionDTO.getName());
        permission.setDescription(permissionDTO.getDescription());
        permission.setCategory(permissionDTO.getCategory());
        permission.setActive(permissionDTO.isActive());

        Permission savedPermission = permissionRepository.save(permission);
        return convertPermissionToDTO(savedPermission);
    }

    public PermissionDTO updatePermission(String id, PermissionDTO permissionDTO) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));

        permission.setName(permissionDTO.getName());
        permission.setDescription(permissionDTO.getDescription());
        permission.setCategory(permissionDTO.getCategory());
        permission.setActive(permissionDTO.isActive());

        Permission savedPermission = permissionRepository.save(permission);
        return convertPermissionToDTO(savedPermission);
    }

    public void deletePermission(String id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission not found: " + id);
        }
        permissionRepository.deleteById(id);
    }

    // ─── Conversion Methods ───────────────────────────────────────

    private RoleDTO convertRoleToDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setActive(role.isActive());
        dto.setCreatedAt(role.getCreatedAt() != null ? role.getCreatedAt().toString() : null);
        dto.setUpdatedAt(role.getUpdatedAt() != null ? role.getUpdatedAt().toString() : null);

        if (role.getPermissions() != null) {
            dto.setPermissions(role.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toSet()));
        }

        return dto;
    }

    private PermissionDTO convertPermissionToDTO(Permission permission) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        dto.setDescription(permission.getDescription());
        dto.setCategory(permission.getCategory());
        dto.setActive(permission.isActive());
        dto.setCreatedAt(permission.getCreatedAt() != null ? permission.getCreatedAt().toString() : null);
        dto.setUpdatedAt(permission.getUpdatedAt() != null ? permission.getUpdatedAt().toString() : null);
        return dto;
    }
}

