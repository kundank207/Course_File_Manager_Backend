package com.mitmeerut.CFM_Portal.dto;

import lombok.Data;

@Data
public class ChatTeacherDTO {
    private Long id;
    private String name;
    private String email;
    private String department;
    private Boolean isOnline;

    public ChatTeacherDTO(Long id, String name, String email, String department, Boolean isOnline) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.isOnline = isOnline;
    }

    public ChatTeacherDTO() {
    }
}
