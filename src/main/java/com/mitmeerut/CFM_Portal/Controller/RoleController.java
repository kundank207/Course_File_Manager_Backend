package com.mitmeerut.CFM_Portal.Controller;

import com.mitmeerut.CFM_Portal.Model.RolePermission;
import com.mitmeerut.CFM_Portal.Model.User;
import com.mitmeerut.CFM_Portal.Repository.UserRepository;
import com.mitmeerut.CFM_Portal.Service.RolePermissionService;
import com.mitmeerut.CFM_Portal.dto.RoleDto;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")

public class RoleController {

    private final RolePermissionService rolePermissionService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoleController(RolePermissionService rolePermissionService, UserRepository userRepository) {
        this.rolePermissionService = rolePermissionService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        List<RolePermission> roles = rolePermissionService.getAllRoles();
        List<RoleDto> result = new ArrayList<>();

        for (RolePermission rp : roles) {
            try {
                // Count users with this role
                User.userRole roleEnum = User.userRole.valueOf(rp.getRoleName());
                int userCount = userRepository.findByRole(roleEnum).size();

                // Parse permissions JSON
                List<String> permissions = objectMapper.readValue(
                        rp.getPermissions(),
                        new TypeReference<List<String>>() {
                        });

                result.add(new RoleDto(rp.getId(), rp.getRoleName(), userCount, permissions));
            } catch (Exception e) {
                // If parsing fails, add with empty permissions
                result.add(new RoleDto(rp.getId(), rp.getRoleName(), 0, new ArrayList<>()));
            }
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{roleName}")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<RoleDto> getRoleByName(@PathVariable("roleName") String roleName) {
        return rolePermissionService.getByRoleName(roleName)
                .map(rp -> {
                    try {
                        User.userRole roleEnum = User.userRole.valueOf(rp.getRoleName());
                        int userCount = userRepository.findByRole(roleEnum).size();
                        List<String> permissions = objectMapper.readValue(
                                rp.getPermissions(),
                                new TypeReference<List<String>>() {
                                });
                        return ResponseEntity.ok(new RoleDto(rp.getId(), rp.getRoleName(), userCount, permissions));
                    } catch (Exception e) {
                        return ResponseEntity.ok(new RoleDto(rp.getId(), rp.getRoleName(), 0, new ArrayList<>()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{roleName}")
    @PreAuthorize("hasAuthority('MANAGE_ROLES')")
    public ResponseEntity<?> updatePermissions(
            @PathVariable("roleName") String roleName,
            @RequestBody Map<String, List<String>> body,
            @AuthenticationPrincipal CustomUserDetails user) {

        try {
            List<String> permissions = body.get("permissions");
            if (permissions == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing 'permissions' field");
                return ResponseEntity.badRequest().body(error);
            }

            String permissionsJson = objectMapper.writeValueAsString(permissions);
            RolePermission updated = rolePermissionService.updatePermissions(
                    roleName,
                    permissionsJson,
                    user.getUserId());

            // Return updated role
            User.userRole roleEnum = User.userRole.valueOf(updated.getRoleName());
            int userCount = userRepository.findByRole(roleEnum).size();

            return ResponseEntity.ok(new RoleDto(
                    updated.getId(),
                    updated.getRoleName(),
                    userCount,
                    permissions));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
