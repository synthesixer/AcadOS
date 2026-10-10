# AcadOS — เอกสารและการทดสอบระบบ (Automated Testing Suite)

ไดเรกทอรีนี้เป็นส่วนหนึ่งของโครงสร้างโฟลเดอร์มาตรฐานตามเกณฑ์ข้อกำหนดรายวิชา ([`prof_ruleset.md`](../doc/prof_ruleset.md) §9)

## 1. ข้อมูลสรุปชุดทดสอบของระบบ (Test Summary)
- **จำนวนชุดทดสอบทั้งหมด:** 326 Tests (ผ่านเกณฑ์ 100%, 0 Failures, 0 Errors, 0 Skipped)
- **เครื่องมือที่ใช้:** JUnit 5 (Jupiter), Mockito 5, Spring Boot Test, H2 In-Memory Database
- **ตำแหน่ง Source Code ของชุดทดสอบ:** [`code/acados/src/test/java/com/project/acados/`](../code/acados/src/test/java/com/project/acados/)

## 2. การสร้างและจัดเก็บรายงานความครอบคลุม (JaCoCo Code Coverage)
- **เครื่องมือ:** JaCoCo Maven Plugin 0.8.12
- **ตำแหน่งรายงานใน Repository:** [`test/jacoco/index.html`](jacoco/index.html) (จัดเก็บผลการทดสอบ HTML Interactive Report ครบ 94 คลาส)
- **ไฟล์สรุปผลเพิ่มเติม:** [`test/jacoco/jacoco.csv`](jacoco/jacoco.csv) และ [`test/jacoco/jacoco.xml`](jacoco/jacoco.xml)
- **รายงานสรุปการทดสอบฉบับสมบูรณ์:** [`test/TEST_REPORT.md`](TEST_REPORT.md)
- **การทำงานร่วมกับ CI/CD:** ทุกครั้งที่มีการ Push/PR เข้า `main` หรือ `Develop`, GitHub Actions Workflow ([`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml)) จะทำการรันและอัปโหลดรายงาน JaCoCo ขึ้นเป็น Artifact ให้อัตโนมัติ

## 3. ชุดทดสอบระบบภายนอก (Robot Framework Acceptance Tests)
- **ไฟล์สคริปต์:** [`test/robot_testcases.robot`](robot_testcases.robot) (และสำเนาใน [`img/robot_testcase.robot`](../img/robot_testcase.robot))
- **วัตถุประสงค์:** ทำการทดสอบ End-to-End User Journeys ครอบคลุมทั้ง Positive Cases (Admin, Timetable Engine, Student Portal, Teacher Swaps, Swagger UI), Negative Cases (Invalid Password, Unknown User, Unauthorized Access) และบันทึกภาพหน้าจออัตโนมัติ 17 ภาพลงในโฟลเดอร์ [`img/`](../img/)
- **ผลการทดสอบ:** **8 Tests Run, 8 Passed, 0 Failed (100% Green)**
- **คำสั่งรัน:**
```bash
robot -d test/results test/robot_testcases.robot
```

## 4. คำสั่งในการรันชุดทดสอบ Backend (Unit & Integration Tests)
```bash
cd code/acados
mvn clean test
```


