# Spec 01: Students CRUD API

## 🎯 目標

建立 `Student` 的 RESTful CRUD API，供 `sromantics-web` 透過 HTTP 存取，並設定 CORS 允許本地開發伺服器跨域請求。

---

## 📁 需建立的檔案

```
src/main/java/com/sromantics/sromantics_api/
├── config/
│   └── CorsConfig.java
├── dto/
│   └── StudentDto.java
└── student/
    ├── StudentController.java
    └── StudentService.java
```

---

## 🛠️ Step 1: 建立 CORS 設定

允許 `sromantics-web` 的 dev server（`http://localhost:5173`）跨域存取 `/api/**`。

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }
}
```

---

## 🛠️ Step 2: 建立 `StudentDto`

DTO 用於隔離 Entity 與 API 層，`id` 在建立時由後端產生（UUID），更新與查詢時由前端提供。

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {
    private String id;
    private String parentId;
    private String name;
    private String gender;       // "male" | "female"
    private String schoolName;
    private String grade;
    private String note;
    private String status;       // "active" | "graduated" | "suspended"
}
```

---

## 🛠️ Step 3: 建立 `StudentService`

負責 CRUD 邏輯與 Entity ↔ DTO 轉換；建立時以 UUID 產生 `id`。

```java
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public List<StudentDto> findAll() {
        return studentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public StudentDto findById(String id) {
        return studentRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public StudentDto create(StudentDto dto) {
        dto.setId(UUID.randomUUID().toString());
        return toDto(studentRepository.save(toEntity(dto)));
    }

    public StudentDto update(String id, StudentDto dto) {
        if (!studentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        dto.setId(id);
        return toDto(studentRepository.save(toEntity(dto)));
    }

    public void delete(String id) {
        if (!studentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        studentRepository.deleteById(id);
    }

    private StudentDto toDto(Student s) {
        return new StudentDto(s.getId(), s.getParentId(), s.getName(),
                s.getGender().name(), s.getSchoolName(), s.getGrade(),
                s.getNote(), s.getStatus().name());
    }

    private Student toEntity(StudentDto dto) {
        return new Student(dto.getId(), dto.getParentId(), dto.getName(),
                Student.Gender.valueOf(dto.getGender()),
                dto.getSchoolName(), dto.getGrade(), dto.getNote(),
                Student.Status.valueOf(dto.getStatus()));
    }
}
```

---

## 🛠️ Step 4: 建立 `StudentController`

```java
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    public List<StudentDto> list() {
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    public StudentDto get(@PathVariable String id) {
        return studentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDto create(@RequestBody StudentDto dto) {
        return studentService.create(dto);
    }

    @PutMapping("/{id}")
    public StudentDto update(@PathVariable String id, @RequestBody StudentDto dto) {
        return studentService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        studentService.delete(id);
    }
}
```

---

## 📡 API 端點總覽

| Method   | Path                  | 說明         | 成功回應        |
| -------- | --------------------- | ------------ | --------------- |
| `GET`    | `/api/students`       | 取得全部學生  | `200` JSON 陣列 |
| `GET`    | `/api/students/{id}`  | 取得單筆學生  | `200` JSON 物件 |
| `POST`   | `/api/students`       | 新增學生      | `201` JSON 物件 |
| `PUT`    | `/api/students/{id}`  | 更新學生      | `200` JSON 物件 |
| `DELETE` | `/api/students/{id}`  | 刪除學生      | `204` 無內容    |

---

## ✅ 驗證

啟動後執行：

```bash
# 列出全部學生
curl http://localhost:8080/api/students

# 新增學生
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -d '{"parentId":"p_001","name":"測試生","gender":"male","schoolName":"測試國中","grade":"國一","status":"active"}'

# 更新學生
curl -X PUT http://localhost:8080/api/students/{id} \
  -H "Content-Type: application/json" \
  -d '{"parentId":"p_001","name":"測試生（改）","gender":"male","schoolName":"測試國中","grade":"國二","status":"active"}'

# 刪除學生
curl -X DELETE http://localhost:8080/api/students/{id}
```

預期結果：
- `GET /api/students` 回傳至少 2 筆（DataInitializer 寫入的資料）
- `POST` 回傳 `201` 含新 UUID id
- `PUT` 回傳 `200` 含更新後資料
- `DELETE` 回傳 `204`
- 查詢不存在的 id 回傳 `404`
