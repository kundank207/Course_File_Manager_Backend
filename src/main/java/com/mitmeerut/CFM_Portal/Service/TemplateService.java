package com.mitmeerut.CFM_Portal.Service;

import com.mitmeerut.CFM_Portal.Model.Template;
import com.mitmeerut.CFM_Portal.security.user.CustomUserDetails;

import java.util.List;

public interface TemplateService {

    Template createTemplate(Template template, CustomUserDetails user);

    List<Template> getByDepartment(Long departmentId);

    Template update(Long id, Template template, CustomUserDetails user);

    void delete(Long id, CustomUserDetails user);
    Template getDepartmentTemplate(Long departmentId);
}
