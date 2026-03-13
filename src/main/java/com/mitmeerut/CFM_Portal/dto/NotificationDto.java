package com.mitmeerut.CFM_Portal.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NotificationDto {
    private Long id;
    @NotBlank private String title;
    @NotBlank private String message;
    @NotNull private String audience; // ALL / TEACHERS / HODS / DEPT_12 etc.
    // getters/setters
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getMessage(){return message;} public void setMessage(String message){this.message=message;}
    public String getAudience(){return audience;} public void setAudience(String audience){this.audience=audience;}
}
