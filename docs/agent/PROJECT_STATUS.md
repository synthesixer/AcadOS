# AcadOS — Project Status & Task Summary

**บันทึกสถานะโครงการและผลการดำเนินงานทางวิศวกรรมซอฟต์แวร์**  
**อัปเดตล่าสุด:** 10 ตุลาคม 2569  
**Branch:** `puttimed_6733804171_03`  
**สถานะการทดสอบล่าสุด:** **172/172 Tests Passed (100% Green)**  

---

## 1. วัตถุประสงค์และขอบเขตงานล่าสุด (Objective & Scope)
1. **Timetable Workbench Multi-Perspective View:** เพิ่มแถบเครื่องมือ View Filter Toolbar บนหน้า Timetable Workbench (`grid.html`) ให้ผู้ดูแลระบบ (Admin) และผู้ใช้งานสามารถเลือกเปิดดูตารางสอนของทุกคนได้อย่างยืดหยุ่น (ดูผังรวม Master, ดูตามอาจารย์รายบุคคล, ดูตามห้องเรียน, หรือดูตามรายวิชา/กลุ่มเรียน)
2. **ผังการสอนกำหนดเวลาจริง (Flexible Period Scheduling):** ปรับโครงสร้างตารางเรียนจากเดิมที่เป็นช่วงตายตัว (เช้า 3 ชม. / บ่าย 3 ชม.) ให้เป็น Academic Timeline Grid (09:00 - 17:00 น.) รองรับคาบสอน 1 ชั่วโมง, 2 ชั่วโมง (เช่น 09:00 - 11:00 น.), 3 ชั่วโมง พร้อมป้ายระบุระยะเวลาที่ถูกต้อง
3. **การสอน 2 คาบต่อสัปดาห์ (Multi-session per Section):** รองรับกรณี 1 กลุ่มเรียนมีการสอน 2 รอบต่อสัปดาห์ (เช่น รายวิชาภาษาอังกฤษ `EN012001 Technical English for Computing` สอน 2 คาบ คาบละ 2 ชั่วโมง รวม 4 ชั่วโมง/สัปดาห์) ทั้งในระดับ Data Seed, Scheduling Engine Algorithm (`SchedulingServiceImpl.java`), และ UI Detailed Registry

---

## 2. ไฟล์ที่ตรวจสอบและแก้ไขจริง (Files Reviewed & Modified)

### 2.1 Backend & Algorithms
- [`SchedulingServiceImpl.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/java/com/project/acados/service/impl/SchedulingServiceImpl.java): ปรับปรุงฟังก์ชัน `generateSlotCombinations` ให้คำนวณ Duration ของแต่ละ TimeSlot และจับคู่ Combination ตามผลรวมชั่วโมง (`targetHours`) ทำให้รองรับทั้งวิชา 4 ชั่วโมงที่แยกสอน 2 คาบละ 2 ชม. และวิชา 1, 2, 3 ชม. พร้อม Count-based Fallback
- [`SchedulingServiceIntegrationTest.java`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/test/java/com/project/acados/service/SchedulingServiceIntegrationTest.java): เพิ่มชุดทดสอบ `testMultiSessionTwoHourScheduling()` เพื่อตรวจสอบการสร้าง Draft Schedule 2 คาบ/สัปดาห์ คาบละ 2 ชั่วโมง รวม 4 ชั่วโมง

### 2.2 Data Seed & Schema
- [`data.sql`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/data.sql) & [`code/acados/data.sql`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/data.sql):
  - เพิ่ม TimeSlots ขนาด 2 ชั่วโมง (09:00 - 11:00 และ 13:00 - 15:00) สำหรับวันจันทร์ พุธ ศุกร์ (IDs: 26-31)
  - เพิ่ม Course `EN012001 Technical English for Computing` (weekly_hours = 4)
  - เพิ่ม Section 8 (`EN012001` Sec 1, capacity 40, ACTIVE)
  - เพิ่ม Teacher Qualification & Preference ให้ T002 สอนวิชานี้
  - เพิ่ม Pre-seeded Schedules คาบที่ 1 (จันทร์ 09:00-11:00) และคาบที่ 2 (พุธ 09:00-11:00) สถานะ PUBLISHED

### 2.3 Frontend & UX/UI
- [`grid.html`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/templates/timetable/grid.html):
  - เพิ่ม View Filter Toolbar: ตัวเลือกมุมมอง (ผังรวม Master, ตามอาจารย์, ตามห้องเรียน, ตามกลุ่มเรียน), Dropdown เป้าหมายที่แสดงผลแบบไดนามิก, ช่องค้นหาด่วน, และปุ่มล้างตัวกรอง
  - ปรับโครงสร้าง Timetable Matrix ให้เป็นแบบ Hourly Grid (09:00 - 17:00 น.) พร้อมช่วงพักเที่ยง (12:00 - 13:00 น.) และใช้ Track Allocation จัดเรียงคาบสอนที่มี Colspan ตามระยะเวลาจริง ป้องกันปัญหาข้อความล้นหรือตารางผิดสัดส่วน
  - ปรับ Detailed Registry ด้านล่าง ให้จับกลุ่มตาม Section พร้อมแสดงป้าย `⚡ สอน 2 คาบ/สัปดาห์ (รวม 4 ชม.)` และรายละเอียดคาบเรียนแต่ละรอบอย่างชัดเจน
- [`app.css`](file:///e:/Doc/Code/GitHub/SQA/AcadOS/code/acados/src/main/resources/static/css/app.css): เพิ่มคลาส `.view-filter-bar`, `.timetable-card`, `.timetable-empty-cell`, `.timetable-lunch-cell`, ปรับแต่ง `.timetable-grid`

### 2.4 Documentation
- ลบคำต้องห้าม (`v4`, `SQA`, `Calm Command Center`, `Nager`) ออกจากเอกสาร `Implement_Plan-AcadOS.md`, `database.md`, `TEAM_WORK_DIVISION.md`, และ `data.sql`

---

## 3. ผลการทดสอบ (Verification Results)
- คำสั่ง: `mvn test`
- ผลลัพธ์: **172/172 Tests Run, 0 Failures, 0 Errors, 0 Skipped (BUILD SUCCESS)**
- ครอบคลุม:
  - Unit Tests ทุก Controller, Service, Domain Entity, Repository
  - Hard Constraint Verification (BR-01 ถึง BR-08)
  - Multi-session & Custom Duration Scheduling Integration Tests
  - Web Controller View Rendering Tests

