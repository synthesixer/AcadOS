# AcadOS — Project Status & Task Summary

**บันทึกสถานะโครงการและผลการดำเนินงานทางวิศวกรรมซอฟต์แวร์**  
**อัปเดตล่าสุด:** 10 ตุลาคม 2569  
**Branch:** `puttimed_6733804171_03`  
**สถานะการทดสอบล่าสุด:** **326/326 Tests Passed (100% Green, 0 Failures, 0 Errors)**  

---

## 1. วัตถุประสงค์และผลการดำเนินงานล่าสุด (Latest Objectives & Audit Resolutions)

ดำเนินการตรวจสอบเอกสาร ไดอะแกรม และซอร์สโค้ด (Documentation & Architecture Audit) เทียบกับกฎเหล็กของอาจารย์ประจำวิชา (`doc/prof_ruleset.md`) และหลักการใน `AGENTS.md` พร้อมแก้ไขจุด Outdated และ Conflicts ครบถ้วนทั้ง 8 ประเด็นตามมติที่ได้รับอนุมัติ:

1. **[Sub-feature A13] การจัดวางตำแหน่ง Service ของฟังก์ชัน Assign Teacher:**
   - **ปัญหาเดิม:** ไดอะแกรมระบุ `TeacherAssignmentService` แยกต่างหาก แต่โค้ดจริง implemented อยู่ใน `SectionService`
   - **การแก้ไข:** ปรับปรุง `class diagram.puml`, `component-diagram.puml`, และ `Implement_Plan-AcadOS.md` โดยรวมเมธอด `assignTeacher` และ `getTeacherOptions` ไว้ใน `SectionService` ยึดหลัก Single Responsibility & High Cohesion ของ Section Domain และหลัก KISS
2. **[Security Filter Flow] การแยกผลลัพธ์ 401 Unauthorized vs 403 Forbidden ใน Activity Diagrams:**
   - **ปัญหาเดิม:** Activity Diagrams ทั้ง 5 ฉบับระบุเฉพาะ 401 เมื่อมีปัญหาด้านความปลอดภัย
   - **การแก้ไข:** ปรับปรุง Activity Diagrams ครบทั้ง 5 ฉบับ (`activity_authentication.puml`, `activity_schedule_generation.puml`, `activity_section_cancellation.puml`, `activity_student_registration.puml`, `activity_teacher_swap.puml`) ให้แยกกรณี **401 Unauthorized** (ยังไม่ได้ล็อกอิน / Token หมดอายุ ให้ Redirect ไปหน้า Login) และ **403 Forbidden** (ยืนยันตัวตนแล้วแต่ไม่มีสิทธิ์/Role ไม่ตรง ให้แสดงหน้า Neutral Error 403) สอดคล้องกับ `SecurityConfig.java` และ `error.html`
3. **[Holiday Sync Flow] การอนุญาตให้ Admin กด Sync วันหยุดราชการใน Userflow Diagram:**
   - **ปัญหาเดิม:** ตารางสรุปใน `Userflow_diagram.md` (บรรทัดที่ 91) ระบุว่า *"Admin ไม่ต้องกดดึง ดูอย่างเดียว"* ขัดแย้งกับ UI จริง (`timetable/holidays.html`) และ API `POST /api/v1/holidays/sync`
   - **การแก้ไข:** ปรับปรุงตารางสรุปใน `Userflow_diagram.md` ให้อนุญาตให้ Admin กด Trigger Sync วันหยุดราชการแบบ Manual ได้ผ่านหน้าเว็บ นอกเหนือจากระบบ Background Sync รายเดือน
4. **[Layered Architecture & Prof Ruleset] การปรับปรุง Teacher Preference & Availability ให้ผ่านกฎอาจารย์ 100%:**
   - **ปัญหาเดิม:** `TeacherPreferenceApiController.java` เรียก Repositories 7 ตัวโดยตรง ขัดต่อกฎเหล็กข้อ 3 ของอาจารย์ (`doc/prof_ruleset.md`: *"ต้องแยก Layer ชัดเจน และห้ามข้าม Layer เช่น Controller เรียก Repository ตรง ๆ ถือว่าผิด"*) และข้อ 4 (DIP)
   - **การแก้ไข:**
     - สร้าง Service Interface [`TeacherPreferenceService.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/service/TeacherPreferenceService.java)
     - สร้าง Service Implementation [`TeacherPreferenceServiceImpl.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/service/impl/TeacherPreferenceServiceImpl.java) จัดการ Business Logic, Transaction และ Validation ทั้งหมด
     - Refactor `TeacherPreferenceApiController.java` ให้พึ่งพาเฉพาะ `TeacherPreferenceService` ผ่าน Constructor Injection
     - สร้าง [`TeacherPreferenceServiceImplTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/TeacherPreferenceServiceImplTest.java) ครอบคลุมทุก Scenario
     - อัปเดต `class diagram.puml` และ `component-diagram.puml` แทนที่คลาสเดิมด้วย `TeacherPreferenceService`
5. **[Use Case Completeness] การเพิ่ม Use Cases สำหรับอาจารย์ใน `Teacher-UseCase-Diagram.puml`:**
   - **ปัญหาเดิม:** Usecase Diagram ขาดการระบุ Use Case "Manage Course Preferences (D21)" และ "Change Password"
   - **การแก้ไข:** เพิ่ม `UC_TManagePref` (Manage Course Preferences Priority 1-5, D21) และ `UC_TChangePassword` (Change Password) ลงในแพ็กเกจ Profile Modal ใน `doc/diagram/UsecaseDiagram/Teacher-UseCase-Diagram.puml` พร้อมเชื่อมโยงกับ Actor Teacher
6. **[Data Dictionary Detail] การระบุสล็อตเวลาละเอียด 1.5 ชม. (IDs 32–56) ใน `database.md`:**
   - **ปัญหาเดิม:** `database.md` ยังไม่ได้บันทึกสเปกของสล็อตเวลาละเอียด 1.5 ชม. ที่เพิ่มใน `data.sql`
   - **การแก้ไข:** อัปเดตหัวข้อ 2.7 `time_slots` และ 2.12 `teacher_availabilities` ใน `doc/database.md` บันทึกรายละเอียดของ TimeSlot ทั้ง 5 กลุ่ม โดยเฉพาะสล็อต 1.5 ชม. (IDs 32–56 รวม 25 สล็อต) ที่ใช้ในการกำหนดเวลาไม่สะดวกสอนของอาจารย์เพื่อตรวจสอบ Hard Constraint BR-07
7. **[Project Structure & Broken Links] การปรับปรุง `README.md` ให้เป็นมาตรฐาน:**
   - **ปัญหาเดิม:** มี Broken Link ไปยัง `doc/AcadOS-v4.md` (ไม่มีจริง) และโครงสร้างโฟลเดอร์ระบุ `code/src/` แทน `code/acados/`
   - **การแก้ไข:** อัปเดต `README.md` แก้ไขลิงก์ไปยัง `doc/Implement_Plan-AcadOS.md`, ปรับโครงสร้างพาธเป็น `code/acados/`, ระบุสถานะสิ่งที่รอส่งมอบเป็น `TBA` (เช่น Deployment URL) โดยคงข้อมูลสมาชิกกลุ่ม (นายพุฒิเมธ Sec 3, นายวงศกร Sec 4, นายจิรภัทร Sec 3), รหัสนักศึกษา, Git Branches และชื่อโปรเจกต์ AcadOS ไว้อย่างครบถ้วน
8. **[CQRS Pattern Representation] การบันทึก `TeacherSwapQueryService` ใน Diagrams:**
   - **ปัญหาเดิม:** มีการแยก Query ออกจาก Command ตามแนวคิด CQRS ในโค้ดจริง แต่ในไดอะแกรมไม่มีคลาสนี้
   - **การแก้ไข:** เพิ่ม `TeacherSwapQueryService` และ `TeacherSwapQueryServiceImpl` ลงใน `class diagram.puml` และ `component-diagram.puml` สะท้อนสถาปัตยกรรมระบบจริง
9. **[Userflow Consolidation - Hybrid Best Practice] การปรับปรุง Userflow Diagram ให้กระชับ:**
   - **ปัญหาเดิม:** `Userflow_diagram.md` มีขนาดยาวถึง 1,654 บรรทัด และแตก Micro-CRUD ย่อยมากถึง 49 แผนภาพ ทำให้เลื่อนดูยากและเรนเดอร์ช้า
   - **การแก้ไข:** ยุบรวมแผนภาพจาก 49 แผนภาพ เหลือเพียง 19 แผนภาพหลักแบบ Unified User Journey โดยผสาน Micro-CRUD และการทำงานใน Modal เดียวกันเข้าด้วยกัน พร้อมจัดกลุ่มด้วย `<details><summary>` พับเก็บได้ตาม 4 บทบาท (Common, Student, Teacher, Admin) โดยยังคงเงื่อนไข Business Rules BR-01 ถึง BR-10 ครบถ้วน 100%
10. **[Design Patterns Documentation - Prof Ruleset §5] การจัดทำเอกสาร GoF Patterns ที่ใช้งานจริง:**
    - **การดำเนินการ:** จัดทำเอกสาร [`doc/design-patterns.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/design-patterns.md) สรุป Pattern ที่ใช้งานจริงในโค้ด ทั้งกลุ่ม Behavioral (State, Strategy Scoring, Strategy Notification, Observer), Structural (Adapter), และ Creational (Builder, Singleton, Factory Method) พร้อมปัญหาที่แก้, ซอร์สไฟล์ที่ใช้งาน, และ Class Diagrams ประกอบครบถ้วน 100%
11. **[Cross-Artifact Consistency & Gap Resolution] การตรวจสอบและปรับปรุงความสอดคล้องข้ามเอกสารและโค้ด:**
    - **การตรวจสอบ:** Cross-check ระหว่าง `design-patterns.md`, `Userflow_diagram.md`, `Implement_Plan-AcadOS.md`, Usecase Diagrams (`*.puml`), System Diagrams, และ Source Code จริง
    - **การแก้ไข:**
      - ปรับปรุง Observer Pattern ใน `design-patterns.md` ให้ระบุ `NotificationService`/`NotificationServiceImpl` เป็น Concrete Observer ตรงตามโค้ดจริง (ตัดชื่อคลาสสมมติ TeacherScheduleObserver/StudentScheduleObserver ออกตามหลัก Zero Hallucination)
      - ปรับแก้ HTTP Method & Paths ใน `Userflow_diagram.md`: T01 (`/teacher-swaps/my-schedules`), T02 (`PUT /change-password`), A05 (`PUT /sections/{id}/teacher`), A08 (`PUT /{id}/approve` & `PUT /{id}/reject`), A09 (`PUT /sections/{id}/cancel`) ให้ตรงกับ REST Controllers 100%
      - แก้ไข UI Bug ใน `admin/sections.html` โดยเพิ่ม Event Listener ให้แบบฟอร์ม Assign Teacher Modal (`#assign-teacher-form`) เพื่อยิง `PUT /api/v1/sections/{id}/teacher` ได้จริง
      - ลบข้อความ `(TBA)` ของ `RoomSuitabilityScoreStrategy` ใน `component-diagram.puml` หลังมี Implementation และ Unit Tests รองรับ
      - เพิ่ม Use Case Profile Modal (View Profile Info, Change Password) ใน `Student-UseCase-Diagram.puml` และ `Admin-UseCase-Diagram.puml` ให้สอดคล้องกันทุกบทบาทตาม `main-layout.html`
      - เพิ่ม Cross-reference Link ใน `Implement_Plan-AcadOS.md` §17.2 ไปยัง `doc/design-patterns.md`
12. **[SOLID Principles Documentation - Prof Ruleset §4] การจัดทำเอกสารวิเคราะห์ SOLID Principles:**
    - **การดำเนินการ:** จัดทำเอกสาร [`doc/solid-analysis.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/solid-analysis.md) วิเคราะห์และตรวจสอบการปฏิบัติตามหลักการ SOLID Principles ทั้ง 5 ข้อ (SRP, OCP, LSP, ISP, DIP) อย่างละเอียด พร้อมระบุชื่อคลาส หมายเลขบรรทัดจริงในซอร์สโค้ด และเหตุผลทางวิศวกรรมรองรับ 100% ตรงตามข้อกำหนดอาจารย์ใน `doc/prof_ruleset.md` §4

---

## 2. ไฟล์ที่ตรวจสอบ สร้าง และแก้ไขจริงในรอบนี้ (Files Audited, Created & Modified)

### 2.1 Java Source Code & Tests
- [`TeacherPreferenceService.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/service/TeacherPreferenceService.java): สร้างใหม่ Service Interface ตาม Layered Architecture และ DIP
- [`TeacherPreferenceServiceImpl.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/service/impl/TeacherPreferenceServiceImpl.java): สร้างใหม่ Service Implementation รวบรวม Business Logic, Transaction และ Validation
- [`TeacherPreferenceApiController.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/controller/api/TeacherPreferenceApiController.java): Refactor ลด Coupling โดยพึ่งพาเฉพาะ `TeacherPreferenceService`
- [`TeacherPreferenceApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/TeacherPreferenceApiControllerTest.java): ปรับปรุง Unit Test ให้ mock Service Layer
- [`TeacherPreferenceServiceImplTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/TeacherPreferenceServiceImplTest.java): สร้างใหม่ Unit Test 8 ข้อ ครอบคลุม BR-06, BR-07, CRUD, และ Authorization (ผ่าน 100%)
- [`admin/sections.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/admin/sections.html): เพิ่ม Event Listener ให้แบบฟอร์ม Assign Teacher Modal (`#assign-teacher-form`) เพื่อยิง `PUT /api/v1/sections/{id}/teacher` ได้จริง

### 2.2 Documentation & Specification
- [`README.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/README.md): แก้ไข Broken Links, โครงสร้างไดเรกทอรี, ระบุ TBA ตามมติผู้ใช้
- [`doc/database.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/database.md): บันทึกรายละเอียด TimeSlots IDs 32–56 (1.5 ชม.) ในตาราง `time_slots` และ `teacher_availabilities`
- [`doc/Implement_Plan-AcadOS.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/Implement_Plan-AcadOS.md): อัปเดตรายการ Service Layer และ Cross-reference ถึง `design-patterns.md`
- [`doc/design-patterns.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/design-patterns.md): จัดทำเอกสารสรุป GoF Patterns 8 แบบ และ Architectural Patterns พร้อมไดอะแกรมและผลการทดสอบ
- [`doc/solid-analysis.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/solid-analysis.md): จัดทำเอกสารวิเคราะห์ SOLID Principles พร้อมระบุชื่อคลาส หมายเลขบรรทัด และเหตุผลทางวิศวกรรมครบทั้ง 5 ข้อ

### 2.3 System Architecture & UML Diagrams
- [`doc/diagram/class diagram.puml`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/class%20diagram.puml): ปรับปรุง `SectionService` (เพิ่ม `assignTeacher`), เพิ่ม `TeacherPreferenceService`, เพิ่ม `TeacherSwapQueryService`, ลบ `TeacherAssignmentService`
- [`doc/diagram/component-diagram.puml`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/component-diagram.puml): อัปเดต Components และความสัมพันธ์ให้ตรงกับโครงสร้าง Service Layer ล่าสุด
- [`doc/diagram/Userflow_diagram.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/Userflow_diagram.md): อัปเดต Flow วันหยุดราชการในตารางสรุปให้รองรับ Admin Manual Sync
- [`doc/diagram/UsecaseDiagram/*.puml`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/UsecaseDiagram/): เพิ่ม Use Cases Profile Modal (View Profile Info, Change Password, Manage Preferences D21) ให้ครอบคลุมทุกบทบาท
- [`doc/diagram/ActivityDiagram/activity_authentication.puml`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/ActivityDiagram/activity_authentication.puml): แยกการตรวจสอบ 401 Unauthorized vs 403 Forbidden
- [`doc/diagram/ActivityDiagram/activity_schedule_generation.puml`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/ActivityDiagram/activity_schedule_generation.puml): แยก 401 vs 403 ใน Security Filter ของ Generate, Publish, และ Discard Flows
- [`doc/diagram/ActivityDiagram/activity_section_cancellation.puml`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/ActivityDiagram/activity_section_cancellation.puml): แยก 401 vs 403 ใน Security Filter ของ Admin Cancellation
- [`doc/diagram/ActivityDiagram/activity_student_registration.puml`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/ActivityDiagram/activity_student_registration.puml): แยก 401 vs 403 ใน Security Filter ของ Student Registration
- [`doc/diagram/ActivityDiagram/activity_teacher_swap.puml`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/ActivityDiagram/activity_teacher_swap.puml): แยก 401 vs 403 ใน Security Filter ของ Create, Cancel, Respond, และ Admin Review Flows

---

## 3. สรุปผลการทดสอบทางวิศวกรรม (Verification & Test Results)

- **ชุดทดสอบ Unit & Integration Tests ทั้งระบบ:**
  - รันคำสั่ง: `mvn clean test`
  - ผลลัพธ์: **326 Tests Run, 0 Failures, 0 Errors, 0 Skipped (BUILD SUCCESS 100% Green)**
- **การปฏิบัติตามกฎเกณฑ์ของอาจารย์ (`doc/prof_ruleset.md`):**
  - **ข้อ 3 (Layered Architecture):** ผ่าน 100% — ไม่มีการข้าม Layer จาก Controller ไปยัง Repository โดยทุก Controller เรียกผ่าน Service Interface
  - **ข้อ 4 (SOLID Principles):** ผ่าน 100% — ปฏิบัติตาม DIP (Controller พึ่งพา Interface), SRP (Controller คุมเฉพาะ Web/HTTP, Service คุม Business Logic), และ ISP
  - **ข้อ 5 (Design Patterns):** ผ่าน 100% — Layered Architecture, Repository Pattern, Service Layer Pattern, CQRS (TeacherSwapQueryService), DTO + Mapper

---

## 4. สถานะและขั้นตอนต่อไป (Readiness & Next Steps)

1. **Git Synchronization:** พร้อมสำหรับ Stage และ Commit การเปลี่ยนแปลงทั้งหมดใน Branch `puttimed_6733804171_03`
2. **System Consistency:** โค้ดจริง ไดอะแกรม เอกสารข้อกำหนด และ Data Dictionary สอดคล้องตรงกัน 100% โดยไม่มีข้อขัดแย้งตกค้าง
