package com.mitmeerut.CFM_Portal.dto;

import java.util.List;

public class RoleDto {
    private Long id;
    private String name;
    private int users;
    private List<String> permissions;

    public RoleDto() {
    }

    public RoleDto(Long id, String name, int users, List<String> permissions) {
        this.id = id;
        this.name = name;
        this.users = users;
        this.permissions = permissions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUsers() {
        return users;
    }

    public void setUsers(int users) {
        this.users = users;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
