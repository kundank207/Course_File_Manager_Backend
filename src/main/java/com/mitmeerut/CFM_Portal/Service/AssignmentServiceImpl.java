package com.mitmeerut.CFM_Portal.Service;
//
//import com.mitmeerut.CFM_Portal.Model.CourseTeacher;
//import com.mitmeerut.CFM_Portal.Repository.CourseTeacherRepository;
//import org.springframework.stereotype.Service;
//import java.util.List;
//import java.util.Optional;
//
//@Service
//public class AssignmentServiceImpl implements AssignmentService {
//    private final CourseTeacherRepository ctRepo;
//    public AssignmentServiceImpl(CourseTeacherRepository ctRepo){ this.ctRepo = ctRepo; }
//
//    @Override public List<CourseTeacher> findAll(){ return ctRepo.findAll(); }
//    @Override public CourseTeacher assign(CourseTeacher ct){ return ctRepo.save(ct); }
//    @Override public void unassign(Long id){ ctRepo.deleteById(id); }
//    @Override public Optional<CourseTeacher> findById(Long id){ return ctRepo.findById(id); }
//    @Override public List<CourseTeacher> findByCourseId(Long courseId){ return ctRepo.findByCourse_Id(courseId); }
//    @Override public List<CourseTeacher> findByTeacherId(Long teacherId){ return ctRepo.findByTeacher_Id(teacherId); }
//}
