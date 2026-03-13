package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.RolePermission;
import java.util.List;
import java.util.Optional;

public interface RolePermissionService {
    List<RolePermission> getAllRoles();

    Optional<RolePermission> getByRoleName(String roleName);

    RolePermission updatePermissions(String roleName, String permissions, Long updatedBy);

    java.util.List<String> getPermissionsByRole(String roleName);

    void initializeDefaultRoles();
}
