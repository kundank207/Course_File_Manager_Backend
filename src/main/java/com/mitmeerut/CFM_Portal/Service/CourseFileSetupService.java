//package com.mitmeerut.CFM_Portal.Service;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mitmeerut.CFM_Portal.Model.CourseFile;
//import com.mitmeerut.CFM_Portal.Model.Heading;
//import com.mitmeerut.CFM_Portal.Model.Template;
//import com.mitmeerut.CFM_Portal.Repository.CourseFileRepository;
//import com.mitmeerut.CFM_Portal.Repository.HeadingRepository;
//import com.mitmeerut.CFM_Portal.Repository.TemplateRepository;
//import com.mitmeerut.CFM_Portal.dto.TemplateHeadingDTO;
//
//import jakarta.transaction.Transactional;
//
//@Service
//public class CourseFileSetupService{
//	private TemplateRepository templateRepo;
//	private HeadingRepository headingRepo;
//	private ObjectMapper objectMapper;
//	private CourseFileRepository courseFileRepo;
//
//	@Autowired
//	public CourseFileSetupService(TemplateRepository templateRepo, HeadingRepository headingRepo,ObjectMapper objectMapper) {
//		this.templateRepo = templateRepo;
//		this.headingRepo = headingRepo;
//		this.objectMapper=objectMapper;
//	}
//
//    @Transactional
//    public void applyTemplateToCourseFile(
//            Long courseFileId,
//            Long departmentId,
//            String templateType
//    ) throws Exception {
//
//        CourseFile courseFile = courseFileRepo.findById(courseFileId)
//                .orElseThrow(() -> new RuntimeException("CourseFile not found"));
//
//        Template template = templateRepo
//                .findTopByDepartmentIdAndTypeOrderByCreatedAtDesc(
//                        departmentId, templateType
//                )
//                .orElseThrow(() -> new RuntimeException("Template not found"));
//
//        List<TemplateHeadingDTO> headings =
//                objectMapper.readValue(
//                        template.getStructure(),
//                        new TypeReference<List<TemplateHeadingDTO>>() {}
//                );
//
//        for (TemplateHeadingDTO h : headings) {
//            Heading heading = new Heading();
//            heading.setCourseFile(courseFile); // if using entity
//            heading.setParentHeading(null);
//            heading.setTitle(h.getTitle());
//            heading.setOrderIndex(h.getOrder());
//
//            headingRepo.save(heading);
//        }
//    }
//}
