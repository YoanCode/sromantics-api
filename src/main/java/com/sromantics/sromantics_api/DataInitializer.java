package com.sromantics.sromantics_api;

import com.sromantics.sromantics_api.entity.*;
import com.sromantics.sromantics_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;
    private final ClazzRepository clazzRepo;
    private final StudentCourseRepository studentCourseRepo;
    private final EnrollmentRepository enrollmentRepo;

    @Override
    public void run(String... args) {
        if (!parentRepo.existsById("p_001")) {
            parentRepo.save(new Parent("p_001", "王大明", "0912345678",
                    "daming.wang@example.com", Parent.Relationship.father));
        }
        if (!studentRepo.existsById("s_001")) {
            studentRepo.save(new Student("s_001", "p_001", "王小智",
                    Student.Gender.male, "東山國中", "國二",
                    "對海鮮過敏，下課需要家長接送", Student.Status.active));
        }
        if (!studentRepo.existsById("s_002")) {
            studentRepo.save(new Student("s_002", "p_001", "王小美",
                    Student.Gender.female, "東山國小", "小六", null,
                    Student.Status.active));
        }
        if (!courseRepo.existsById("c_math")) {
            courseRepo.save(new Course("c_math", "國中數學拔尖", Course.Category.math,
                    "針對國中段考與會考進階題型訓練"));
        }
        if (!courseRepo.existsById("c_eng")) {
            courseRepo.save(new Course("c_eng", "兒童實用美語", Course.Category.english,
                    "外師全美語互動教學"));
        }
        if (!clazzRepo.existsById("cl_001")) {
            clazzRepo.save(new Clazz("cl_001", "c_math", "2026秋季 國二數學特訓A班",
                    "張天才", "201大教室", 2, "18:30", "21:30", 25, 600));
        }
        if (!clazzRepo.existsById("cl_002")) {
            clazzRepo.save(new Clazz("cl_002", "c_eng", "2026秋季 小六美語衝刺班",
                    "David Lee", "102語言教室", 4, "17:00", "19:00", 15, 800));
        }
        if (!clazzRepo.existsById("cl_003")) {
            clazzRepo.save(new Clazz("cl_003", "c_math", "2026秋季 國二數學特訓B班",
                    "李老師", "202小教室", 4, "18:30", "21:30", 20, 600));
        }
        if (!studentCourseRepo.existsById("sc_001")) {
            studentCourseRepo.save(new StudentCourse("sc_001", "s_001", "c_math",
                    "2026-08-01", StudentCourse.PaymentStatus.paid, 20, 3, 17,
                    StudentCourse.Status.active));
        }
        if (!studentCourseRepo.existsById("sc_002")) {
            studentCourseRepo.save(new StudentCourse("sc_002", "s_002", "c_eng",
                    "2026-08-05", StudentCourse.PaymentStatus.partial, 10, 0, 10,
                    StudentCourse.Status.active));
        }

        Enrollment firstEnrollment = enrollmentRepo.findById("e_001")
                .orElse(new Enrollment("e_001", "2026-08-01", Enrollment.PaymentStatus.paid,
                    20, 3, 17, "s_001", "sc_001", "cl_001", "2026-08-01", "2026-08-20",
                    Enrollment.Status.transferred));
            firstEnrollment.setPaymentStatus(Enrollment.PaymentStatus.paid);
            firstEnrollment.setStudentId("s_001");
            firstEnrollment.setPurchasedLessons(20);
            firstEnrollment.setUsedLessons(3);
            firstEnrollment.setRemainingLessons(17);
        firstEnrollment.setStudentCourseId("sc_001");
        firstEnrollment.setClassId("cl_001");
        firstEnrollment.setStartedAt("2026-08-01");
        firstEnrollment.setEndedAt("2026-08-20");
        firstEnrollment.setStatus(Enrollment.Status.transferred);
        enrollmentRepo.save(firstEnrollment);

        Enrollment secondEnrollment = enrollmentRepo.findById("e_002")
            .orElse(new Enrollment("e_002", "2026-08-05", Enrollment.PaymentStatus.partial,
                10, 0, 10, "s_002", "sc_002", "cl_002", "2026-08-05", null,
                Enrollment.Status.active));
        secondEnrollment.setPaymentStatus(Enrollment.PaymentStatus.partial);
        secondEnrollment.setStudentId("s_002");
        secondEnrollment.setPurchasedLessons(10);
        secondEnrollment.setUsedLessons(0);
        secondEnrollment.setRemainingLessons(10);
        secondEnrollment.setStudentCourseId("sc_002");
        secondEnrollment.setClassId("cl_002");
        secondEnrollment.setStartedAt("2026-08-05");
        secondEnrollment.setStatus(Enrollment.Status.active);
        enrollmentRepo.save(secondEnrollment);

        if (!enrollmentRepo.existsById("e_003")) {
                    enrollmentRepo.save(new Enrollment("e_003", "2026-08-21", Enrollment.PaymentStatus.paid,
                        20, 3, 17, "s_001", "sc_001", "cl_003", "2026-08-21", null,
                    Enrollment.Status.active));
        }
    }
}
