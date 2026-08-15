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
    private final EnrollmentRepository enrollmentRepo;

    @Override
    public void run(String... args) {
        if (parentRepo.count() > 0) return; // 已有資料則跳過

        // --- Parents ---
        parentRepo.save(new Parent("p_001", "王大明", "0912345678",
                "daming.wang@example.com", Parent.Relationship.father));

        // --- Students ---
        studentRepo.save(new Student("s_001", "p_001", "王小智",
                Student.Gender.male, "東山國中", "國二",
                "對海鮮過敏，下課需要家長接送", Student.Status.active));
        studentRepo.save(new Student("s_002", "p_001", "王小美",
                Student.Gender.female, "東山國小", "小六",
                null, Student.Status.active));

        // --- Courses ---
        courseRepo.save(new Course("c_math", "國中數學拔尖",
                Course.Category.math, "針對國中段考與會考進階題型訓練"));
        courseRepo.save(new Course("c_eng", "兒童實用美語",
                Course.Category.english, "外師全美語互動教學"));

        // --- Classes ---
        clazzRepo.save(new Clazz("cl_001", "c_math", "2026秋季 國二數學特訓A班",
                "張天才", "201大教室", 2, "18:30", "21:30", 25, 600));
        clazzRepo.save(new Clazz("cl_002", "c_eng", "2026秋季 小六美語衝刺班",
                "David Lee", "102語言教室", 4, "17:00", "19:00", 15, 800));

        // --- Enrollments ---
        enrollmentRepo.save(new Enrollment("e_001", "s_001", "cl_001",
                "2026-08-01", Enrollment.PaymentStatus.paid, 20));
        enrollmentRepo.save(new Enrollment("e_002", "s_002", "cl_002",
                "2026-08-05", Enrollment.PaymentStatus.partial, 10));
    }
}
