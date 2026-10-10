-- =============================================================================
-- AcadOS — Mock & Demo Seed Script (data.sql)
-- Environment: MySQL 8.x / Docker Compose
-- Order: Topological Order based on Foreign Key Dependencies
-- Passwords: All user accounts have password 'password123'
--            BCrypt Hash: $2a$10$8IeHXrOY5n2VPONWnydm3eTrmYSOtzRZN3k/ttVlzWEpgUsOEG8nS
-- Scenarios Supported:
--   1. Student Registration (S001 -> Register Courses during REGISTRATION_PERIOD)
--   2. Automatic Scheduling (Admin -> Generate Schedule for unscheduled active sections)
--   3. Teacher Swap (T001 & T002 -> Published schedules, pending swap request, respond & approve)
--   4. Section Cancellation (Admin -> Cancel Section 4 with existing registrations & schedule)
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. users
-- Roles: ADMIN, TEACHER, STUDENT
-- Login Username: university_id, Password: password123
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `users` (`id`, `university_id`, `email`, `password_hash`, `role`) VALUES
(1, 'admin', 'admin@kku.ac.th', '$2a$10$8IeHXrOY5n2VPONWnydm3eTrmYSOtzRZN3k/ttVlzWEpgUsOEG8nS', 'ADMIN'),
(2, 'T001', 't001@kku.ac.th', '$2a$10$8IeHXrOY5n2VPONWnydm3eTrmYSOtzRZN3k/ttVlzWEpgUsOEG8nS', 'TEACHER'),
(3, 'T002', 't002@kku.ac.th', '$2a$10$8IeHXrOY5n2VPONWnydm3eTrmYSOtzRZN3k/ttVlzWEpgUsOEG8nS', 'TEACHER'),
(4, 'T003', 't003@kku.ac.th', '$2a$10$8IeHXrOY5n2VPONWnydm3eTrmYSOtzRZN3k/ttVlzWEpgUsOEG8nS', 'TEACHER'),
(5, 'S001', 's001@kku.ac.th', '$2a$10$8IeHXrOY5n2VPONWnydm3eTrmYSOtzRZN3k/ttVlzWEpgUsOEG8nS', 'STUDENT'),
(6, 'S002', 's002@kku.ac.th', '$2a$10$8IeHXrOY5n2VPONWnydm3eTrmYSOtzRZN3k/ttVlzWEpgUsOEG8nS', 'STUDENT'),
(7, 'S003', 's003@kku.ac.th', '$2a$10$8IeHXrOY5n2VPONWnydm3eTrmYSOtzRZN3k/ttVlzWEpgUsOEG8nS', 'STUDENT');

-- -----------------------------------------------------------------------------
-- 2. teachers (1:1 with users id 2, 3, 4)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `teachers` (`id`, `user_id`, `full_name`) VALUES
(1, 2, 'ผศ.ดร.พุฒิเมธ ชมศรีสวัสดิ์'),
(2, 3, 'รศ.ดร.วงศกร สงวนกลิ่น'),
(3, 4, 'อ.จิรภัทร สีสาร'),
(4, 31, 'รศ.ดร.สิรภัทร เชี่ยวชาญวัฒนา'),
(5, 21, 'ศ.ดร.ศาสตรา วงศ์ธนวสุ'),
(6, 22, 'ศ.ดร.จักรชัย โสอินทร์'),
(7, 23, 'รศ.ดร.ปัญญาพล หอระตะ'),
(8, 24, 'รศ.ดร.วรารัตน์ สงฆ์แป้น'),
(9, 25, 'ผศ.ดร.พุธษดี ศิริแสงตระกูล'),
(10, 26, 'ผศ.ดร.วชิราวุธ ธรรมวิเศษ'),
(11, 27, 'ผศ.ดร.มัลลิกา วัฒนะ'),
(12, 28, 'ผศ.ดร.วรัญญา วรรณศรี'),
(13, 29, 'อ.ดร.ภัคราช มุสิกะวัน'),
(14, 30, 'อ.ดร.พงษ์ศธร จันทร์ยอย');

-- -----------------------------------------------------------------------------
-- 3. students (1:1 with users id 5, 6, 7)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `students` (`id`, `user_id`, `full_name`) VALUES
(1, 5, 'นายเอ นามสกุลบี'),
(2, 6, 'นางสาวซี นามสกุลดี'),
(3, 7, 'นายเอบีเอ นามสกุลวีวี')
(14, 15, 'นายตูน บอดี้สแลม'),
(4, 8, 'นายธนกร ศรีสุวรรณ'),
(5, 9, 'นางสาวพิมพ์ชนก แก้วมณี'),
(6, 10, 'นายกิตติพัฒน์ บุญประเสริฐ'),
(7, 11, 'นางสาวณัฐธิดา วงศ์ใหญ่'),
(8, 12, 'นายภูริภัทร จันทร์หอม'),
(9, 13, 'นางสาวกมลชนก ทองดี'),
(10, 14, 'นายวรเมธ สายทอง'),
(11, 20, 'นายหม่ำ จ๊กมก'),
(12, 18, 'นายเท่ง เถิดเทิง'),
(13, 19, 'นายโหน่ง ชะชะช่า');

-- -----------------------------------------------------------------------------
-- 4. courses
-- -----------------------------------------------------------------------------
-- -----------------------------------------------------------------------------
-- 4. courses
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `courses` (`id`, `course_code`, `title`, `weekly_hours`) VALUES
-- รายวิชาเดิม
(1, 'CP353201', '	Software Quality Assurance', 3),
(2, 'CP353003', 'Artificial Intelligence', 4),
(3, 'SC403602', 'NUMERICAL METHODS FOR COMPUTER SCIENCE', 3),
(4, 'CP353002', 'Principles of Software Design and Development', 3),
(5, 'CP353001', 'Operating Systems and System Calls Programming', 1),
(6, 'GE362785', '	CREATIVE THINKING AND PROBLEM SOLVING', 2),
-- รายวิชาวิทยาการคอมพิวเตอร์ / วิทยาศาสตร์
(7, 'CP322494', 'Computer Project I', 3),
(8, 'SC331021', 'Basics of Information Technology', 3),
(9, 'CP322113', 'Foundations of Computer Science', 3),
(10, 'CP322114', 'Structured Programming for Computer Science', 3),
(11, 'CP322219', 'Object-Oriented Programming', 3),
(12, 'SC311004', 'Computer Systems Architecture', 3),
(13, 'CP342375', 'XML Technologies and Applications', 3),
(14, 'CP320101', 'Microcomputer and Application', 3),
(15, 'CP324201', 'ภูมิสารสนเทศขั้นมูลฐาน', 3),
(16, 'CP322235', 'Software Testing', 3),
(17, 'SC333301', 'การเขียนโปรแกรมในระบบสารสนเทศภูมิศาสตร์', 3),
(18, 'CP322254', 'Script Programming', 3),
(19, 'CP322234', 'Introduction to Remote Sensing', 3),
(20, 'CP322391', 'Research Methodology', 3),
(21, 'CP322161', 'Introduction to Information and Communication', 3),
(22, 'SC313103', 'การวิเคราะห์วิทยาข้อมูลและการทำเหมืองข้อมูล', 3),
(23, 'SC002001', 'Orientation to Co-operative Education for Science Students', 1),
(24, 'CP322239', 'Database Application', 3),
(25, 'SC310002', 'โครงสร้างข้อมูล', 3),
(26, 'CP342365', 'การประมวลผลแบบกลุ่มเมฆและการประยุกต์', 3),
(27, 'SC332213', 'Introduction to Geographic Information System', 3),
(28, 'SC332211', 'Aerial Photograph and Interpretation', 3),
(29, 'SC312101', 'การเรียนรู้เชิงเครื่องจักรสำหรับวิทยาการข้อมูล', 3),
(30, 'SC328852', 'โครงข่ายประสาทเทียม', 3),
(31, 'CP353109', 'การประมวลผลภาษาธรรมชาติ', 3),
(32, 'SC352101', 'Introduction to Data Science', 3),
-- คณะเกษตรศาสตร์
(33, 'AG103008', 'เรื่องมหัศจรรย์ของแมลงและแมงมุม', 3),
(34, 'AG172101', 'การผลิตไก่พื้นเมืองไทยเชิงพาณิชย์', 3),
(35, 'AG172112', 'การจัดการของเสียจากสัตว์', 3),
(36, 'AG172131', 'พฤติกรรมและสวัสดิภาพของสัตว์เลี้ยง', 3),
-- คณะแพทยศาสตร์
(37, 'MD660401', 'ยาในชีวิตประจำวัน', 3),
(38, 'MD660402', 'ผลิตภัณฑ์เสริมอาหาร ยา และเครื่องสำอาง เพื่อสุขภาพ', 3),
(39, 'MD371120', 'นิติวิทยาศาสตร์เบื้องต้น', 3),
(40, 'MD750230', 'รู้ทันโรคศัลยกรรมใกล้ตัว', 3),
(41, 'MD750231', 'การจัดการชีวิต', 3),
-- คณะเภสัชศาสตร์
(42, 'PS614201', 'สารพิษในชีวิตประจำวัน', 3),
-- คณะสาธารณสุขศาสตร์
(43, 'PH511101', 'แนะนำการสาธารณสุข', 3),
(44, 'PH515330', 'ความรอบรู้ด้านสุขภาพเพื่อส่งเสริมสุขภาพทางเพศ', 3),
(45, 'PH511414', 'การบริหารโครงการสาธารณสุข', 3),
(46, 'PH511419', 'นวัตกรรมสาธารณสุขและประชาสังคม', 3),
(47, 'PH515121', 'นิรภัยศึกษา', 3),
(48, 'PH514313', 'พิษวิทยาทางอาหารเบื้องต้น', 3),
-- สถาบันการสอนรายวิชาศึกษาทั่วไป
(49, 'GE001091', 'ปัญญาประดิษฐ์เพื่อการเรียนรู้และการทำงาน', 3),
(50, 'GE821245', 'สมาธิเพื่อพัฒนาชีวิต', 3);

-- -----------------------------------------------------------------------------
-- 5. rooms
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `rooms` (`id`, `room_number`, `building`, `floor`, `capacity`, `is_available`) VALUES
-- ห้องเดิม
(1, 'SC9127', 'อาคาร SC01', 1, 120, true),
(2, 'SC9201', 'อาคาร SC01', 1, 50, true),
(3, 'SC9202', 'อาคาร SC02', 2, 40, true),
(4, 'SC9203', 'อาคาร SC02', 2, 40, true),
(5, 'SC7301', 'อาคาร SC03', 5, 25, true),
(6, 'GL01', 'อาคารวิทยบริการ', 1, 120, true),
(7, 'SC9226', 'อาคาร SC09', 2, 40, true),
(8, 'SC9227', 'อาคาร SC09', 2, 40, true),
(9, 'SC9228', 'อาคาร SC09', 2, 40, true),
(10, 'SC9421', 'อาคาร SC09', 4, 30, true),
(11, 'SC9422', 'อาคาร SC09', 4, 30, true),
(12, 'SC9524', 'อาคาร SC09', 5, 30, true),
(13, 'SC9525', 'อาคาร SC09', 5, 60, true),
(14, 'SC6201', 'อาคาร SC06', 2, 80, true),
(15, 'SC6601A', 'อาคาร SC06', 6, 35, true),
(16, 'SC6601B', 'อาคาร SC06', 6, 35, true);

-- -----------------------------------------------------------------------------
-- 6. time_slots
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `time_slots` (`id`, `day_of_week`, `start_time`, `end_time`) VALUES
-- 3-hour lecture/lab slots
(1, 'MONDAY', '09:00:00', '12:00:00'),
(2, 'MONDAY', '13:00:00', '16:00:00'),
(3, 'TUESDAY', '09:00:00', '12:00:00'),
(4, 'TUESDAY', '13:00:00', '16:00:00'),
(5, 'WEDNESDAY', '09:00:00', '12:00:00'),
(6, 'WEDNESDAY', '13:00:00', '16:00:00'),
(7, 'THURSDAY', '09:00:00', '12:00:00'),
(8, 'THURSDAY', '13:00:00', '16:00:00'),
(9, 'FRIDAY', '09:00:00', '12:00:00'),
(10, 'FRIDAY', '13:00:00', '16:00:00'),
-- 1-hour slots
(11, 'MONDAY', '09:00:00', '10:00:00'),
(12, 'MONDAY', '10:00:00', '11:00:00'),
(13, 'MONDAY', '11:00:00', '12:00:00'),
(14, 'TUESDAY', '09:00:00', '10:00:00'),
(15, 'TUESDAY', '10:00:00', '11:00:00'),
(16, 'TUESDAY', '11:00:00', '12:00:00'),
(17, 'WEDNESDAY', '09:00:00', '10:00:00'),
(18, 'WEDNESDAY', '10:00:00', '11:00:00'),
(19, 'WEDNESDAY', '11:00:00', '12:00:00'),
(20, 'THURSDAY', '09:00:00', '10:00:00'),
(21, 'THURSDAY', '10:00:00', '11:00:00'),
(22, 'THURSDAY', '11:00:00', '12:00:00'),
(23, 'FRIDAY', '09:00:00', '10:00:00'),
(24, 'FRIDAY', '10:00:00', '11:00:00'),
(25, 'FRIDAY', '11:00:00', '12:00:00'),
-- 2-hour slots (09:00-11:00, 13:00-15:00)
(26, 'MONDAY', '09:00:00', '11:00:00'),
(27, 'MONDAY', '13:00:00', '15:00:00'),
(28, 'WEDNESDAY', '09:00:00', '11:00:00'),
(29, 'WEDNESDAY', '13:00:00', '15:00:00'),
(30, 'FRIDAY', '09:00:00', '11:00:00'),
(31, 'FRIDAY', '13:00:00', '15:00:00'),
-- 1.5-hour academic period slots (09:00-10:30, 10:30-12:00, 13:00-14:30, 14:30-16:00, 16:00-17:00)
(32, 'MONDAY', '09:00:00', '10:30:00'),
(33, 'MONDAY', '10:30:00', '12:00:00'),
(34, 'MONDAY', '13:00:00', '14:30:00'),
(35, 'MONDAY', '14:30:00', '16:00:00'),
(36, 'MONDAY', '16:00:00', '17:00:00'),
(37, 'TUESDAY', '09:00:00', '10:30:00'),
(38, 'TUESDAY', '10:30:00', '12:00:00'),
(39, 'TUESDAY', '13:00:00', '14:30:00'),
(40, 'TUESDAY', '14:30:00', '16:00:00'),
(41, 'TUESDAY', '16:00:00', '17:00:00'),
(42, 'WEDNESDAY', '09:00:00', '10:30:00'),
(43, 'WEDNESDAY', '10:30:00', '12:00:00'),
(44, 'WEDNESDAY', '13:00:00', '14:30:00'),
(45, 'WEDNESDAY', '14:30:00', '16:00:00'),
(46, 'WEDNESDAY', '16:00:00', '17:00:00'),
(47, 'THURSDAY', '09:00:00', '10:30:00'),
(48, 'THURSDAY', '10:30:00', '12:00:00'),
(49, 'THURSDAY', '13:00:00', '14:30:00'),
(50, 'THURSDAY', '14:30:00', '16:00:00'),
(51, 'THURSDAY', '16:00:00', '17:00:00'),
(52, 'FRIDAY', '09:00:00', '10:30:00'),
(53, 'FRIDAY', '10:30:00', '12:00:00'),
(54, 'FRIDAY', '13:00:00', '14:30:00'),
(55, 'FRIDAY', '14:30:00', '16:00:00'),
(56, 'FRIDAY', '16:00:00', '17:00:00'),
-- 1-hour afternoon slots (13:00-14:00, 14:00-15:00, 15:00-16:00)
(57, 'MONDAY', '13:00:00', '14:00:00'),
(58, 'MONDAY', '14:00:00', '15:00:00'),
(59, 'MONDAY', '15:00:00', '16:00:00'),
(60, 'TUESDAY', '13:00:00', '14:00:00'),
(61, 'TUESDAY', '14:00:00', '15:00:00'),
(62, 'TUESDAY', '15:00:00', '16:00:00'),
(63, 'WEDNESDAY', '13:00:00', '14:00:00'),
(64, 'WEDNESDAY', '14:00:00', '15:00:00'),
(65, 'WEDNESDAY', '15:00:00', '16:00:00'),
(66, 'THURSDAY', '13:00:00', '14:00:00'),
(67, 'THURSDAY', '14:00:00', '15:00:00'),
(68, 'THURSDAY', '15:00:00', '16:00:00'),
(69, 'FRIDAY', '13:00:00', '14:00:00'),
(70, 'FRIDAY', '14:00:00', '15:00:00'),
(71, 'FRIDAY', '15:00:00', '16:00:00');

-- -----------------------------------------------------------------------------
-- 7. teacher_qualifications (BR-06)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `teacher_qualifications` (`id`, `teacher_id`, `course_id`) VALUES
(1, 1, 1), -- T001 qualifies for CP353001
(2, 1, 2), -- T001 qualifies for CP353002
(3, 1, 3), -- T001 qualifies for CP353003
(4, 1, 5), -- T001 qualifies for CP351001
(5, 2, 1), -- T002 qualifies for CP353001
(6, 2, 2), -- T002 qualifies for CP353002
(7, 2, 4), -- T002 qualifies for CP353004
(8, 2, 5), -- T002 qualifies for CP351001
(9, 3, 2), -- T003 qualifies for CP353002
(10, 3, 3), -- T003 qualifies for CP353003
(11, 3, 4), -- T003 qualifies for CP353004
(12, 3, 5), -- T003 qualifies for CP351001
(13, 2, 6); -- T002 qualifies for EN012001

-- -----------------------------------------------------------------------------
-- 8. teacher_preferences (Priority: 1=highest +30, 2=+20, 3=+10)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `teacher_preferences` (`id`, `teacher_id`, `course_id`, `priority`) VALUES
(1, 1, 1, 1),
(2, 1, 2, 2),
(3, 1, 5, 1),
(4, 2, 2, 1),
(5, 2, 4, 2),
(6, 2, 5, 2),
(7, 3, 3, 1),
(8, 3, 4, 1),
(9, 3, 5, 3),
(10, 2, 6, 1),
(11, 4, 7, 1),   -- สิรภัทร → CP322494 Computer Project I
(12, 5, 9, 1),   -- ศาสตรา → CP322113 Foundations of CS
(13, 6, 10, 1),  -- จักรชัย → CP322114 Structured Programming
(14, 7, 11, 1),  -- ปัญญาพล → CP322219 OOP
(15, 8, 16, 1),  -- วรารัตน์ → CP322235 Software Testing
(16, 9, 9, 2),   -- พุธษดี → CP322113 Foundations of CS
(17, 10, 9, 3),  -- วชิราวุธ → CP322113 Foundations of CS
(18, 11, 24, 1), -- มัลลิกา → CP322239 Database Application
(19, 12, 25, 1), -- วรัญญา → SC310002 โครงสร้างข้อมูล
(20, 13, 29, 1), -- ภัคราช → SC312101 Machine Learning
(21, 14, 31, 1); -- พงษ์ศธร → CP353109 NLP
-- -----------------------------------------------------------------------------
-- 9. teacher_availabilities (BR-07)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `teacher_availabilities` (`id`, `teacher_id`, `time_slot_id`, `is_available`) VALUES
(1, 1, 1, true),
(2, 1, 2, true),
(3, 1, 3, true),
(4, 1, 4, true),
(5, 1, 5, true),
(6, 1, 6, true),
(7, 1, 7, true),
(8, 1, 8, true),
(9, 1, 9, false), -- T001 unavailable Friday morning
(10, 2, 1, true),
(11, 2, 2, true),
(12, 2, 3, true),
(13, 2, 4, true),
(14, 2, 5, true),
(15, 2, 6, true),
(16, 2, 7, true),
(17, 2, 8, true),
(18, 3, 5, true),
(19, 3, 6, true),
(20, 3, 7, true),
(21, 3, 8, true),
(22, 2, 26, true),
(23, 2, 28, true);

-- -----------------------------------------------------------------------------
-- 10. sections
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `sections` (`id`, `course_id`, `section_number`, `capacity`, `status`) VALUES
(1, 1, 1, 40, 'ACTIVE'), -- CP353001 Sec 1 (Published schedule with T001)
(2, 1, 2, 35, 'ACTIVE'), -- CP353001 Sec 2 (Available for Student Registration demo)
(3, 2, 1, 40, 'ACTIVE'), -- CP353002 Sec 1 (Published schedule with T002)
(4, 2, 2, 30, 'ACTIVE'), -- CP353002 Sec 2 (Has schedule and S002 registered; for Section Cancellation demo)
(5, 3, 1, 40, 'ACTIVE'), -- CP353003 Sec 1 (Published schedule with T003; enrolled by S001)
(6, 5, 1, 30, 'ACTIVE'), -- CP351001 Sec 1 (Unscheduled; for Scenario 2 Auto Scheduling demo)
(7, 4, 1, 25, 'ACTIVE'), -- CP353004 Sec 1 (Unscheduled; for Scenario 2 Auto Scheduling demo)
(8, 6, 1, 40, 'ACTIVE'); -- EN012001 Sec 1 (2 sessions/week: Mon 09:00-11:00 and Wed 09:00-11:00 = 4 hrs/week)

-- -----------------------------------------------------------------------------
-- 11. schedules
-- Status: PUBLISHED (Used for live registration and swap)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `schedules` (`id`, `section_id`, `room_id`, `teacher_id`, `time_slot_id`, `status`) VALUES
-- Schedule 1: CP353001 Sec 1, Room SC0101, Teacher T001, Mon 09:00-12:00
(1, 1, 1, 1, 1, 'PUBLISHED'),
-- Schedule 2: CP353002 Sec 1, Room SC0102, Teacher T002, Tue 09:00-12:00
(2, 3, 2, 2, 3, 'PUBLISHED'),
-- Schedule 3: CP353002 Sec 2, Room SC0201, Teacher T001, Wed 09:00-12:00 (For cancellation demo)
(3, 4, 3, 1, 5, 'PUBLISHED'),
-- Schedule 4: CP353003 Sec 1, Room LAB-501, Teacher T003, Thu 09:00-12:00 (Student S001 enrolled)
(4, 5, 5, 3, 7, 'PUBLISHED'),
-- Schedule 5 & 6: EN012001 Sec 1 (Section 8), 2 sessions/week (Mon 09:00-11:00 & Wed 09:00-11:00), 2 hrs each = 4 hrs/week
(5, 8, 2, 2, 26, 'PUBLISHED'),
(6, 8, 2, 2, 28, 'PUBLISHED');

-- -----------------------------------------------------------------------------
-- 12. registrations
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `registrations` (`id`, `student_id`, `section_id`, `course_id`, `registered_at`) VALUES
-- S001 registered in CP353003 Sec 1 (Thu 09:00-12:00)
(1, 1, 5, 3, '2026-10-10 08:00:00'),
-- S002 registered in CP353002 Sec 2 (Wed 09:00-12:00) -> Ready for Section Cancellation demo
(2, 2, 4, 2, '2026-10-10 08:30:00'),
-- S003 registered in CP353002 Sec 1 (Tue 09:00-12:00)
(3, 3, 3, 2, '2026-10-10 08:35:00');

-- -----------------------------------------------------------------------------
-- 13. teacher_swap_requests
-- Pre-seeded PENDING swap request between T001 (Schedule 1) and T002 (Schedule 2)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `teacher_swap_requests` (`id`, `requesting_teacher_id`, `requesting_schedule_id`, `target_teacher_id`, `target_schedule_id`, `status`, `created_at`, `responded_at`, `reviewed_at`) VALUES
(1, 1, 1, 2, 2, 'PENDING', '2026-10-10 08:15:00', NULL, NULL);

-- -----------------------------------------------------------------------------
-- 14. notifications
-- In-app notifications
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `notifications` (`id`, `user_id`, `title`, `message`, `type`, `is_read`, `created_at`) VALUES
(1, 1, 'ยินดีต้อนรับสู่ระบบ AcadOS', 'ระบบพร้อมสำหรับการบริหารจัดการตารางสอนและบริการวิชาการ', 'SCHEDULE_CHANGED', false, '2026-10-10 08:00:00'),
(2, 2, 'ประกาศตารางสอนภาคการศึกษาใหม่', 'ตารางสอนวิชา CP353001 ได้รับการเผยแพร่เรียบร้อยแล้ว', 'SCHEDULE_CHANGED', true, '2026-10-10 08:00:00'),
(3, 3, 'มีคำขอสลับคาบสอนใหม่', 'ผศ.ดร.พุฒิเมธ สิทธิชัย ส่งคำขอสลับคาบสอนวิชา CP353001 กับ CP353002 ของท่าน', 'SWAP_REQUESTED', false, '2026-10-10 08:15:00'),
(4, 5, 'เปิดระบบลงทะเบียนเรียน', 'ระบบเปิดให้ลงทะเบียนเรียนภาคการศึกษา 1/2569 แล้ว กรุณาเลือกรายวิชาที่ต้องการ', 'REGISTRATION_SUCCESS', false, '2026-10-10 08:00:00'),
(5, 5, 'ลงทะเบียนสำเร็จ', 'คุณลงทะเบียน CP353003 Section 1 เรียบร้อยแล้ว', 'REGISTRATION_SUCCESS', true, '2026-10-10 08:00:00');

-- -----------------------------------------------------------------------------
-- 15. academic_events (BR-09)
-- REGISTRATION_PERIOD covers 2024-2030 to ensure active registration period always
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `academic_events` (`id`, `event_name`, `event_type`, `start_date`, `end_date`) VALUES
(1, 'ภาคการศึกษาที่ 1 ประจำปีการศึกษา 2569 (Semester 1)', 'SEMESTER_START', '2026-06-01', '2026-06-01'),
(2, 'ช่วงเวลาลงทะเบียนเรียนปกติ (Registration Period)', 'REGISTRATION_PERIOD', '2024-01-01', '2030-12-31'),
(3, 'การสอบกลางภาค (Midterm Examination)', 'MIDTERM_EXAM', '2026-08-15', '2026-08-22'),
(4, 'การสอบปลายภาค (Final Examination)', 'FINAL_EXAM', '2026-10-20', '2026-10-31'),
(5, 'สิ้นสุดภาคการศึกษาที่ 1 (Semester End)', 'SEMESTER_END', '2026-11-05', '2026-11-05');

-- -----------------------------------------------------------------------------
-- 16. public_holidays (FL-04)
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO `public_holidays` (`id`, `holiday_date`, `name`, `description`) VALUES
(1, '2026-01-01', 'วันขึ้นปีใหม่ (New Year\'s Day)', 'วันหยุดราชการสากลประจำปี'),
(2, '2026-04-13', 'วันสงกรานต์ (Songkran Festival Day 1)', 'วันผู้สูงอายุแห่งชาติ'),
(3, '2026-04-14', 'วันสงกรานต์ (Songkran Festival Day 2)', 'วันครอบครัวแห่งชาติ'),
(4, '2026-04-15', 'วันสงกรานต์ (Songkran Festival Day 3)', 'วันเถลิงศก'),
(5, '2026-12-05', 'วันชาติและวันพ่อแห่งชาติ (National Day)', 'วันคล้ายวันพระบรมราชสมภพในหลวงรัชกาลที่ 9'),
(6, '2026-12-31', 'วันสิ้นปี (New Year\'s Eve)', 'วันหยุดส่งท้ายปีเก่าต้อนรับปีใหม่');

SET FOREIGN_KEY_CHECKS = 1;
