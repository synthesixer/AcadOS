# AcadOS — Project Status & Task Summary

**บันทึกสถานะโครงการและผลการดำเนินงานทางวิศวกรรมซอฟต์แวร์**  
**อัปเดตล่าสุด:** 10 ตุลาคม 2569  
**Branch:** `puttimed_6733804171_03`  
**สถานะการทดสอบล่าสุด:** **247/247 Tests Passed (100% Green, 0 Failures, 0 Errors)**  

---

## 1. วัตถุประสงค์และขอบเขตงานล่าสุด (Objective & Scope)
1. **JUnit 5 + Mockito Unit & Slice Testing Suite (Complete Coverage):**
   - **Phase 1: Service Layer Unit Tests (Mockito Pure Unit Tests)**
     - `CourseServiceTest`: ทดสอบ CRUD รายวิชา, ตรวจสอบความถูกต้องของรหัสวิชาซ้ำ (Conflict Exception), การค้นหาแบบ Paging และแบบ All
     - `RoomServiceTest`: ทดสอบ CRUD ห้องเรียน, ตรวจสอบเงื่อนไขชื่ออาคารและเลขห้องซ้ำ (Conflict Exception), การอัปเดตสถานะความพร้อมใช้งาน
     - `TeacherSwapServiceTest`: ทดสอบ Lifecycle ของ Teacher Swap อย่างสมบูรณ์ (การสร้างคำขอ, การตรวจสอบคุณสมบัติวิชาที่สอน BR-06, ความพร้อมสอน BR-07, ตารางชน DRAFT/PUBLISHED, การยอมรับ/ปฏิเสธคำขอ, การยกเลิกคำขอโดยผู้ขอ, การอนุมัติและการปฏิเสธโดยผู้ดูแลระบบ พร้อม Observer Notification และการจัดรูปแบบ Inbox Response)
     - `UserServiceTest`: ทดสอบการสร้างบัญชีผู้ใช้ TEACHER / STUDENT, ป้องกันการสร้าง ADMIN นอกระบบ, ป้องกัน University ID และ Email ซ้ำ, การแก้ไขข้อมูลโปรไฟล์, และการลบบัญชีพร้อม Cascade Cleanup ที่เกี่ยวข้อง
   - **Phase 2: Security Layer Unit Tests**
     - `TokenProviderTest`: ทดสอบการสร้าง JWT Token, การดึง Username, การตรวจสอบความถูกต้องของ Token, Token ของผู้ใช้อื่น, และการหมดอายุ (Token Expiration)
     - `CustomUserDetailsServiceTest`: ทดสอบการโหลดข้อมูลผู้ใช้ด้วย University ID, การค้นหาสำรองด้วย Email, และ UsernameNotFoundException
     - `JwtAuthenticationFilterTest`: ทดสอบการยืนยันตัวตนผ่าน Authorization Bearer Header, Cookie Token, การปฏิเสธ Invalid Token, และการปล่อย Request ผ่านเมื่อไม่มี Token
   - **Phase 3: Controller Layer WebMvc Tests (MockMvc + MockBean)**
     - `TeacherSwapApiControllerTest`: ทดสอบ Endpoint การดูคำขอสลับสอน, การสร้างคำขอ (201 Created), การตอบรับ/ปฏิเสธ, การอนุมัติ/ปฏิเสธโดย Admin, และการสกัดกั้น Student ไม่ให้เข้าถึง Endpoint ของ Admin (403 Forbidden)
     - `UserApiControllerTest`: ทดสอบ Endpoint การบริหารจัดการผู้ใช้สำหรับ Admin (List, Get, Create, Update, Delete) และการปฏิเสธผู้ใช้ทั่วไป (403 Forbidden)
     - `HolidayApiControllerTest`: ทดสอบ Endpoint การเรียกดูวันหยุดราชการ และการ Trigger Sync โดย Admin (พร้อม Role Restriction 403 Forbidden สำหรับบทบาทอื่น)
2. **Phase 4: Verification & Regression Test Run:**
   - รันชุดทดสอบทั้งโปรเจกต์ `mvn test` ผ่านครบถ้วน **247/247 Tests Passed (100% Green)**

---

## 2. ไฟล์ที่ตรวจสอบและสร้าง/แก้ไขจริง (Files Reviewed & Created)

### 2.1 Service Layer Tests
- [`CourseServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/CourseServiceTest.java): 10 tests
- [`RoomServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/RoomServiceTest.java): 9 tests
- [`TeacherSwapServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/TeacherSwapServiceTest.java): 10 tests
- [`UserServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/UserServiceTest.java): 11 tests

### 2.2 Security Layer Tests
- [`TokenProviderTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/security/TokenProviderTest.java): 5 tests
- [`CustomUserDetailsServiceTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/security/CustomUserDetailsServiceTest.java): 3 tests
- [`JwtAuthenticationFilterTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/security/JwtAuthenticationFilterTest.java): 4 tests

### 2.3 Controller Layer Slice Tests
- [`TeacherSwapApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/TeacherSwapApiControllerTest.java): 7 tests
- [`UserApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/UserApiControllerTest.java): 6 tests
- [`HolidayApiControllerTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/controller/api/HolidayApiControllerTest.java): 3 tests

---

## 3. ผลการทดสอบ (Verification Results)
- คำสั่ง: `mvn test`
- ผลลัพธ์: **247/247 Tests Run, 0 Failures, 0 Errors, 0 Skipped (BUILD SUCCESS)**
- สรุปความครอบคลุม:
  - **Unit Tests:** Service Layer (Course, Room, TeacherSwap, User, Registration, Section, Scheduling, Holiday, Notification, AcademicEvent)
  - **Pattern & Strategy Tests:** Observer (`ScheduleChangeObserver`), State (`SectionState`), Scoring (`Preference`, `Workload`, `RoomSuitability`), Holiday Adapter (`ExternalHolidayAdapter`)
  - **Security Tests:** `TokenProvider`, `CustomUserDetailsService`, `JwtAuthenticationFilter`
  - **Controller WebMvc Tests:** REST APIs ทั้งหมด (`TeacherSwap`, `User`, `Holiday`, `Course`, `Room`, `Schedule`, `AcademicEvent`, `TrackCApiControllerTests`)
  - **Integration Tests:** End-to-End Database & Domain Integrity (`Day1CrossEntityIntegrationTest`, `SchedulingServiceIntegrationTest`, `UserProfileAndAuthIntegrationTest`)

---

## 4. ผลการวัดระดับ Code Coverage ด้วย JaCoCo (Baseline Measurement)

- **เครื่องมือ:** `jacoco-maven-plugin:0.8.12` (รองรับ Java 21)
- **แหล่งข้อมูลรายงาน:** `target/site/jacoco/jacoco.csv`, `target/site/jacoco/index.html`
- **จำนวนคลาสทั้งหมดที่ตรวจวัด (Analyzed Classes):** 91 Classes

### 4.1 สรุป Code Coverage ราย Package
| Package | Line Coverage (%) | Branch Coverage (%) | ประเมินสถานะ |
| :--- | :---: | :---: | :---: |
| `domain.entity` | **100.00%** | **87.50%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `state` (Section State Machine) | **100.00%** | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `domain.enums` | **100.00%** | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `dto.request` | **100.00%** | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `notification.strategy` | **100.00%** | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `config` | **100.00%** | **100.00%** | ✅ ผ่านเกณฑ์ระดับสูง |
| `security` | **97.22%** | **65.62%** | ✅ Line ผ่าน / Branch เฝ้าระวัง |
| `service` (Constraint/Selector) | **92.91%** | **82.61%** | ✅ ผ่านเกณฑ์ ($\ge$ 85%) |
| `dto.response` | **90.54%** | **61.54%** | ✅ ผ่านเกณฑ์ |
| `exception` | **89.47%** | **66.67%** | ✅ ผ่านเกณฑ์ |
| `controller.api` | **87.38%** | **50.00%** | ✅ Line ผ่าน ($\ge$ 85%) |
| `service.impl` | **86.79%** | **67.71%** | ✅ Line ผ่าน ($\ge$ 85%) |
| `strategy` (Scoring Strategy) | **79.31%** | **58.82%** | ⚠️ ปรับปรุงเพิ่มอีก 6% เพื่อแตะ 85% |
| `mapper` | **78.82%** | **53.85%** | ℹ️ MapStruct Generated |
| `pattern.observer` | **75.00%** | **100.00%** | ℹ️ Observer Subject/Publisher |
| `pattern.holiday` (External Adapter)| **62.22%** | **34.29%** | ⚠️ ต้องการ Negative & Resilience Tests |

### 4.2 สรุป Service Implementation Coverage ราย Class
| Class ใน `service.impl` | Line Coverage (%) | Branch Coverage (%) |
| :--- | :---: | :---: |
| `NotificationServiceImpl` | **100.00%** | **95.45%** |
| `RoomServiceImpl` | **100.00%** | **71.43%** |
| `SectionServiceImpl` | **100.00%** | **100.00%** |
| `CourseServiceImpl` | **100.00%** | **83.33%** |
| `SectionCancellationServiceImpl` | **100.00%** | **87.50%** |
| `RegistrationServiceImpl` | **98.41%** | **96.15%** |
| `TeacherSwapServiceImpl` | **91.67%** | **60.00%** |
| `TeacherSwapQueryServiceImpl` | **87.27%** | **40.91%** |
| `HolidayServiceImpl` | **77.78%** | **83.33%** |
| `SchedulingServiceImpl` | **76.22%** | **59.21%** |
| `UserServiceImpl` | **70.59%** | **50.00%** |
| `AcademicEventServiceImpl` | **70.37%** | **62.50%** |

