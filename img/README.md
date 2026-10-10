# AcadOS — คลังรูปภาพและภาพบันทึกการทดสอบระบบ (Multimedia Assets & E2E Screenshots)

ไดเรกทอรีนี้เป็นส่วนหนึ่งของโครงสร้างโฟลเดอร์มาตรฐานตามเกณฑ์ข้อกำหนดรายวิชา ([`doc/prof_ruleset.md`](../doc/prof_ruleset.md) §9) สำหรับจัดเก็บรูปภาพมัลติมีเดีย, ภาพสถาปัตยกรรม, และภาพบันทึกผลการทดสอบระบบจริง (Test Evidence Screenshots)

---

## 1. การทำงานร่วมกับ Robot Framework Test Suite

ภายในไดเรกทอรีนี้มีไฟล์สคริปต์ [**`robot_testcase.robot`**](robot_testcase.robot) สำหรับสั่งเปิดเบราว์เซอร์อัตโนมัติ ทดสอบหน้าเว็บของ AcadOS และ **แคปภาพหน้าจอ (Capture Page Screenshot)** ทุกขั้นตอนมาเก็บไว้ในโฟลเดอร์ `img/` โดยอัตโนมัติ

### 1.1 การติดตั้งเครื่องมือ (Prerequisites)
ติดตั้ง Robot Framework และ SeleniumLibrary ผ่าน pip (รันครั้งเดียว):
```bash
pip install robotframework robotframework-seleniumlibrary
```

### 1.2 คำสั่งในการรันเพื่อแคปภาพทั้งหมดลง `img/`
ตรวจสอบว่าเว็บ AcadOS กำลังรันอยู่ที่ `http://localhost:8080` จากนั้นรันคำสั่ง:
```bash
robot -d results img/robot_testcase.robot
```

---

## 2. รายการภาพบันทึกผลการทดสอบ (Test Evidence Artifacts)

เมื่อรันสคริปต์เสร็จสิ้น จะได้ไฟล์รูปภาพบันทึกผลการทำงานของ 8 Test Cases (Negative 3 Scenarios และ Positive 5 Scenarios) รวมทั้งสิ้น 17 ภาพดังนี้:

| ชื่อไฟล์รูปภาพ | Test Case ที่เกี่ยวข้อง | หน้าจอที่ถูกบันทึก (Screenshot Scope) |
| :--- | :--- | :--- |
| **`TC_NEG_01_invalid_password.png`** | TC_NEG_01 (Negative Auth) | แจ้งเตือนรหัสผ่านไม่ถูกต้องบนฟอร์มล็อกอิน |
| **`TC_NEG_02_unknown_user.png`** | TC_NEG_02 (Negative Auth) | แจ้งเตือนไม่พบ University ID ในระบบ |
| **`TC_NEG_03_unauthorized_access.png`** | TC_NEG_03 (Negative Security) | ตอบกลับ 401 Unauthorized เมื่อเข้าถึงโดยไม่ล็อกอิน |
| **`TC01_01_login_page.png`** | TC01 (Admin Flow) | หน้าฟอร์มเข้าสู่ระบบ (Login UI) |
| **`TC01_02_admin_dashboard.png`** | TC01 (Admin Flow) | หน้าภาพรวมระบบผู้ดูแลระบบ (Admin Dashboard) |
| **`TC01_03_admin_courses.png`** | TC01 (Admin Flow) | หน้าจัดการหลักสูตรและรายวิชา (`/admin/courses`) |
| **`TC01_04_admin_rooms.png`** | TC01 (Admin Flow) | หน้าจัดการห้องเรียนและอุปกรณ์ (`/admin/rooms`) |
| **`TC01_05_admin_sections.png`** | TC01 (Admin Flow) | หน้าจัดการกลุ่มเรียนและผู้สอน (`/admin/sections`) |
| **`TC01_06_admin_users.png`** | TC01 (Admin Flow) | หน้าจัดการบัญชีผู้ใช้งานระบบ (`/admin/users`) |
| **`TC02_01_timetable_workbench.png`** | TC02 (Scheduling Engine) | หน้าผังตารางเรียนและตารางสอน (Timetable Workbench) |
| **`TC02_02_schedule_generated.png`** | TC02 (Scheduling Engine) | หน้าจอผลการสร้างตารางอัตโนมัติ (DRAFT Schedule) |
| **`TC03_01_student_dashboard.png`** | TC03 (Student Flow) | หน้าศูนย์บริการนักศึกษา (Student Portal) |
| **`TC03_02_student_courses.png`** | TC03 (Student Flow) | หน้ารายวิชาที่เปิดรับลงทะเบียนเรียน |
| **`TC03_03_student_registrations.png`** | TC03 (Student Flow) | หน้ารายวิชาที่นักศึกษาลงทะเบียนสำเร็จแล้ว |
| **`TC04_01_teacher_dashboard.png`** | TC04 (Teacher Flow) | หน้าภาพรวมระบบอาจารย์ (Teacher Portal) |
| **`TC04_02_teacher_swaps_inbox.png`** | TC04 (Teacher Flow) | หน้ากล่องข้อความคำขอแลกคาบสอน (Swap Requests) |
| **`TC05_01_swagger_ui.png`** | TC05 (API & Monitoring) | หน้าเอกสาร API แบบโต้ตอบได้ (Swagger UI) |
| **`TC05_02_actuator_health.png`** | TC05 (API & Monitoring) | หน้าตรวจสอบสถานะสุขภาพของระบบ (Actuator Health UP) |


---

## 3. การนำรูปภาพไปใช้งาน

รูปภาพที่ได้สามารถนำไปประกอบ:
1. สไลด์นำเสนอโครงงานในโฟลเดอร์ [`doc/slide/`](../doc/slide/)
2. รายงานผลการทดสอบระบบในโฟลเดอร์ [`test/`](../test/)
3. เอกสารประกอบการตรวจประเมินของคณะกรรมการและอาจารย์ประจำวิชา

