# AcadOS — Project Status & Task Summary

**บันทึกสถานะโครงการและผลการดำเนินงานทางวิศวกรรมซอฟต์แวร์**  
**อัปเดตล่าสุด:** 10 ตุลาคม 2569  
**Branch:** `puttimed_6733804171_03`  
**สถานะการทดสอบล่าสุด:** **314/314 Tests Passed (100% Green, 0 Failures, 0 Errors)**  

---

## 1. วัตถุประสงค์และขอบเขตงานล่าสุด (Objective & Scope)
1. **[Web Route Security & 401 Alignment] ควบคุมสิทธิ์การเข้าถึงหน้าเว็บ Admin ตามเงื่อนไขใน Activity Diagrams ทั้งหมด:**
   - แก้ไขปัญหาบทบาทอื่น (เช่น `TEACHER`, `STUDENT`) หรือผู้ใช้ที่ยังไม่ล็อกอิน สามารถเข้าถึงหน้าเว็บ Admin (`/admin/dashboard`, `/admin/swaps`, ฯลฯ)
   - ปรับปรุง `SecurityConfig.java`:
     - เพิ่มข้อกำหนดสิทธิ์หน้าเว็บ: `.requestMatchers("/admin/**").hasRole("ADMIN")`, `.requestMatchers("/teacher/**").hasRole("TEACHER")`, `.requestMatchers("/student/**").hasRole("STUDENT")`
     - กำหนด `accessDeniedHandler` และ `authenticationEntryPoint` ให้ส่งคืนสถานะ HTTP 401 Unauthorized สำหรับการปฏิเสธการเข้าถึงหน้าเว็บตามเงื่อนไขใน Security Filter ของ Activity Diagrams ทั้ง 5 ฉบับ
     - อนุญาต Public Access สำหรับ `/api-docs/**`, `/swagger-ui/**`, และ `/error`
2. **[Neutral HTML Error Page Architecture] ออกแบบและสร้างหน้าจอข้อผิดพลาด HTML กลาง (templates/error.html):**
   - รวมศูนย์การแสดงผล Error Page ของระบบทั้งหมดไว้ใน `templates/error.html` เพียงหน้าเดียว ภายใต้แนวคิด "Neutral Structural Shell + Dynamic Semantics"
   - รองรับ HTTP Status Codes ครอบคลุม: **401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 400 Bad Request, 500 Internal Server Error** และ Default Fallback
   - แสดงผล Dynamic SVG Icons, หัวข้อภาษาไทย, และคำอธิบายเฉพาะเจาะจงตามประเภทข้อผิดพลาด
   - มีปุ่ม Action Buttons ชาญฉลาด:
     - ปุ่ม **"← ย้อนกลับไปแก้ไข (Go Back)"** สำหรับสถานะ **409 Conflict** และ **400 Bad Request**
     - ปุ่ม **"เข้าสู่ระบบใหม่ (Sign In)"** สำหรับสถานะ **401 Unauthorized**
     - ปุ่ม **"ไปยังหน้าภาพรวม (${role} Dashboard)"** นำทางตามบทบาทของผู้ใช้จาก Session อัตโนมัติ
     - รายละเอียดข้อมูลทางเทคนิค (Path, Timestamp, Reason) ใน Accordion สำหรับ IT/Developer
   - ยืนยันการคงไว้ซึ่ง JSON `ErrorResponse` DTO ตามข้อกำหนด §16.1 สำหรับ REST API (`/api/v1/**`) 100%
3. **[Timetable Consecutive Schedule Merging] ผังตารางประจำสัปดาห์ — รวมคาบสอนต่อเนื่องเป็นช่องเดียว:**
   - ปรับปรุง `timetable/grid.html` เพิ่มฟังก์ชัน `mergeConsecutiveSchedules` รวมคาบสอนที่สอนต่อเนื่องกัน (เช่น คาบ 2 และ 3: 10:00 - 11:00 และ 11:00 - 12:00 รวมเป็น 10:00 - 12:00, 2 ชม.) ให้แสดงผลเป็น **"ช่องเดียว (Single Unified Block)"** ด้วย `colSpan = 2`
   - ปรับปรุงการคำนวณ session ใน Detailed Registry (`renderRibbonList`) และระบบตารางสอนในอาจารย์ (`teacher/dashboard.html`) ให้แสดงผลช่วงเวลาที่รวมกันอย่างถูกต้อง ไม่แตกเป็นคาบย่อย
4. **[Testing & Verification] รันการทดสอบครอบคลุมทั้งระบบ:**
   - รันชุดทดสอบทั้งระบบ `mvn test` ผ่านครบถ้วน **314/314 Tests Passed (100% Green, 0 Failures, 0 Errors)**

---

## 2. ไฟล์ที่ตรวจสอบและสร้าง/แก้ไขจริง (Files Reviewed & Created)

### 2.1 Service Layer Tests & Implementation
- [`SectionService.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/service/SectionService.java): เพิ่มเมธอด `assignTeacher` และ `getTeacherOptions`
- [`SectionServiceImpl.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/service/impl/SectionServiceImpl.java): รองรับ Sub-feature A13 ตรวจสอบ BR-06, BR-07, BR-01 และส่ง Notification
- [`SectionServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/SectionServiceTest.java): อัปเดตเป็น 22 tests (เพิ่ม DRAFT conflict & null query)
- [`TeacherSwapQueryServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/TeacherSwapQueryServiceTest.java): สร้างใหม่ 3 tests
- [`CourseServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/CourseServiceTest.java): 10 tests
- [`RoomServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/RoomServiceTest.java): 9 tests
- [`TeacherSwapServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/TeacherSwapServiceTest.java): 10 tests
- [`UserServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/UserServiceTest.java): 11 tests

### 2.2 Controller Layer, Security & Web Tests
- [`SecurityConfig.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/config/SecurityConfig.java): ปรับปรุง Web Route Security บังคับ Role-based access (`/admin/**`, `/teacher/**`, `/student/**`) และ 401 Unauthorized handling
- [`TimetableWebController.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/controller/web/TimetableWebController.java): เพิ่ม Route Mapping `/admin/timetable` ควบคู่ `/admin/schedules`
- [`DashboardWebControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/web/DashboardWebControllerTest.java): สร้างใหม่ 19 WebMvc Slice Tests ครอบคลุมการเข้าถึงทุก Dashboard และการปฏิเสธสิทธิ์ด้วย 401 Unauthorized
- [`TimetableWebControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/web/TimetableWebControllerTest.java): อัปเดตเป็น 10 Tests เพิ่มการทดสอบ `/admin/timetable` และสิทธิ์ 401 Unauthorized
- [`AssignTeacherRequest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/dto/request/AssignTeacherRequest.java): DTO มอบหมายอาจารย์
- [`TeacherAssignmentOptionResponse.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/dto/response/TeacherAssignmentOptionResponse.java): DTO ตัวเลือกอาจารย์พร้อมสถานะ Qualification
- [`SectionApiController.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/controller/api/SectionApiController.java): เพิ่ม `PUT /{id}/teacher` และ `GET /{id}/teachers`
- [`SectionApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/SectionApiControllerTest.java): 14 WebMvc Slice Tests ครอบคลุม CRUD เต็มรูปแบบและ Security
- [`TeacherPreferenceApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/TeacherPreferenceApiControllerTest.java): 9 WebMvc Slice Tests (BR-06/07)
- [`AuthApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/AuthApiControllerTest.java): 6 WebMvc Slice Tests
- [`TeacherSwapApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/TeacherSwapApiControllerTest.java): 7 tests
- [`UserApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/UserApiControllerTest.java): 6 tests
- [`HolidayApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/HolidayApiControllerTest.java): 3 tests

### 2.3 HTML Templates & UI Action Enhancements
- [`error.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/error.html): Neutral HTML Error Page เต็มรูปแบบ รองรับ 401, 403, 404, 409 Conflict, 400, 500 พร้อม SVG Icons, ข้อความเฉพาะสถานะ, ปุ่มย้อนกลับไปแก้ไข, และ Role Dashboard
- [`grid.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/timetable/grid.html): เพิ่ม `mergeConsecutiveSchedules` รวมคาบสอนต่อเนื่อง (เช่น คาบ 2 และ 3, 2 ชม.) ให้แสดงผลเป็นช่องเดียว (`colSpan = 2`) ในตารางสัปดาห์
- [`teacher/dashboard.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/teacher/dashboard.html): ปรับปรุงให้รวมคาบสอนต่อเนื่องสำหรับ upcoming class และรายการคาบสอนของอาจารย์
- [`ui.js`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/static/js/ui.js): เพิ่ม Client-side Route Guard ป้องกันและ Redirect เมื่อผู้ใช้เข้าถึงเส้นทางไม่ตรงกับสิทธิ์
- [`admin/sections.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/admin/sections.html): เพิ่มปุ่มและ Modal "มอบหมายผู้สอน" (Sub-feature A13)
- [`admin/courses.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/admin/courses.html): เพิ่มปุ่มและ Modal "แก้ไขรายวิชา"
- [`admin/rooms.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/admin/rooms.html): เพิ่มปุ่มและ Modal "แก้ไขห้องเรียน"
- [`admin/users.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/admin/users.html): เพิ่มปุ่มและ Modal "แก้ไขผู้ใช้งาน"
- [`timetable/calendar.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/timetable/calendar.html): เพิ่มปุ่มและ Modal "แก้ไขกิจกรรมในปฏิทิน"

### 2.4 Diagram & Documentation
- [`Usecase_diagram.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/Usecase_diagram.md): ปรับแก้ Note วันหยุดราชการ และ Profile Modal
- [`Userflow_diagram.md`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/doc/diagram/Userflow_diagram.md): ปรับแก้ A10-1 (Manual Sync) และ T02-T04 (Profile Modal)

---

## 3. ผลการทดสอบ (Verification Results)
- คำสั่ง: `mvn test`
- ผลลัพธ์: **314/314 Tests Run, 0 Failures, 0 Errors, 0 Skipped (BUILD SUCCESS)**
- สรุปความครอบคลุม:
  - **Unit Tests:** Service Layer ครบทุก Use Case, Exception paths, Null-safety
  - **Pattern & Strategy Tests:** Observer, State, Scoring Strategy, External Holiday Adapter
  - **Security Tests:** `TokenProvider`, `CustomUserDetailsService`, `JwtAuthenticationFilter`, `AuthApiController`
  - **Controller WebMvc Tests:** REST APIs ทั้งหมด และ Web Controllers ทั้งหมด (`DashboardWebControllerTest`, `TimetableWebControllerTest`, `LayoutRenderingTests`, ฯลฯ)
  - **Integration Tests:** Database & Domain Integrity (`Day1CrossEntityIntegrationTest`, `SchedulingServiceIntegrationTest`, `UserProfileAndAuthIntegrationTest`, `TrackCApiIntegrationTest`)

---

## 4. ผลการวัดระดับ Code Coverage ด้วย JaCoCo (Updated Measurement)

- **เครื่องมือ:** `jacoco-maven-plugin:0.8.12` (รองรับ Java 21)
- **แหล่งข้อมูลรายงาน:** `target/site/jacoco/jacoco.csv`, `target/site/jacoco/index.html`
- **จำนวนคลาสทั้งหมดที่ตรวจวัด (Analyzed Classes):** 93 Classes
- **Total Line Coverage รวมทั้งระบบ:** **88.40%** (1,684 / 1,905 lines)
- **Total Instruction Coverage รวมทั้งระบบ:** **88.94%** (7,263 / 8,166 instructions)

### 4.1 สรุป Code Coverage ราย Package
| Package | Line Coverage (%) | ประเมินสถานะ |
| :--- | :---: | :---: |
| `domain.entity` | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `state` (Section State Machine) | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `domain.enums` | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `dto.request` | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `notification.strategy` | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `config` | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `security` | **97.22%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `service` (Constraint/Selector) | **92.91%** | ✅ ผ่านเกณฑ์ ($\ge$ 85%) |
| `controller.api` | **92.43%** | ✅ ผ่านเกณฑ์ ($\ge$ 85%) (เพิ่มขึ้นจาก 87.38%) |
| `dto.response` | **90.67%** | ✅ ผ่านเกณฑ์ |
| `exception` | **89.47%** | ✅ ผ่านเกณฑ์ |
| `service.impl` | **88.58%** | ✅ ผ่านเกณฑ์ ($\ge$ 85%) (เพิ่มขึ้นจาก 86.79%) |
| `strategy` (Scoring Strategy) | **79.31%** | ⚠️ ปรับปรุงเพิ่มอีก 6% เพื่อแตะ 85% |
| `mapper` | **78.82%** | ℹ️ MapStruct Generated |
| `pattern.observer` | **75.00%** | ℹ️ Observer Subject/Publisher |
| `pattern.holiday` (External Adapter)| **62.22%** | ⚠️ ต้องการ Negative & Resilience Tests |

### 4.2 สรุป Service Implementation Coverage ราย Class
| Class ใน `service.impl` | Line Coverage (%) |
| :--- | :---: |
| `NotificationServiceImpl` | **100.00%** |
| `RoomServiceImpl` | **100.00%** |
| `TeacherSwapQueryServiceImpl` | **100.00%** (เพิ่มขึ้นจาก 87.27%) |
| `CourseServiceImpl` | **100.00%** |
| `SectionCancellationServiceImpl` | **100.00%** |
| `RegistrationServiceImpl` | **98.41%** |
| `SectionServiceImpl` | **97.65%** |
| `TeacherSwapServiceImpl` | **91.67%** |
| `HolidayServiceImpl` | **77.78%** |
| `SchedulingServiceImpl` | **76.22%** |
| `UserServiceImpl` | **70.59%** |
| `AcademicEventServiceImpl` | **70.37%** |

---

## 5. การตัดสินใจที่ได้รับการยืนยันและ Requirements ที่เกี่ยวข้อง (Confirmed Decisions & Requirements)

- **การ Sync วันหยุดราชการ (Public Holidays):**
  - ยืนยันการอิงตาม HTML จริง (`timetable/holidays.html`) ที่มีปุ่ม `btn-sync` ให้ Admin สามารถกด Trigger Sync แบบ Manual ได้ทันที นอกเหนือจากการดึงแบบตั้งเวลาอัตโนมัติ
  - ปรับปรุง `doc/diagram/Usecase_diagram.md` (UC_FetchHoliday) และ `doc/diagram/Userflow_diagram.md` (A10-1) ให้ตรงตามพฤติกรรมจริง
- **การจัดการคุณสมบัติและความพร้อมของอาจารย์ (Teacher Qualification & Availability):**
  - ยืนยันตำแหน่งการเข้าถึงว่าอยู่ใน **Profile Modal** บนแถบเมนูด้านบน (`layout/main-layout.html`) แทนการมีเมนูแยกต่างหาก
  - ปรับปรุงข้อความใน Use Case และ Userflow Diagram ให้สอดคล้องกัน
- **Sub-feature A13 มอบหมายผู้สอน (Assign Teacher to Section):**
  - อิงตาม Business Rules BR-01 (ตรวจตารางชนทั้งสถานะ PUBLISHED และ DRAFT), BR-06 (อาจารย์ต้องมีคุณสมบัติผ่านการรับรอง), BR-07 (อาจารย์ต้องพร้อมสอนในช่วงเวลาดังกล่าว), และ BR-08 (Section ต้องจัดตารางและ Publish แล้ว)
  - ส่งการแจ้งเตือน Notification (In-App) แก่อาจารย์ใหม่, อาจารย์เดิม (ถ้ามี), และนักศึกษาในกลุ่มเรียน
- **ความสมบูรณ์ของ CRUD Actions ในหน้า Admin:**
  - เพิ่ม Action "แก้ไข (Update)" ให้ครบวงจรในหน้า Courses, Rooms, Users, และ Academic Events

---

## 6. ข้อสังเกตและความเสี่ยงที่บันทึกไว้ (Known Issues & Observations)

1. **External Holiday API Adapter:**
   - Code coverage ของ `pattern.holiday` อยู่ที่ 62.22% เนื่องจากมีการเชื่อมโยงกับ Mock Web Server และ API ภายนอก หากต้องการยกระดับความทนทาน ควรเพิ่ม Circuit Breaker หรือ Negative Resilience Tests เพิ่มเติม
2. **Scoring Strategy Edge Cases:**
   - Code coverage ของ `strategy` อยู่ที่ 79.31% ซึ่งเกือบแตะเกณฑ์ 85% สามารถเพิ่มเติม Boundary Tests สำหรับกรณีคะแนนชนกัน (Tie-breaker) ได้ในรอบถัดไป
3. **Database Migration Consistency:**
   - ไฟล์ `code/acados/data.sql` มีการปรับปรุงข้อมูลเริ่มต้นเป็นชื่อวิชาและห้องเรียนที่สมจริง ไม่ส่งผลกระทบต่อ Automated Tests เนื่องจากรันบนฐานข้อมูล In-Memory H2 ที่แยกชุดทดสอบอิสระ

---

## 7. งานที่พร้อมดำเนินการต่อ (Next Steps & Readiness)

1. **Git Commit & Push:**
   - ทำการ Stage และ Commit โค้ดทั้งหมดที่ผ่านการทดสอบ 100% Green เข้าสู่ Git Branch `puttimed_6733804171_03`
2. **Quality Audit & Final Handover:**
   - ทุกข้อกำหนดและเกณฑ์คุณภาพทางวิศวกรรมซอฟต์แวร์ได้รับการตรวจสอบและบันทึกหลักฐานครบถ้วน พร้อมส่งมอบให้ทีมงานหรือผู้ใช้ตรวจสอบ

