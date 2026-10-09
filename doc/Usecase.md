Database

## ฟิลด์ของแต่ละ Entity

เอกสารกำหนดให้ทุก Entity ใช้ `id: Long` เป็น Primary Key และเก็บ `universityId` แยกจาก `id` โดย `universityId` ปรากฏเป็นฟิลด์ของ `User` ใน Class Diagram

| Entity | ข้อมูลที่ระบุให้เก็บ |
|---|---|
| **User** | `id: Long` (PK), `universityId: String`, `passwordHash: String` (BCrypt), `role: String` โดย role ที่ระบุคือ `ADMIN`, `TEACHER`, `STUDENT` |
| **Student** | `id: Long` (PK), `user: User`, `fullName: String` |
| **Teacher** | `id: Long` (PK), `user: User`, `fullName: String` |
| **Course** | `id: Long` (PK), `courseCode: String`, `title: String`, `weeklyHours: int` |
| **Section** | `id: Long` (PK), `course: Course`, `sectionNumber: int`, `capacity: int`, `room: Room`, `teacher: Teacher`, `state: SectionState` |
| **Room** | `id: Long` (PK), `roomNumber: String`, `building: String`, `floor: int`, `capacity: int`, `isAvailable: boolean` |
| **TimeSlot** | `id: Long` (PK), `dayOfWeek: String`, `startTime: LocalTime`, `endTime: LocalTime` |
| **Schedule** | `id: Long` (PK), `section: Section`, `timeSlot: TimeSlot` ตาม Class Diagram |
| **Registration** | `id: Long` (PK), `student: Student`, `section: Section`, `registeredAt: LocalDateTime` |
| **TeacherQualification** | `id: Long` (PK), `teacher: Teacher`, `course: Course` |
| **TeacherPreference** | `id: Long` (PK), `teacher: Teacher`, `course: Course`, `priority: int` |
| **TeacherAvailability** | `id: Long` (PK), `teacher: Teacher`, `timeSlot: TimeSlot`, `isAvailable: boolean` ตาม Class Diagram |
| **TeacherSwapRequest** | `id: Long` (PK), `requestingTeacher: Teacher`, `targetTeacher: Teacher`, `requestingSchedule: Schedule`, `targetSchedule: Schedule`, `status: String` |
| **Notification** | `id: Long` (PK), `userId: Long`, `title: String`, `message: String`, `type: String`, `isRead: boolean`, `createdAt: LocalDateTime` |
| **AcademicEvent** | `id: Long` (PK), `eventName: String`, `eventType: String`, `startDate: LocalDate`, `endDate: LocalDate` |
| **PublicHoliday** | `id: Long` (PK), `date: LocalDate`, `name: String`, `description: String` |

## ความสัมพันธ์และข้อกำหนดฐานข้อมูลที่เอกสารระบุ

- `User` เชื่อมกับ `Student` หรือ `Teacher` แบบ **One-to-One**; ไม่สร้าง `StudentProfile` หรือ `TeacherProfile` แยก
- `Course` หนึ่งรายการมีหลาย `Section`
- แต่ละ `Section` ใช้ห้องประจำหนึ่งห้อง และ `Section.capacity` ต้องไม่เกิน `Room.capacity`
- `Section` หนึ่งรายการมีหลาย `Schedule`
- `TeacherQualification` และ `TeacherPreference` เชื่อม `Teacher` กับ `Course`
- `TeacherAvailability` เก็บช่วงเวลาที่อาจารย์ไม่สะดวก และใช้เป็น Hard Constraint
- ต้องป้องกันการลงทะเบียน `Student` ใน `Section` ซ้ำ ทั้งใน Service และด้วย Unique Constraint ในฐานข้อมูล
- `AcademicEvent` กับ `PublicHoliday` ไม่มีความสัมพันธ์กันในฐานข้อมูล
- `TeacherSwapRequest.status` ระบุสถานะ `PENDING`, `ACCEPTED`, `REJECTED`, `APPROVED`

## จุดที่เอกสารยังไม่ชัด จึงไม่ควรกำหนดเอง

1. **Schedule มีข้อมูลขัดกันใน PDF:** คำอธิบาย Entity บอกว่าผูกกับ `Section`, `Teacher`, `Room`, `TimeSlot` แต่ Class Diagram แสดงเฉพาะ `section` และ `timeSlot`; ขณะเดียวกัน `Section` มี `teacher` และ `room` อยู่แล้ว จึงต้องยืนยันก่อนกำหนด FK จริง
2. **TeacherAvailability:** คำอธิบายบอกว่าเก็บเวลาที่ไม่ว่าง แต่ Class Diagram ใช้ `isAvailable: boolean` ความหมายของค่า `true/false` ยังไม่ระบุชัด
3. PDF ไม่ได้ระบุชื่อคอลัมน์ FK ในตาราง, SQL column types, index แต่ละตัว, หรือรายละเอียด Cascade/Fetch ของแต่ละความสัมพันธ์ จึงยังสรุปค่าเหล่านี้ไม่ได้จากเอกสารนี้เพียงอย่างเดียว

อ้างอิงข้อมูลทั้งหมดจาก :codex-file-citation{path="C:\Users\ADMIN\Downloads\AcadOS (3).pdf" purpose="source"}