package com.mitmeerut.CFM_Portal.Model;

import java.io.Serializable;

public class CurrentUser implements Serializable {

    private Long userId;
    private Long departmentId;
    private String role;

    public CurrentUser(Long userId, Long departmentId, String role) {
        this.userId = userId;
        this.departmentId = departmentId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getRole() {
        return role;
    }
}
