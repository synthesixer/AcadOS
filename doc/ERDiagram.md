# AcadOS — ส่วนที่เพิ่มในเอกสารฐานข้อมูล

ต่อท้าย `database.md` (หลัง §4 ER Diagram) เป็น §5 และ §6

---

## 5. Indexes

MySQL (InnoDB) สร้าง index ให้อัตโนมัติสำหรับ **Primary Key, UNIQUE และ Foreign Key** จึงไม่ต้องสร้างซ้ำ ตารางนี้แสดงทั้งส่วนที่ได้มาอัตโนมัติ และ index เพิ่มเติมที่สร้างเองตาม query ที่ใช้บ่อย

### 5.1 Index ที่ได้มาอัตโนมัติ (ไม่ต้องสร้างเอง)

| ตาราง | Index | ที่มา | ใช้กับ query |
| :--- | :--- | :--- | :--- |
| users | `university_id`, `email` | UNIQUE | Login, ตรวจซ้ำตอนสร้างบัญชี |
| students / teachers | `user_id` | UNIQUE + FK | หา profile จากบัญชี |
| courses | `course_code` | UNIQUE | ค้นหาวิชา |
| sections | `(course_id, section_number)` | UNIQUE (นำหน้าด้วย `course_id` จึงใช้เป็น index ของ FK ด้วย) | รายการ Section ของวิชา |
| sections | `room_id` | FK | หา Section ที่ใช้ห้องนี้ (BR-08, ลบห้อง) |
| rooms | `(building, room_number)` | UNIQUE | ตรวจห้องซ้ำ |
| time_slots | `(day_of_week, start_time, end_time)` | UNIQUE | ตรวจ slot ซ้ำ |
| schedules | `(section_id, time_slot_id)` | UNIQUE (นำหน้าด้วย `section_id`) | คาบของ Section |
| schedules | `(teacher_id, time_slot_id)` | UNIQUE (นำหน้าด้วย `teacher_id`) | ตารางสอนอาจารย์ / ตรวจ Teacher Conflict |
| schedules | `time_slot_id` | FK | หาคาบตามช่วงเวลา |
| registrations | `(student_id, section_id)`, `(student_id, course_id)` | UNIQUE | ตารางเรียนของนักศึกษา / ตรวจซ้ำ (D24, BR-04) |
| registrations | `section_id`, `course_id` | FK | นับที่นั่ง / ผู้ลงทะเบียนในแต่ละ Section |
| teacher_qualifications / teacher_preferences | `(teacher_id, course_id)` | UNIQUE | วิชาที่อาจารย์สอนได้ / อยากสอน |
| teacher_qualifications / teacher_preferences | `course_id` | FK | หาอาจารย์ที่สอนวิชานี้ได้ (ตอน Generate) |
| teacher_availabilities | `(teacher_id, time_slot_id)` | UNIQUE | เช็กเวลาว่างของอาจารย์ |
| teacher_swap_requests | `requesting_teacher_id`, `target_teacher_id`, `requesting_schedule_id`, `target_schedule_id` | FK | คำขอของอาจารย์แต่ละคน |
| public_holidays | `(holiday_date, name)` | UNIQUE | ค้นวันหยุดตามวันที่ / กันข้อมูลซ้ำตอน Sync |

### 5.2 Index ที่สร้างเพิ่ม

| ตาราง | Index | ใช้กับ query | เหตุผล |
| :--- | :--- | :--- | :--- |
| notifications | `(user_id, is_read, created_at)` | แสดงแจ้งเตือนที่ยังไม่อ่านของผู้ใช้ เรียงจากใหม่ไปเก่า | ค้นทุกครั้งที่เปิดหน้า / ใช้แทน index ของ FK `user_id` (นำหน้าด้วย `user_id`) |
| academic_events | `(event_type, start_date, end_date)` | ตรวจ Registration Period (BR-09) ทุกครั้งที่ลงทะเบียน | ลดการอ่านทั้งตาราง |
| schedules | `(status, section_id)` | Generate / Publish / Discard ค้นเฉพาะ DRAFT, Teacher และ Student ค้นเฉพาะ PUBLISHED | แยกคาบตามสถานะ |
| teacher_swap_requests | `(status)` | Admin ดูเฉพาะ ACCEPTED, Teacher B ดูเฉพาะ PENDING | กรองตามสถานะ |

```sql
CREATE INDEX idx_notifications_user_read_created ON notifications (user_id, is_read, created_at);
CREATE INDEX idx_academic_events_type_dates      ON academic_events (event_type, start_date, end_date);
CREATE INDEX idx_schedules_status_section        ON schedules (status, section_id);
CREATE INDEX idx_swap_requests_status            ON teacher_swap_requests (status);
```

> หมายเหตุ: ข้อมูลของโปรเจกต์มีขนาดเล็ก MySQL อาจเลือกอ่านทั้งตารางแทนการใช้ index ตัวที่สร้างเพิ่ม การสร้างไว้เพื่อให้สอดคล้องกับ query จริงเมื่อข้อมูลโตขึ้น ตรวจด้วย `EXPLAIN` ได้

---

## 6. Fetch Type และ Cascade (JPA)

### 6.1 หลักการ

- ทุก relation ตั้ง `fetch = FetchType.LAZY` ไว้ก่อน (ค่า default ของ `@ManyToOne` / `@OneToOne` คือ EAGER ซึ่งก่อให้เกิดปัญหา N+1)
- query ที่ต้องใช้ข้อมูลที่เชื่อมกัน ใช้ `JOIN FETCH` หรือ `@EntityGraph` เพื่อโหลดรวดเดียว
- Service แปลง Entity เป็น DTO ภายใน `@Transactional` ก่อนส่งกลับ Controller (ป้องกัน `LazyInitializationException`)
- ไม่ใช้ `CascadeType.ALL` / `REMOVE` ใน Entity: การลบข้ามตารางใช้ `ON DELETE` ของฐานข้อมูล (ดู §3) และขั้นตอนใน Service ที่ระบุไว้
- ไม่ประกาศฝั่ง `mappedBy` ของ `@OneToOne` (เช่น `User → Student`) เพราะ LAZY ไม่ทำงานบนฝั่งนี้ ให้ค้นผ่าน `StudentRepository.findByUserId()` / `TeacherRepository.findByUserId()` แทน

### 6.2 Fetch Type ของแต่ละ Relation

| Relation | Fetch | โหลดอย่างไรเมื่อต้องใช้ | เหตุผล |
| :--- | :--- | :--- | :--- |
| Student → User, Teacher → User | LAZY | `JOIN FETCH` เฉพาะตอนต้องใช้ email / รหัส | โหลดเฉพาะเมื่อใช้ |
| Section → Course | LAZY | `JOIN FETCH` ใน query รายการ Section | รายการต้องแสดงชื่อวิชา |
| Section → Room | LAZY | `JOIN FETCH` ใน query ตารางสอน | แสดงห้องในตารางสอน |
| Schedule → Section | LAZY | `JOIN FETCH` ใน query Timetable | หลีกเลี่ยง N+1 |
| Schedule → Teacher | LAZY | `JOIN FETCH` ใน query Timetable | หลีกเลี่ยง N+1 |
| Schedule → TimeSlot | LAZY | `JOIN FETCH` ใน query Timetable | หลีกเลี่ยง N+1 |
| Registration → Student | LAZY | ไม่โหลด (ใช้เฉพาะ id) | Service รู้ตัวนักศึกษาอยู่แล้ว |
| Registration → Section | LAZY | `JOIN FETCH` ใน query "ตารางเรียนของฉัน" | ต้องการข้อมูล Section |
| Registration → Course | LAZY | `JOIN FETCH` ใน query "ตารางเรียนของฉัน" | แสดงชื่อวิชา |
| TeacherQualification / TeacherPreference → Teacher, Course | LAZY | `JOIN FETCH` ตอน Generate | Scheduling Engine ใช้ครบ |
| TeacherAvailability → Teacher, TimeSlot | LAZY | `JOIN FETCH` ตอน Generate | Scheduling Engine ใช้ครบ |
| TeacherSwapRequest → Teacher, Schedule | LAZY | `@EntityGraph` ใน query รายการคำขอ | หน้ารายการแสดงครบทุกฝ่าย |
| Notification → User | LAZY | ไม่โหลด (ใช้เฉพาะ `user_id`) | แสดงเฉพาะข้อความ |

### 6.3 การลบข้ามตาราง (ตรงกับ §3)

| กรณี | วิธีจัดการ |
| :--- | :--- |
| ลบ User | DB ลบ Student / Teacher และ Notification ตาม (`CASCADE`) แต่ถ้ามี Registration / คาบสอน / คำขอแลกคาบผูกอยู่ DB จะปฏิเสธ (`RESTRICT`) Service คืน 409 |
| ยกเลิก Section | `SectionCancellationService` ลบ Registration ก่อน แล้วลบ Schedule (DB `CASCADE` ลบตาม Section / ลบคำขอแลกคาบที่อ้างคาบนั้นด้วย) แจ้งเตือนนักศึกษาและอาจารย์ก่อนลบ |
| Discard DRAFT | ลบแถว `schedules` ที่ `status = 'DRAFT'` ด้วย query เดียว |
| ลบวิชา | ถูกปฏิเสธถ้ามี Section หรือ Registration อ้างอิง (`RESTRICT`) |

## Foreign Key และการลบข้อมูล

| Foreign Key | ON DELETE | ผลตามเอกสารฐานข้อมูล |
|---|---|---|
| `students.user_id` → `users` | CASCADE | ลบโปรไฟล์ตามบัญชี |
| `teachers.user_id` → `users` | CASCADE | ลบโปรไฟล์ตามบัญชี |
| `sections.course_id` → `courses` | RESTRICT | ลบวิชาที่ยังมี Section ไม่ได้ |
| `sections.room_id` → `rooms` | RESTRICT | ลบห้องที่ Section ใช้อยู่ไม่ได้ |
| `schedules.section_id` → `sections` | CASCADE | ลบคาบตาม Section |
| `schedules.teacher_id`, `schedules.time_slot_id` | RESTRICT | ลบครูหรือช่วงเวลาที่มีคาบสอนไม่ได้ |
| `registrations.student_id` → `students` | RESTRICT | ลบนักศึกษาที่มีประวัติลงทะเบียนไม่ได้ |
| `registrations.section_id` → `sections` | RESTRICT | ต้องลบ registration ใน Service ก่อน |
| `registrations.course_id` → `courses` | RESTRICT | ลบวิชาที่มีผู้ลงทะเบียนไม่ได้ |
| `teacher_qualifications`, `teacher_preferences` → `teachers`, `courses` | CASCADE | ลบตามเจ้าของ |
| `teacher_availabilities` → `teachers`, `time_slots` | CASCADE | ลบตามเจ้าของ |
| `teacher_swap_requests` (อาจารย์ผู้ขอ/เป้าหมาย) → `teachers` | RESTRICT | รักษาประวัติคำขอ |
| `teacher_swap_requests` (ตารางสอนผู้ขอ/เป้าหมาย) → `schedules` | CASCADE | ลบคำขอเมื่อคาบถูกลบ (แจ้งครูทั้งสองฝ่ายก่อน) |
| `notifications.user_id` → `users` | CASCADE | ลบตามบัญชี |

---

