# Spec 01: 建立初始資料庫資料

## 🎯 目標

依據 `sromantics-web` 的 mock data，在 SQLite 建立對應的 JPA Entity、Repository，並透過 `DataInitializer` 自動寫入初始資料。

---

## 📁 需建立的檔案

```
src/main/java/com/sromantics/sromantics_api/
├── entity/
│   ├── Parent.java
│   ├── Student.java
│   ├── Course.java
│   ├── Clazz.java          ← 避免與 java.lang.Class 衝突
│   └── Enrollment.java
├── repository/
│   ├── ParentRepository.java
│   ├── StudentRepository.java
│   ├── CourseRepository.java
│   ├── ClazzRepository.java
│   └── EnrollmentRepository.java
└── DataInitializer.java
```

---

## 🛠️ Step 1: 建立 Entity 類別

所有 Entity 使用字串主鍵（對應 mock data 的 `id`），以 `@Column(nullable = false)` 標注必填欄位。

### `Parent.java`

```java
@Entity
@Table(name = "parents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parent {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Relationship relationship;

    public enum Relationship { father, mother, guardian }
}
```

### `Student.java`

```java
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    private String id;

    @Column(nullable = false)
    private String parentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    private String schoolName;

    @Column(nullable = false)
    private String grade;

    private String note;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Gender { male, female }
    public enum Status { active, graduated, suspended }
}
```

### `Course.java`

```java
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    private String description;

    public enum Category { math, english, science, other }
}
```

### `Clazz.java`

> 類別名稱用 `Clazz`，對應的 table 名稱為 `classes`。

```java
@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Clazz {

    @Id
    private String id;

    @Column(nullable = false)
    private String courseId;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String teacherName;

    @Column(nullable = false)
    private String classroom;

    @Column(nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private String startTime;

    @Column(nullable = false)
    private String endTime;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    private int pricePerPeriod;
}
```

### `Enrollment.java`

```java
@Entity
@Table(name = "enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    private String id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String classId;

    @Column(nullable = false)
    private String enrolledAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private int remainingLessons;

    public enum PaymentStatus { paid, unpaid, partial }
}
```

---

## 🛠️ Step 2: 建立 Repository 介面

每個 Entity 建立對應的 `JpaRepository`，無需額外方法，JPA 自動提供 CRUD。

```java
// 以 Parent 為例，其餘四個結構相同
public interface ParentRepository extends JpaRepository<Parent, String> {}
```

五個 Repository 分別為：
- `ParentRepository extends JpaRepository<Parent, String>`
- `StudentRepository extends JpaRepository<Student, String>`
- `CourseRepository extends JpaRepository<Course, String>`
- `ClazzRepository extends JpaRepository<Clazz, String>`
- `EnrollmentRepository extends JpaRepository<Enrollment, String>`

---

## 🛠️ Step 3: 建立 `DataInitializer.java`

實作 `CommandLineRunner`，在啟動時檢查資料是否已存在，若無則寫入初始資料。

```java
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
```

---

## ✅ 驗證

啟動後確認 log 無 ERROR，並以 SQLite 工具（如 DB Browser for SQLite）開啟 `sromantics.db` 驗證五張 table 資料正確寫入。

```
parents       → 1 筆
students      → 2 筆
courses       → 2 筆
classes       → 2 筆
enrollments   → 2 筆
```
