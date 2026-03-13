package com.mitmeerut.CFM_Portal.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Institute")
public class Institute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String code;

    @Lob
    private String address;

    private String email;
    private String phone;
    private String website;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
/*
    @OneToMany(mappedBy = "institute", cascade = CascadeType.ALL)
    private List<Department> departments;
*/
    public Institute() {}

    // getters and setters
    // ... (omitted here for brevity, paste below)
    public Long getId(){ return id; }
    public void setId(Long id){ this.id = id; }
    public String getName(){ return name; }
    public void setName(String name){ this.name = name; }
    public String getCode(){ return code; }
    public void setCode(String code){ this.code = code; }
    public String getAddress(){ return address; }
    public void setAddress(String address){ this.address = address; }
    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }
    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone = phone; }
    public String getWebsite(){ return website; }
    public void setWebsite(String website){ this.website = website; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
   // public List<Department> getDepartments(){ return departments; }
   // public void setDepartments(List<Department> departments){ this.departments = departments; }
}
