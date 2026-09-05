# Data Model

你的直覺**完全正確，而且非常專業！** 🎯

在開發任何資訊管理系統（MIS / MDS）時，「先定 Data Model（資料模型），再建 UI 介面」是最標準且最能避免後期踩坑的作法。資料結構沒定好，前端畫面寫再漂亮最後都得重構。

你提出的層級關係 `Parent -> Child -> Course -> Class` 已經把核心主軸抓出來了！

不過在補習班（MDS）的實際營運邏輯中，我們通常會對這個關聯做一些**微調與最佳化**，讓資料庫在處理「一對多」、「多對多」時更合理。

---

## 💡 補習班 Data Model 架構建議與微調

你原本的觀念：

> `Parent` (家長) $\rightarrow$ `Child` (學生) $\rightarrow$ `Course` (課程) $\rightarrow$ `Class` (班級)

### 🛠 最佳化建議：

1. **Course (科目/課程) 與 Class (班級/實體班) 的關係**：
* **`Course`** 應該是「抽象的科目/課程」，例如：*國三數學班*、*高中英文精修*。
* **`Class`** 才是「具體的開班/班次」，例如：*2026秋季國三數學A班 (每週二 18:30)*。
* **關係**：一個 `Course` 底下可以開多個 `Class`。


2. **Class (班級/班次) 的上課時間**：
* `Class` 代表補習班實際開設的一個固定班次，因此 `dayOfWeek`、`startTime`、`endTime`、`classroom` 與 `teacherName` 都屬於 `Class`。
* 學生在報名時選擇符合需求的 `Class`，而不是在 `Student` 儲存個人的上課時間。
* 如果同一門課有不同上課時段，應建立多筆 `Class`，例如「國中數學週二班」與「國中數學週四班」。

3. **Child (學生) 與 Class (班級) 的關係 (選課/報名)**：
* 一個學生可以報名多個班級，一個班級也有多個學生，這是 **多對多 (Many-to-Many)** 關係。
* `StudentCourse` 負責記錄學生購買某門課的堂數與付款狀態。
* `Enrollment` 負責記錄學生目前或歷史加入的班級，轉班時保留舊紀錄並新增一筆新班級紀錄。
* 堂數屬於學生與課程的關係，不應放在 `Student`、`Course` 或 `Class`。



---

## 📐 建議的 TypeScript 資料型別 (Data Models)

在 React + TS 專案中，我們可以在 `src/types/mds.ts` 先定義好這些 Type：

```typescript
// 1. 家長 (Parent)
export interface Parent {
  id: string;
  name: string;
  phone: string;
  email?: string;
  relationship: 'father' | 'mother' | 'guardian'; // 關係
}

// 2. 學生 (Student / Child)
export interface Student {
  id: string;
  parentId: string;        // 關聯到 Parent.id
  name: string;
  gender: 'male' | 'female';
  schoolName: string;      // 就讀學校 (例: 東山國中)
  grade: string;           // 年級 (例: 國二)
  note?: string;           // 備註 (例: 對花生過敏)
  status: 'active' | 'graduated' | 'suspended'; // 在學/結業/停課
}

// 3. 抽象課程 (Course)
export interface Course {
  id: string;
  name: string;            // 課程名稱 (例: 國中數學)
  category: 'math' | 'english' | 'science'; // 科目類別
  description?: string;
}

// 4. 具體班級 (Class)
export interface Class {
  id: string;
  courseId: string;        // 關聯到 Course.id
  className: string;       // 班級名稱 (例: 2026秋季國三數學A班)
  teacherName: string;     // 授課教師
  classroom: string;       // 教室 (例: 201 教室)
  dayOfWeek: 1 | 2 | 3 | 4 | 5 | 6 | 7; // 週幾上課
  startTime: string;       // 上課時間 (例: "18:30")
  endTime: string;         // 下課時間 (例: "21:30")
  maxCapacity: number;     // 人數上限
  pricePerPeriod: number;  // 每堂單價或每期學費
}

// 5. 學生課程額度 (StudentCourse) —— 管理跨班共用的堂數！
export interface StudentCourse {
  id: string;
  studentId: string;
  courseId: string;
  enrolledAt: string;
  paymentStatus: 'paid' | 'unpaid' | 'partial';
  purchasedLessons: number;
  usedLessons: number;
  remainingLessons: number;
  status: 'active' | 'completed' | 'cancelled';
}

// 6. 班級加入紀錄 (Enrollment) —— 管理轉班歷史！
export interface Enrollment {
  id: string;
  studentCourseId: string;
  classId: string;
  startedAt: string;
  endedAt?: string;
  status: 'active' | 'transferred' | 'completed' | 'cancelled';
}

// 7. 出席紀錄 (Attendance) —— 記錄每位學生每堂課的實際出席！
export interface Attendance {
  id: string;
  enrollmentId: string;
  studentCourseId: string;
  classId: string;
  attendanceDate: string;
  status: 'present' | 'absent' | 'late' | 'excused';
  makeUpCreditId?: string;
  note?: string;
  recordedAt?: string;
}

// 8. 缺席產生的一次性補課資格
export interface MakeUpCredit {
  id: string;
  studentId: string;
  sourceAttendanceId: string;
  sourceEnrollmentId: string;
  validUntil: string;
  status: 'available' | 'scheduled' | 'used' | 'expired' | 'cancelled';
  targetClassId?: string;
  targetDate?: string;
  usedAttendanceId?: string;
  note?: string;
  createdAt?: string;
  usedAt?: string;
}

```

---

## 🔄 資料關聯流程圖

```text
[ Parent 家長 ]
      │ (1對多)
      ▼
[ Student 學生 ] ─── [ StudentCourse 學生課程額度 ] ─── [ Course 課程 ]
             │
             ▼
        [ Enrollment 班級紀錄 ]
             │
             ▼
           [ Class 班級 ]
             │
             ▼
       [ Attendance 出席紀錄 ]
      ▲
      │ (多對1)
[ Course 課程科目 ]

```

### 堂數與選班規則

1. `Class` 的時間是班次固定的時間；學生透過建立 `Enrollment` 選擇班次。
2. `StudentCourse.purchasedLessons` 記錄該學生購買某門課的總堂數。
3. `StudentCourse.usedLessons` 記錄該學生已使用的堂數。
4. `StudentCourse.remainingLessons` 記錄尚未使用的堂數，必須符合：

  ```text
  remainingLessons = purchasedLessons - usedLessons
  ```

5. 同一個 `StudentCourse` 可以有多筆 `Enrollment`，支援轉班與跨班使用堂數。
6. 轉班時，舊 `Enrollment.status` 設為 `transferred`，再建立新的 `active` Enrollment；`StudentCourse` 的堂數不變。
6a. `StudentCourse` 只代表學生與 Course 的額度、付款與狀態，不直接儲存或顯示單一 Class；班級歸屬與歷史由 Enrollment 管理。
7. `Attendance` 以 `enrollmentId + attendanceDate` 唯一識別一次點名紀錄。
8. `present` 或 `late` 會增加 `StudentCourse.usedLessons` 並減少 `remainingLessons`；`absent` 與 `excused` 不扣堂。
9. `Attendance` 只有在 `attendanceDate` 落於 Enrollment 的 `startedAt` 到 `endedAt`（含邊界，未設定 `endedAt` 視為持續有效），且符合 Class 固定上課星期時才具備建立資格；`cancelled` Enrollment 不可建立出席。
10. 刪除 `present` 或 `late` Attendance 必須回補一堂 StudentCourse 額度；若 Attendance 指向已不存在的 StudentCourse，視為孤兒資料，刪除時跳過回補但仍移除孤兒紀錄。
11. StudentCourse 或 Enrollment 被 Attendance／Enrollment 參照時不可直接刪除，避免產生新的孤兒 Attendance；API 應回傳衝突並要求先處理關聯資料。
12. `absent` Attendance 建立成功時自動產生一筆 `MakeUpCredit`；每筆缺席最多一筆，預設有效期限為缺席日起 30 天。
13. `MakeUpCredit` 是學生一次性的補課資格，不屬於 Course 或 StudentCourse 額度；安排補課時可選擇同 Course 的其他 Class。
14. 補課出席必須帶入 `makeUpCreditId`，並使用來源 Enrollment 驗證學生與 StudentCourse 資格；Attendance 的 `classId` 記錄補課資格安排的目標 Class，日期必須符合目標 Class 的固定上課日；成功出席後狀態改為 `used`，同一資格不可重複使用。
15. Attendance 狀態由非 `absent` 改為 `absent` 時自動建立補課資格；由 `absent` 改回 `present`、`late` 或 `excused` 時，尚未使用的補課資格改為 `cancelled`，已使用的補課資格不得取消。
16. 補課資格的來源 Attendance 被刪除時，該補課資格也必須一併移除；系統啟動時應清理已沒有來源 Attendance 的孤兒補課資格。

---

## 🚀 下一步

把資料結構定義好之後，我們就能用這組 **Type** 建立幾筆 Mock Data（測試假資料），接著就能輕鬆地在畫面呈現：

1. **學生清單**（包含點擊可看到家長聯絡資訊）。
2. **班級課表**（可以看到該班級有哪些學生報名）。

我們要把這份 Type 檔案寫進 `src/types/mds.ts` 嗎？