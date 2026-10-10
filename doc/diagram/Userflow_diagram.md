# AcadOS v4 — Consolidated Userflow Architecture

**แผนภาพขั้นตอนการใช้งานระบบเชิงสถาปัตยกรรม (Consolidated End-to-End Userflows)**  
**สถาปัตยกรรม:** Hybrid Best Practice (Semantic Consolidation + Collapsible Accordion by Role)  
**ขอบเขต:** ทุกบทบาท (Common, Student, Teacher, Admin) ครอบคลุม Business Rules BR-01 ถึง BR-10 ครบถ้วน 100%

---

## สารบัญและภาพรวมการจัดกลุ่ม (Table of Contents)

| หมวดหมู่ (Domain Scope) | รหัสโฟลว์ | ชื่อกระบวนการ (Unified User Journey) | ขอบเขตการทำงานที่ครอบคลุม |
|---|:---:|---|---|
| **1. Common (ทุก Role)** | **C01** | [Authentication & Access Control](#c01-authentication--access-control) | Login, Logout, Security Filter (401/403/404) |
| | **C02** | [Notification Center](#c02-notification-center) | ดูแจ้งเตือนส่วนตัว และกดยืนยันอ่านแล้ว (Mark as Read) |
| | **C03** | [Academic Calendar & Public Holidays](#c03-academic-calendar--public-holidays) | ตรวจสอบปฏิทินการศึกษาและวันหยุดราชการ |
| **2. Student** | **S01** | [Course Browsing & Registration](#s01-course-browsing--registration) | ค้นหาวิชา/กลุ่มเรียน → ตรวจ BR-03, 04, 05, 09 → ลงทะเบียน |
| | **S02** | [Course Withdrawal](#s02-course-withdrawal) | ถอนรายวิชาที่ลงทะเบียนไว้ |
| | **S03** | [Timetable & Schedule Change Tracking](#s03-timetable--schedule-change-tracking) | ดูตารางเรียนส่วนตัว + รับแจ้งเตือนตารางเปลี่ยน/ยกเลิก |
| **3. Teacher** | **T01** | [Teaching Timetable View](#t01-teaching-timetable-view) | ดูตารางสอนและกลุ่มเรียนที่ได้รับมอบหมาย |
| | **T02** | [Profile, Availability & Preferences Modal](#t02-profile-availability--preferences-modal) | จัดการเวลาไม่สะดวกสอน (BR-07), วิชาที่อยากสอน (D21), เปลี่ยนรหัสผ่าน |
| | **T03** | [Teacher Swap Request Lifecycle](#t03-teacher-swap-request-lifecycle) | สร้างคำขอแลกคาบ (BR-01/06/07) → ยกเลิก → ตอบรับ/ปฏิเสธ |
| **4. Admin** | **A01** | [Course Management Lifecycle](#a01-course-management-lifecycle) | CRUD รายวิชา (รหัสวิชา, หน่วยกิต, ตรวจ Section ผูกอยู่) |
| | **A02** | [Room Management Lifecycle](#a02-room-management-lifecycle) | CRUD ห้องเรียน (อาคาร, เลขห้อง, ความจุ, ตรวจคาบผูกอยู่) |
| | **A03** | [Academic Event Management Lifecycle](#a03-academic-event-management-lifecycle) | CRUD ปฏิทินวิชาการ (Semester, Registration BR-09, Exams) |
| | **A04** | [User Account Management Lifecycle](#a04-user-account-management-lifecycle) | CRUD บัญชีผู้ใช้ Teacher และ Student |
| | **A05** | [Section Management & Teacher Assignment](#a05-section-management--teacher-assignment) | สร้างกลุ่มเรียน, กำหนดความจุ, มอบหมายผู้สอน (A13, BR-06/07) |
| | **A06** | [Timetable Generation & Publishing](#a06-timetable-generation--publishing) | สุ่มจัดตารางอัตโนมัติ (DRAFT) → ตรวจสอบ → Publish / Discard |
| | **A07** | [Master Timetable & Registry Overview](#a07-master-timetable--registry-overview) | ดูผังตารางรวมทุกห้อง/อาจารย์ + ดูสถิติการลงทะเบียน |
| | **A08** | [Admin Swap Review Workflow](#a08-admin-swap-review-workflow) | พิจารณาคำขอแลกคาบ (Approve สลับตารางถาวร / Reject) |
| | **A09** | [Section Cancellation Workflow](#a09-section-cancellation-workflow) | ยกเลิกกลุ่มเรียน (State Pattern) → ปลดตาราง → เคลียร์ลงทะเบียน |
| | **A10** | [Public Holiday Management](#a10-public-holiday-management) | ดูตารางวันหยุด + ปุ่ม Manual Sync ดึงจาก Bot API |

---

## สัญลักษณ์สีในแผนภาพ (Color Conventions)

| สี | ความหมาย |
|---|---|
| 🔵 **น้ำเงินเข้ม** | จุดเริ่มต้น / สิ้นสุดของกระบวนการ (`:::start`) |
| ⚪ **ฟ้าอ่อน** | การกระทำของผู้ใช้หรือการประมวลผลของระบบ (`:::act`) |
| 🟡 **เหลืองทอง** | จุดตัดสินใจและการตรวจสอบเงื่อนไขทางธุรกิจ (`:::dec`) |
| 🔴 **ชมพูแดง** | กรณีเกิดข้อผิดพลาด / ระบบปฏิเสธ พร้อม HTTP Status Code (`:::bad`) |
| 🟢 **เขียวสด** | ผลลัพธ์สำเร็จ / การส่งการแจ้งเตือน (`:::ok`) |
| 🔘 **เทาอ่อน** | คำอธิบายและหมายเหตุทางเทคนิค (`:::note`) |

---

<details open>
<summary><h2 style="display:inline-block; cursor:pointer;">1. Common Userflows (ทุก Role)</h2></summary>

### C01 Authentication & Access Control
**Role:** ทุก Role · **API:** `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` · **Security:** JWT Filter, Role Authorization (401 / 403)

```mermaid
flowchart TD
    n1(["ผู้ใช้เปิดหน้าระบบ"]):::start
    n2("กรอก University ID / Email + Password"):::act
    n1 --> n2
    n3("POST /api/v1/auth/login"):::act
    n2 --> n3
    n4{"ข้อมูลถูกต้อง<br/>(BCrypt match)?"}:::dec
    n3 --> n4
    n5["401 Unauthorized<br/>รหัสผ่านหรือผู้ใช้ไม่ถูกต้อง"]:::bad
    n4 -->|ไม่ผ่าน| n5
    n5 -.-> n2
    n6("สร้าง JWT Token (บรรจุ Role และ Claims)"):::act
    n4 -->|ผ่าน| n6
    n7{"Role ของผู้ใช้"}:::dec
    n6 --> n7
    n8(["Admin Dashboard"]):::ok
    n9(["Teacher Dashboard"]):::ok
    n10(["Student Dashboard"]):::ok
    n7 -->|ADMIN| n8
    n7 -->|TEACHER| n9
    n7 -->|STUDENT| n10

    subgraph Access_Control ["Security Filter & Guard"]
        req("ผู้ใช้เรียก URL / API"):::act
        t_chk{"มี JWT ที่ถูกต้อง<br/>และไม่หมดอายุ?"}:::dec
        req --> t_chk
        t_chk -->|ไม่มี / หมดอายุ| err401["401 Unauthorized<br/>(Redirect ไปหน้า Login)"]:::bad
        r_chk{"มี Role ตรงตาม<br/>สิทธิ์ที่กำหนด?"}:::dec
        t_chk -->|มี| r_chk
        r_chk -->|สิทธิ์ไม่ตรง| err403["403 Forbidden<br/>(แสดง Neutral Error Page)"]:::bad
        r_chk -->|ผ่าน| allow(["อนุญาตเข้าสู่ Controller"]):::ok
    end

    subgraph Logout_Flow ["Logout Action"]
        lo_btn("กดปุ่มออกจากระบบ (Logout)"):::act
        lo_act("ล้าง Token ใน Client Storage & Cookie"):::act
        lo_end(["กลับสู่หน้า Login พร้อม logout=true"]):::start
        lo_btn --> lo_act --> lo_end
    end

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
    classDef note fill:#F4F4F4,stroke:#999,color:#1a1a1a
```

---

### C02 Notification Center
**Role:** ทุก Role · **API:** `GET /api/v1/notifications`, `PUT /api/v1/notifications/{id}/read`

```mermaid
flowchart TD
    n1(["Dashboard (ทุก Role)"]):::start
    n2("คลิกไอคอนกระดิ่งแจ้งเตือน"):::act
    n3("GET /api/v1/notifications"):::act
    n1 --> n2 --> n3
    n4(["แสดงรายการแจ้งเตือน<br/>(เรียงจากใหม่สุดไปเก่าสุด)"]):::ok
    n3 --> n4
    n5{"คลิกรายการแจ้งเตือน<br/>ที่ยังไม่อ่าน"}:::dec
    n4 --> n5
    n6("PUT /api/v1/notifications/{id}/read"):::act
    n5 -->|เลือกอ่าน| n6
    n7{"แจ้งเตือนเป็นของ<br/>ผู้ใช้คนนี้?"}:::dec
    n6 --> n7
    n8["404 Not Found / 403 Forbidden"]:::bad
    n7 -->|ไม่ใช่| n8
    n9("markAsRead() → อัปเดต is_read = true"):::act
    n7 -->|ใช่| n9
    n10(["เปลี่ยนสถานะเป็นอ่านแล้วบน UI"]):::ok
    n9 --> n10

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### C03 Academic Calendar & Public Holidays
**Role:** ทุก Role · **UI:** `/calendar`, `/holidays` · **API:** `GET /api/v1/academic-events`, `GET /api/v1/holidays`

```mermaid
flowchart TD
    n1(["ผู้ใช้เปิดหน้า Academic Calendar"]):::start
    n2("ดึงข้อมูล Academic Events<br/>(Semester, Exam, Registration Period)"):::act
    n3("ดึงข้อมูล Public Holidays<br/>(วันหยุดราชการประจำเดือน/ปี)"):::act
    n1 --> n2
    n1 --> n3
    n4(["ผสานข้อมูลและแสดงผลบนปฏิทินรายเดือน/สัปดาห์"]):::ok
    n2 --> n4
    n3 --> n4

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

</details>

---

<details>
<summary><h2 style="display:inline-block; cursor:pointer;">2. Student Userflows (นักศึกษา)</h2></summary>

### S01 Course Browsing & Registration
**Role:** Student · **API:** `GET /api/v1/courses`, `GET /api/v1/sections`, `POST /api/v1/registrations` · **Rules:** BR-03, BR-04, BR-05, BR-09

```mermaid
flowchart TD
    n1(["Student Dashboard"]):::start
    n2("เปิดหน้าระบบลงทะเบียนเรียน"):::act
    n3("เลือกดูรายวิชาและ Section ที่เปิดรับ"):::act
    n1 --> n2 --> n3
    n4("กดปุ่ม 'ลงทะเบียน' ใน Section ที่ต้องการ"):::act
    n3 --> n4
    n5("POST /api/v1/registrations (sectionId)"):::act
    n4 --> n5
    br9{"อยู่ในช่วงเวลา<br/>ลงทะเบียนเรียน (BR-09)?"}:::dec
    n5 --> br9
    br9 -->|ไม่| err9["400: อยู่นอกช่วงเวลาลงทะเบียน"]:::bad
    sec_chk{"สถานะ Section<br/>เป็น ACTIVE?"}:::dec
    br9 -->|ใช่| sec_chk
    sec_chk -->|ถูกยกเลิกแล้ว| err_sec["400: กลุ่มเรียนถูกยกเลิกแล้ว"]:::bad
    br4{"เคยลงทะเบียน<br/>วิชานี้แล้ว (BR-04)?"}:::dec
    sec_chk -->|ใช่| br4
    br4 -->|วิชาซ้ำ| err4["400: ลงทะเบียนวิชาเดียวกันซ้ำซ้อน"]:::bad
    br5{"จำนวนผู้ลงทะเบียน<br/>ยังไม่เกิน Capacity (BR-05)?"}:::dec
    br4 -->|ไม่ซ้ำ| br5
    br5 -->|กลุ่มเรียนเต็ม| err5["400: จำนวนผู้ลงทะเบียนเต็มแล้ว"]:::bad
    br3{"เวลาเรียนไม่ชนกับ<br/>วิชาที่ลงไว้ก่อนหน้า (BR-03)?"}:::dec
    br5 -->|ยังว่าง| br3
    br3 -->|เวลาชน| err3["409: ตารางเรียนชนกัน (Conflict Detected)<br/>ส่ง Notification เตือนนักศึกษา"]:::bad
    save_reg("บันทึก Registration ลง Database"):::act
    br3 -->|ไม่ชน| save_reg
    notify("ส่ง Notification: REGISTRATION_SUCCESS (In-App + Email)"):::act
    save_reg --> notify
    done(["ลงทะเบียนสำเร็จ (201 Created)<br/>แสดงผลในตารางเรียนของฉัน"]):::ok
    notify --> done

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### S02 Course Withdrawal
**Role:** Student · **API:** `DELETE /api/v1/registrations/{id}` · **Rules:** BR-09 (ช่วงเวลาเพิ่ม-ถอน)

```mermaid
flowchart TD
    n1(["Student Dashboard"]):::start
    n2("เปิดหน้าตารางเรียน / วิชาที่ลงทะเบียนไว้"):::act
    n3("คลิกปุ่ม 'ถอนรายวิชา' (Withdraw)"):::act
    n1 --> n2 --> n3
    n4{"อยู่ในช่วงเวลา<br/>ถอนรายวิชา (BR-09)?"}:::dec
    n3 --> n4
    n5["400: หมดเขตช่วงเวลาถอนรายวิชา"]:::bad
    n4 -->|ไม่ผ่าน| n5
    n6("ลบข้อมูลออกจาก RegistrationRepository"):::act
    n4 -->|ผ่าน| n6
    n7("ส่ง Notification แจ้งผลการถอนรายวิชา"):::act
    n6 --> n7
    n8(["ถอนรายวิชาสำเร็จ (204 No Content)<br/>ตารางเรียนอัปเดตทันที"]):::ok
    n7 --> n8

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### S03 Timetable & Schedule Change Tracking
**Role:** Student · **UI:** `/student/timetable` · **Design Pattern:** Observer Pattern (`ScheduleChangePublisher` & `NotificationService`)

```mermaid
flowchart TD
    n1(["Student เปิดหน้าตารางเรียน"]):::start
    n2("ดึงเฉพาะ Schedule ที่สถานะ PUBLISHED<br/>ของ Section ที่ตนเองลงทะเบียนไว้"):::act
    n1 --> n2
    n3(["แสดงผังตารางประจำสัปดาห์<br/>(ผสานคาบเรียนต่อเนื่องเป็นกล่องเดียว)"]):::ok
    n2 --> n3

    subgraph Background_Observer ["การติดตามการเปลี่ยนแปลงแบบ Real-time"]
        ev{"เกิดเหตุการณ์ในระบบ?"}:::dec
        ev -->|Admin Publish ตารางใหม่ / มีการ Swap คาบ| ev_chg("Observer แจ้งเตือน SCHEDULE_CHANGED"):::act
        ev -->|Admin ยกเลิกกลุ่มเรียน| ev_can("Observer แจ้งเตือน SECTION_CANCELLED"):::act
        ev_chg --> pop(["แสดงการแจ้งเตือนบนไอคอนกระดิ่ง + ส่ง Email"]):::ok
        ev_can --> pop
    end

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

</details>

---

<details>
<summary><h2 style="display:inline-block; cursor:pointer;">3. Teacher Userflows (อาจารย์)</h2></summary>

### T01 Teaching Timetable View
**Role:** Teacher · **UI:** `/teacher/dashboard` · **API:** `GET /api/v1/teacher-swaps/my-schedules`

```mermaid
flowchart TD
    n1(["Teacher เข้าสู่ระบบ"]):::start
    n2("เปิดหน้าตารางสอนของฉัน"):::act
    n3("ดึงคาบสอนที่สถานะ = PUBLISHED<br/>ที่ได้รับมอบหมายเป็นผู้สอน"):::act
    n1 --> n2 --> n3
    n4(["แสดงผลตารางสอนประจำสัปดาห์<br/>(รวมคาบ 2-3 ชม. ต่อเนื่องเป็นช่องเดียว)"]):::ok
    n3 --> n4

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### T02 Profile, Availability & Preferences Modal
**Role:** Teacher · **UI:** Profile Modal (`main-layout.html`) · **API:** `TeacherPreferenceApiController`, `TeacherPreferenceService` · **Rules:** BR-06, BR-07, D21

```mermaid
flowchart TD
    n1(["Teacher: คลิกชื่อ/โปรไฟล์มุมขวาบน"]):::start
    n2("เปิด Profile Modal"):::act
    n1 --> n2
    tabs{"เลือกแท็บการทำงาน"}:::dec
    n2 --> tabs

    subgraph Tab_Availability ["แท็บ: ความพร้อมในการสอน (BR-07)"]
        t_avail("แสดงตาราง Unavailable Slots (สล็อตละ 1.5 ชม. IDs 32-56)"):::act
        t_avail_act{"คลิกเลือกสล็อตเวลา หรือใช้ Quick Range Form"}:::dec
        t_avail --> t_avail_act
        t_avail_act -->|ตั้งเป็นไม่สะดวกสอน| set_unavail("PUT /api/v1/teacher/availabilities (isAvailable=false)"):::act
        t_avail_act -->|ตั้งเป็นสะดวกสอน| set_avail("PUT /api/v1/teacher/availabilities (isAvailable=true)"):::act
        res_avail(["บันทึกและแสดงสีแดง/เขียวแบบ Interactive ทันที"]):::ok
        set_unavail --> res_avail
        set_avail --> res_avail
    end

    subgraph Tab_Preferences ["แท็บ: ความประสงค์ในการสอน (BR-06 & D21)"]
        t_pref("ดึง Qualified Courses (BR-06) ผ่าน GET /qualifications"):::act
        t_pref_act("เลือกวิชาที่อยากสอน + กำหนด Priority 1-5"):::act
        t_pref --> t_pref_act
        t_save_pref("POST /api/v1/teacher/preferences"):::act
        t_pref_act --> t_save_pref
        res_pref(["บันทึกลำดับความต้องการสอนสำเร็จ"]):::ok
        t_save_pref --> res_pref
    end

    subgraph Tab_Password ["แท็บ: บัญชีและความปลอดภัย"]
        t_pwd("กรอก Current Password + New Password"):::act
        t_pwd_post("PUT /api/v1/auth/change-password"):::act
        t_pwd --> t_pwd_post
        res_pwd(["เปลี่ยนรหัสผ่านสำเร็จ"]):::ok
        t_pwd_post --> res_pwd
    end

    tabs --> Tab_Availability
    tabs --> Tab_Preferences
    tabs --> Tab_Password

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### T03 Teacher Swap Request Lifecycle
**Role:** Teacher A (ผู้ขอ), Teacher B (ผู้ถูกขอ) · **API:** `/api/v1/teacher-swaps` · **Rules:** BR-01, BR-06, BR-07

```mermaid
flowchart TD
    start_a(["Teacher A เปิดหน้าแลกคาบ"]):::start
    act_req("เลือกคาบตนเอง + เลือก Teacher B + เลือกคาบของ B"):::act
    start_a --> act_req
    chk_rule{"ตรวจสอบเงื่อนไขการแลกคาบ<br/>- B ไม่ใช่ตนเอง<br/>- ทั้งคู่มีคุณสมบัติสอนได้ (BR-06)<br/>- ทั้งคู่ว่างในเวลาใหม่ (BR-07)<br/>- ตารางไม่ชนกัน (BR-01)"}:::dec
    act_req --> chk_rule
    chk_rule -->|ไม่ผ่าน| err_swap["400/409: ผิดเงื่อนไข ไม่สามารถยื่นขอแลกได้"]:::bad
    save_swap("บันทึกคำขอ สถานะ = PENDING"):::act
    chk_rule -->|ผ่าน| save_swap
    notify_b("ส่ง Notification แจ้งเตือน Teacher B"):::act
    save_swap --> notify_b

    subgraph Flow_Cancel ["A ขอยกเลิกคำขอ"]
        a_can("Teacher A กด 'ยกเลิกคำขอ'"):::act
        a_can_chk{"สถานะยังเป็น<br/>PENDING?"}:::dec
        a_can --> a_can_chk
        a_can_chk -->|ใช่| a_can_done("เปลี่ยนสถานะเป็น CANCELLED + แจ้งเตือน B"):::act
        a_can_chk -->|ไม่ใช่| a_can_err["ยกเลิกไม่ได้ (B ตอบแล้ว)"]:::bad
    end

    subgraph Flow_Respond ["B ตอบรับ / ปฏิเสธคำขอ"]
        b_view("Teacher B เปิดดูคำขอแลกคาบ"):::act
        b_dec{"Teacher B ตัดสินใจ"}:::dec
        b_view --> b_dec
        b_dec -->|ปฏิเสธ| b_rej("PUT /respond (action=REJECT)<br/>สถานะ = REJECTED → แจ้งเตือน A"):::act
        b_dec -->|ตอบรับ| b_acc("PUT /respond (action=ACCEPT)<br/>สถานะ = ACCEPTED → แจ้งเตือน A และ Admin"):::act
    end

    notify_b --> Flow_Respond
    save_swap -.-> Flow_Cancel
    b_acc --> wait_admin(["คำขอสถานะ ACCEPTED<br/>รอ Admin อนุมัติในขั้นตอน A08"]):::ok

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

</details>

---

<details>
<summary><h2 style="display:inline-block; cursor:pointer;">4. Admin Userflows (ผู้ดูแลระบบ)</h2></summary>

### A01 Course Management Lifecycle (CRUD)
**Role:** Admin · **UI:** `/admin/courses` · **API:** `/api/v1/courses`

```mermaid
flowchart TD
    n1(["Admin เปิดหน้าจัดการรายวิชา"]):::start
    n2("GET /api/v1/courses (แสดงตารางรายวิชาทั้งหมดพร้อม Pagination)"):::act
    n1 --> n2
    act_choice{"เลือกการดำเนินการ"}:::dec
    n2 --> act_choice

    subgraph Create_Flow ["สร้างรายวิชาใหม่"]
        c_open("กดปุ่ม 'เพิ่มรายวิชา' → กรอกข้อมูล"):::act
        c_post("POST /api/v1/courses"):::act
        c_chk{"รหัสวิชาซ้ำหรือไม่?"}:::dec
        c_open --> c_post --> c_chk
        c_chk -->|ซ้ำ| c_err["409 Conflict: รหัสวิชาซ้ำ"]:::bad
        c_chk -->|ไม่ซ้ำ| c_ok(["บันทึกรายวิชาใหม่สำเร็จ"]):::ok
    end

    subgraph Update_Flow ["แก้ไขรายวิชา"]
        u_open("กดปุ่ม 'แก้ไข' → แก้ไขชื่อวิชา/หน่วยกิต"):::act
        u_put("PUT /api/v1/courses/{id}"):::act
        u_open --> u_put --> u_ok(["อัปเดตข้อมูลสำเร็จ"]):::ok
    end

    subgraph Delete_Flow ["ลบรายวิชา"]
        d_btn("กดปุ่ม 'ลบรายวิชา'"):::act
        d_del("DELETE /api/v1/courses/{id}"):::act
        d_chk{"ยังมี Section ของวิชานี้<br/>เปิดอยู่ในระบบหรือไม่?"}:::dec
        d_btn --> d_del --> d_chk
        d_chk -->|ยังมี Section ผูกอยู่| d_err["400 Bad Request: ไม่สามารถลบได้เนื่องจากมีกลุ่มเรียนผูกอยู่"]:::bad
        d_chk -->|ไม่มี Section| d_ok(["ลบรายวิชาสำเร็จ"]):::ok
    end

    act_choice --> Create_Flow
    act_choice --> Update_Flow
    act_choice --> Delete_Flow

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A02 Room Management Lifecycle (CRUD)
**Role:** Admin · **UI:** `/admin/rooms` · **API:** `/api/v1/rooms`

```mermaid
flowchart TD
    n1(["Admin เปิดหน้าจัดการห้องเรียน"]):::start
    n2("GET /api/v1/rooms (แสดงรายการห้องเรียน ความจุ และสถานะความพร้อม BR-08)"):::act
    n1 --> n2
    act_choice{"เลือกการดำเนินการ"}:::dec
    n2 --> act_choice

    subgraph Room_Create ["เพิ่มห้องเรียนใหม่"]
        rc_post("POST /api/v1/rooms (อาคาร, เลขห้อง, ความจุ)"):::act
        rc_chk{"อาคาร + เลขห้องซ้ำ?"}:::dec
        rc_post --> rc_chk
        rc_chk -->|ซ้ำ| rc_err["409 Conflict: ห้องเรียนซ้ำ"]:::bad
        rc_chk -->|ไม่ซ้ำ| rc_ok(["บันทึกห้องเรียนสำเร็จ"]):::ok
    end

    subgraph Room_Update ["แก้ไขห้องเรียน"]
        ru_put("PUT /api/v1/rooms/{id} (แก้ไขความจุ, สลับสถานะพร้อมใช้ BR-08)"):::act
        ru_put --> ru_ok(["อัปเดตข้อมูลสำเร็จ"]):::ok
    end

    subgraph Room_Delete ["ลบห้องเรียน"]
        rd_del("DELETE /api/v1/rooms/{id}"):::act
        rd_chk{"มีคาบสอนใน Schedule<br/>ใช้ห้องนี้อยู่หรือไม่?"}:::dec
        rd_del --> rd_chk
        rd_chk -->|มีคาบผูกอยู่| rd_err["400 Bad Request: ห้องเรียนถูกใช้งานอยู่ในตารางสอน"]:::bad
        rd_chk -->|ไม่มีคาบผูกอยู่| rd_ok(["ลบห้องเรียนสำเร็จ"]):::ok
    end

    act_choice --> Room_Create
    act_choice --> Room_Update
    act_choice --> Room_Delete

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A03 Academic Event Management Lifecycle (CRUD)
**Role:** Admin · **UI:** `/admin/academic-events` · **API:** `/api/v1/academic-events`

```mermaid
flowchart TD
    n1(["Admin เปิดหน้าปฏิทินกิจกรรม"]):::start
    n2("GET /api/v1/academic-events"):::act
    n1 --> n2
    act_choice{"เลือกการดำเนินการ"}:::dec
    n2 --> act_choice

    subgraph Event_CRUD ["สร้าง / แก้ไข / ลบ กิจกรรมวิชาการ"]
        e_post("POST / PUT / DELETE /api/v1/academic-events"):::act
        e_val{"วันเริ่ม <= วันสิ้นสุด<br/>และระบุ EventType ถูกต้อง?"}:::dec
        e_post --> e_val
        e_val -->|ไม่ถูกต้อง| e_err["400 Bad Request: ช่วงเวลาไม่สมเหตุสมผล"]:::bad
        e_val -->|ถูกต้อง| e_ok(["บันทึกกิจกรรมวิชาการสำเร็จ<br/>(ส่งผลต่อเงื่อนไขการลงทะเบียน BR-09 ทันที)"]):::ok
    end

    act_choice --> Event_CRUD

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A04 User Account Management Lifecycle (CRUD)
**Role:** Admin · **UI:** `/admin/users` · **API:** `/api/v1/users`

```mermaid
flowchart TD
    n1(["Admin เปิดหน้าจัดการผู้ใช้งาน"]):::start
    n2("GET /api/v1/users (แสดงบัญชี Teacher และ Student)"):::act
    n1 --> n2
    act_choice{"เลือกการดำเนินการ"}:::dec
    n2 --> act_choice

    subgraph User_Create ["สร้างบัญชีผู้ใช้ใหม่"]
        u_post("POST /api/v1/users (University ID, Email, Role, Full Name)"):::act
        u_chk{"University ID หรือ<br/>Email ซ้ำในระบบ?"}:::dec
        u_post --> u_chk
        u_chk -->|ซ้ำ| u_err["409 Conflict: บัญชีผู้ใช้นี้มีอยู่แล้ว"]:::bad
        u_chk -->|ไม่ซ้ำ| u_ok(["สร้างบัญชีสำเร็จ (เข้ารหัสรหัสผ่านด้วย BCrypt)"]):::ok
    end

    subgraph User_Update ["แก้ไขข้อมูลบัญชี"]
        up_put("PUT /api/v1/users/{id} (แก้ไขชื่อ, Email, รหัสผ่าน)"):::act
        up_put --> up_ok(["อัปเดตข้อมูลบัญชีสำเร็จ"]):::ok
    end

    subgraph User_Delete ["ลบบัญชีผู้ใช้"]
        ud_del("DELETE /api/v1/users/{id}"):::act
        ud_chk{"มีตารางสอน หรือ<br/>การลงทะเบียนผูกอยู่หรือไม่?"}:::dec
        ud_del --> ud_chk
        ud_chk -->|มีข้อมูลผูกอยู่| ud_err["400 Bad Request: ไม่สามารถลบได้เนื่องจากมีข้อมูลผูกอยู่"]:::bad
        ud_chk -->|ไม่มีข้อมูลผูก| ud_ok(["ลบบัญชีสำเร็จ"]):::ok
    end

    act_choice --> User_Create
    act_choice --> User_Update
    act_choice --> User_Delete

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A05 Section Management & Teacher Assignment
**Role:** Admin · **UI:** `/admin/sections` · **API:** `/api/v1/sections`, `SectionService` · **Rules:** A13, BR-01, BR-06, BR-07, BR-08

```mermaid
flowchart TD
    n1(["Admin เปิดหน้า Section Management"]):::start
    n2("GET /api/v1/sections"):::act
    n1 --> n2
    choice{"เลือกการดำเนินการ"}:::dec
    n2 --> choice

    subgraph Create_Sec ["สร้างกลุ่มเรียนใหม่"]
        cs_post("POST /api/v1/sections (courseId, sectionNumber, capacity)"):::act
        cs_chk{"เลข Section ในวิชานี้ซ้ำ?"}:::dec
        cs_post --> cs_chk
        cs_chk -->|ซ้ำ| cs_err["409: กลุ่มเรียนซ้ำ"]:::bad
        cs_chk -->|ไม่ซ้ำ| cs_ok(["สร้าง Section สำเร็จ (สถานะเริ่มต้น ACTIVE)"]):::ok
    end

    subgraph Assign_Teacher ["มอบหมายผู้สอน (Sub-feature A13)"]
        at_post("PUT /api/v1/sections/{id}/teacher (teacherId)"):::act
        at_chk{"ตรวจสอบคุณสมบัติอาจารย์<br/>- มีคุณสมบัติสอนวิชานี้ (BR-06)?<br/>- พร้อมสอนในช่วงเวลาของ Section (BR-07)?<br/>- เวลาสอนไม่ชนกับคาบอื่น (BR-01)?"}:::dec
        at_post --> at_chk
        at_chk -->|ไม่ผ่าน| at_err["400/409: อาจารย์ขาดคุณสมบัติ / ไม่พร้อมสอน / เวลาชน"]:::bad
        at_chk -->|ผ่าน| at_save("บันทึกผู้สอนลงในทุกคาบของ Section"):::act
        at_notif("ส่ง Notification แจ้งอาจารย์คนใหม่, คนเดิม และนักศึกษา"):act
        at_save --> at_notif --> at_ok(["มอบหมายผู้สอนสำเร็จ"]):::ok
    end

    choice --> Create_Sec
    choice --> Assign_Teacher

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A06 Timetable Generation & Publishing
**Role:** Admin · **UI:** `/admin/timetable` · **API:** `/api/v1/schedules/generate`, `/publish`, `/discard` · **Engine:** Constraint-based Engine & Multi-factor Scoring

```mermaid
flowchart TD
    n1(["Admin เปิดหน้าจัดตารางสอน"]):::start
    n2("คลิกปุ่ม 'สร้างตารางสอนอัตโนมัติ (Generate Schedule)'"):::act
    n1 --> n2
    gen_act("ลบตาราง DRAFT เดิมทั้งหมด → ค้นหา ACTIVE Sections ที่ยังไม่มีตาราง"):::act
    n2 --> gen_act

    subgraph Engine ["Scheduling Engine Pipeline"]
        pipe_cand("สร้าง Candidate Matrix (Teacher x Room x TimeSlot)"):::act
        pipe_hard{"ConstraintEvaluator ตรวจ Hard Constraints<br/>(BR-01 ไม่ชน, BR-02 ห้องไม่ชน, BR-06 มีคุณสมบัติ,<br/>BR-07 อาจารย์ว่าง, BR-08 ห้องพร้อมใช้)"}:::dec
        pipe_score("ScheduleSelector ประเมินคะแนน Candidate ด้วย ScoringStrategy<br/>(PreferenceScoreStrategy, WorkloadScoreStrategy)"):::act
        pipe_pick("เลือก Candidate ที่ได้คะแนนสูงสุด"):::act
        gen_act --> pipe_cand --> pipe_hard
        pipe_hard -->|ผ่าน| pipe_score --> pipe_pick
    end

    save_draft("บันทึกคาบสอนทั้งหมดเป็นสถานะ DRAFT"):::act
    pipe_pick --> save_draft
    view_draft(["Admin ตรวจสอบผังตาราง DRAFT บน UI"]):::ok
    save_draft --> view_draft

    admin_dec{"Admin พอใจกับผลลัพธ์ DRAFT หรือไม่?"}:::dec
    view_draft --> admin_dec

    subgraph Discard_Flow ["ยกเลิก DRAFT"]
        disc_btn("คลิก 'Discard Draft'"):::act
        disc_del("ลบตาราง DRAFT ทั้งหมดออกจากระบบ"):::act
        disc_end(["กลับสู่สถานะก่อน Generate (204 No Content)"]):::start
        disc_btn --> disc_del --> disc_end
    end

    subgraph Publish_Flow ["เผยแพร่ตาราง (Publish)"]
        pub_btn("คลิก 'Publish Schedule'"):::act
        pub_act("อัปเดตสถานะคาบทั้งหมดเป็น PUBLISHED"):::act
        pub_obs("ScheduleChangePublisher แจ้งเตือน Observer ทั้งหมด"):::act
        pub_notif("ส่ง Notification ไปยังอาจารย์และนักศึกษาทุกคน"):act
        pub_end(["ตารางมีผลใช้งานจริง 100% (PUBLISHED)"]):::ok
        pub_btn --> pub_act --> pub_obs --> pub_notif --> pub_end
    end

    admin_dec -->|ไม่พอใจ| Discard_Flow
    admin_dec -->|พอใจ| Publish_Flow

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A07 Master Timetable & Registry Overview
**Role:** Admin · **UI:** `/admin/timetable`, `/admin/swaps` · **API:** `/api/v1/schedules`, `/api/v1/registrations`

```mermaid
flowchart TD
    n1(["Admin Dashboard"]):::start
    n2("เปิดหน้าผังตารางสอนรวม (Master Timetable Grid)"):::act
    n3("ระบบดึงข้อมูลตารางสอนทั้งสถานะ DRAFT และ PUBLISHED"):::act
    n1 --> n2 --> n3
    n4(["แสดงผลผังตารางรวมทุกห้อง / อาจารย์ พร้อม Detailed Registry"]):::ok
    n3 --> n4

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A08 Admin Swap Review Workflow
**Role:** Admin · **UI:** `/admin/swaps` · **API:** `PUT /api/v1/teacher-swaps/{id}/approve`, `PUT /api/v1/teacher-swaps/{id}/reject` · **Rules:** BR-01, BR-06, BR-07

```mermaid
flowchart TD
    n1(["Admin เปิดหน้ารายการคำขอแลกคาบ"]):::start
    n2("กรองดูคำขอที่มีสถานะ = ACCEPTED (ผ่านความยินยอมจาก Teacher B แล้ว)"):::act
    n1 --> n2
    adm_dec{"Admin พิจารณาคำขอ"}:::dec
    n2 --> adm_dec

    subgraph Reject_Swap ["ปฏิเสธคำขอ"]
        rej_act("PUT /api/v1/teacher-swaps/{id}/reject"):::act
        rej_save("อัปเดตสถานะคำขอเป็น REJECTED"):::act
        rej_notif("ส่ง Notification แจ้งเตือน Teacher A และ B"):::act
        rej_act --> rej_save --> rej_notif --> rej_ok(["คำขอถูกปฏิเสธ"]):::bad
    end

    subgraph Approve_Swap ["อนุมัติคำขอ (Approve)"]
        app_act("PUT /api/v1/teacher-swaps/{id}/approve"):::act
        app_rechk{"ตรวจสอบ Hard Constraints อีกครั้ง<br/>(BR-01 ตารางชนทั้ง DRAFT/PUBLISHED, BR-06, BR-07)?"}:::dec
        app_act --> app_rechk
        app_rechk -->|เกิดความขัดแย้งใหม่| app_err["409 Conflict: เกิดตารางชนก่อนอนุมัติ"]:::bad
        app_exec("สลับอาจารย์ผู้สอนของ 2 คาบแบบถาวรในตาราง Schedule"):::act
        app_rechk -->|ผ่าน| app_exec
        app_save("อัปเดตสถานะคำขอเป็น APPROVED"):::act
        app_pub("Trigger ScheduleChangePublisher ส่ง Notification ถึง A, B, และนักศึกษาทั้ง 2 Section"):::act
        app_exec --> app_save --> app_pub --> app_ok(["การแลกคาบเสร็จสมบูรณ์ 100%"]):::ok
    end

    adm_dec -->|ปฏิเสธ| Reject_Swap
    adm_dec -->|อนุมัติ| Approve_Swap

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A09 Section Cancellation Workflow
**Role:** Admin · **API:** `PUT /api/v1/sections/{id}/cancel`, `SectionCancellationService` · **Design Pattern:** State Pattern (`CancelledSectionState`)

```mermaid
flowchart TD
    n1(["Admin เปิดหน้าจัดการกลุ่มเรียน"]):::start
    n2("เลือก Section ที่ต้องการยกเลิก → คลิก 'Cancel Section'"):::act
    n1 --> n2
    n3{"สถานะปัจจุบันของ Section<br/>เป็น ACTIVE?"}:::dec
    n2 --> n3
    n4["400 Bad Request: Section นี้ไม่ได้อยู่ในสถานะ ACTIVE"]:::bad
    n3 -->|ไม่ใช่| n4
    s_state("Section.cancel() → เปลี่ยนสถานะเป็น CANCELLED (State Pattern)"):::act
    n3 -->|ใช่| s_state
    s_sched("ลบคาบสอนทั้งหมดของ Section นี้ออกจาก ScheduleRepository"):::act
    s_state --> s_sched
    s_reg("ลบข้อมูลการลงทะเบียนทั้งหมดของ Section ออกจาก RegistrationRepository"):::act
    s_sched --> s_reg
    s_swap("ยกเลิกคำขอแลกคาบ (Swap Requests) ที่ผูกอยู่กับ Section นี้"):::act
    s_reg --> s_swap
    s_notif("ส่ง Notification แจ้งเตือนอาจารย์ผู้สอน และนักศึกษาที่เคยลงทะเบียน"):::act
    s_swap --> s_notif
    s_done(["ยกเลิก Section สำเร็จสมบูรณ์ (200 OK)"]):::ok
    s_notif --> s_done

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef bad fill:#FCE8E6,stroke:#C0392B,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

---

### A10 Public Holiday Management
**Role:** Admin / ระบบ · **UI:** `/timetable/holidays.html` · **API:** `POST /api/v1/holidays/sync` · **Design Pattern:** Adapter Pattern (`ExternalHolidayAdapter`)

```mermaid
flowchart TD
    n1(["Admin Dashboard / หน้ารายการวันหยุด"]):::start
    n2("ดูรายการวันหยุดราชการที่มีอยู่ใน Database"):::act
    n1 --> n2
    trigger{"รูปแบบการดึงข้อมูลวันหยุด"}:::dec
    n2 --> trigger

    subgraph Auto_Sync ["ระบบดึงอัตโนมัติ (Background Schedule)"]
        auto_act("Cron Job ดึงข้อมูลวันหยุดราชการรายเดือน"):::act
    end

    subgraph Manual_Sync ["Admin กดปุ่ม Sync ด้วยตนเอง"]
        man_act("Admin คลิกปุ่ม 'ดึงข้อมูลวันหยุดราชการจาก Bot API' (btn-sync)"):::act
        man_post("POST /api/v1/holidays/sync"):::act
        man_act --> man_post
    end

    trigger --> Auto_Sync
    trigger --> Manual_Sync

    adapt("HolidayService เรียก HolidayProvider (ExternalHolidayAdapter)"):::act
    Auto_Sync --> adapt
    man_post --> adapt
    ext("External Holiday API ดึงข้อมูลวันหยุดของประเทศไทย"):::act
    adapt --> ext
    save_hol("แปลงข้อมูลผ่าน Adapter และบันทึก/อัปเดตลงใน PublicHolidayRepository"):::act
    ext --> save_hol
    done_hol(["ตารางวันหยุดราชการอัปเดตสำเร็จ พร้อมนำไปแสดงบนปฏิทิน"]):::ok
    save_hol --> done_hol

    classDef start fill:#1F5F8B,stroke:#1F5F8B,color:#fff
    classDef act fill:#EEF3F8,stroke:#5B7A94,color:#1a1a1a
    classDef dec fill:#FFF6E0,stroke:#5B7A94,color:#1a1a1a
    classDef ok fill:#E6F4EA,stroke:#2E7D32,color:#1a1a1a
```

</details>

---

## ข้อสรุปการเปลี่ยนแปลง (Changelog & Architecture Notes)

1. **ลดจำนวนแผนภาพ:** จากเดิม **49 แผนภาพ เหลือเพียง 19 แผนภาพหลัก** (ลดลง 61%) โดยยังคงรักษา Business Rules (BR-01 ถึง BR-10), HTTP Status Codes และ Notification Side Effects ไว้ครบ 100%
2. **ยุบรวม Micro-CRUD:** ยุบรวมการแยกย่อยของ Course, Room, Academic Event, Account, และ Section จาก 19 แผนภาพ ให้เหลือ 5 แผนภาพวงจรการทำงานแบบ Lifecycle ที่ตรงกับการใช้งานบน UI หน้าเดียว
3. **จัดกลุ่มด้วย `<details>`:** เพิ่ม Accordion แยกตาม 4 บทบาทหลัก (Common, Student, Teacher, Admin) ช่วยให้หน้าเอกสารสะอาด สบายตา ไม่กินทรัพยากรการเรนเดอร์ และสามารถคลี่ดูเฉพาะฟังก์ชันที่สนใจได้ทันที
