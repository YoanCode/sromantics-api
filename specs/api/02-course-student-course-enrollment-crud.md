# Spec 02: Course、StudentCourse 與 Enrollment CRUD API

## 目標

提供課程、學生課程額度與班級加入紀錄的管理 API，支援學生轉班及跨班共用堂數。

## Resource responsibilities

- `Course`：管理課程本身的名稱、分類與描述。
- `StudentCourse`：管理學生購買某門課的堂數、付款狀態與課程狀態。
- `Enrollment`：管理 StudentCourse 目前或歷史加入的 Class；轉班時保留舊紀錄。
- `Class`：管理課程的固定班次、教師、教室與上課時段。

## Endpoints

三個 resource 都提供相同的 CRUD endpoint：

| Method | Path | Behavior |
| --- | --- | --- |
| GET | `/api/courses` | 取得所有課程 |
| GET | `/api/courses/{id}` | 取得單一課程 |
| POST | `/api/courses` | 建立課程，API 產生 id |
| PUT | `/api/courses/{id}` | 更新課程 |
| DELETE | `/api/courses/{id}` | 刪除課程 |
| GET | `/api/student-courses` | 取得所有學生課程額度 |
| POST | `/api/student-courses` | 建立學生課程額度 |
| PUT | `/api/student-courses/{id}` | 更新堂數或狀態 |
| DELETE | `/api/student-courses/{id}` | 刪除學生課程額度 |
| GET | `/api/enrollments` | 取得所有班級加入紀錄 |
| POST | `/api/enrollments` | 建立班級加入紀錄 |
| PUT | `/api/enrollments/{id}` | 更新班級或轉班狀態 |
| DELETE | `/api/enrollments/{id}` | 刪除班級加入紀錄 |
| GET | `/api/classes` | 取得所有班級 |
| GET | `/api/classes/{id}` | 取得單一班級 |
| POST | `/api/classes` | 建立班級，API 產生 id |
| PUT | `/api/classes/{id}` | 更新班級 |
| DELETE | `/api/classes/{id}` | 刪除班級 |

找不到指定 id 時回傳 `404 Not Found`；建立成功回傳 `201 Created`；刪除成功回傳 `204 No Content`。

## Transfer workflow

1. 將原本的 `Enrollment.status` 更新為 `transferred`，並填入 `endedAt`。
2. 建立同一個 `studentCourseId` 對應新 `classId` 的 `Enrollment`，狀態為 `active`。
3. `StudentCourse.purchasedLessons`、`usedLessons` 與 `remainingLessons` 不因轉班而改變。

## Lesson balance rule

```text
remainingLessons = purchasedLessons - usedLessons
```

新建與更新 `StudentCourse` 時，前端與 API client 必須傳送非負整數堂數；實際扣堂流程應由出席功能更新 `usedLessons` 並重新計算 `remainingLessons`。

## Seed data verification

API 啟動時必須補齊下列示範資料，即使 SQLite 已經存在舊資料：

- `cl_003`：數學 B 班。
- `sc_001`：購買 20 堂、已使用 3 堂、剩餘 17 堂。
- `e_001`：`sc_001` 從 `cl_001` 轉出，狀態為 `transferred`。
- `e_003`：`sc_001` 轉入 `cl_003`，狀態為 `active`。
