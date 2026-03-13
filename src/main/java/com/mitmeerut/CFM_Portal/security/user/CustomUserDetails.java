package com.mitmeerut.CFM_Portal.security.user;

import com.mitmeerut.CFM_Portal.Model.Teacher;
import com.mitmeerut.CFM_Portal.Model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return user.getId();
    }

    public User.userRole getRole() {
        return user.getRole();
    }

    public Teacher getTeacher() {
        return user.getTeacher();
    }

    public User getUser() {
        return user;
    }

    public Long getDepartmentId() {
        if (user.getTeacher() == null || user.getTeacher().getDepartment() == null) {
            return null;
        }
        return user.getTeacher().getDepartment().getId();
    }

    public String getProfileImageUrl() {
        if (user.getTeacher() != null && user.getTeacher().getProfileImageUrl() != null) {
            return user.getTeacher().getProfileImageUrl();
        }
        return user.getProfileImageUrl();
    }

    public String getFullName() {
        if (user.getTeacher() != null && user.getTeacher().getName() != null) {
            return user.getTeacher().getName();
        }
        return user.getUsername();
    }

    public String getFirstName() {
        String fullName = getFullName();
        if (fullName == null || fullName.isEmpty())
            return "";
        return fullName.split("\\s+")[0];
    }

    public String getLastName() {
        String fullName = getFullName();
        if (fullName == null || fullName.isEmpty())
            return "";
        String[] parts = fullName.split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        return user.getEffectiveRoles().stream()
                .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getIsActive());
    }
}
