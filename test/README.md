# AcadOS — เอกสารและการทดสอบระบบ (Automated Testing Suite)

ไดเรกทอรีนี้เป็นส่วนหนึ่งของโครงสร้างโฟลเดอร์มาตรฐานตามเกณฑ์ข้อกำหนดรายวิชา ([`prof_ruleset.md`](../doc/prof_ruleset.md) §9)

## 1. ข้อมูลสรุปชุดทดสอบของระบบ (Test Summary)
- **จำนวนชุดทดสอบทั้งหมด:** 326 Tests (ผ่านเกณฑ์ 100%, 0 Failures, 0 Errors, 0 Skipped)
- **เครื่องมือที่ใช้:** JUnit 5 (Jupiter), Mockito 5, Spring Boot Test, H2 In-Memory Database
- **ตำแหน่ง Source Code ของชุดทดสอบ:** [`code/acados/src/test/java/com/project/acados/`](../code/acados/src/test/java/com/project/acados/)

## 2. การสร้างรายงานความครอบคลุม (JaCoCo Code Coverage)
- **เครื่องมือ:** JaCoCo Maven Plugin 0.8.12
- **ไฟล์ผลลัพธ์:** `code/acados/target/site/jacoco/index.html`
- **การทำงานร่วมกับ CI/CD:** ทุกครั้งที่มีการ Push/PR เข้า `main` หรือ `Develop`, GitHub Actions Workflow ([`.github/workflows/ci-cd.yml`](../.github/workflows/ci-cd.yml)) จะทำการรันและอัปโหลดรายงาน JaCoCo ขึ้นเป็น Artifact ให้อัตโนมัติ

## 3. คำสั่งในการรันชุดทดสอบ
```bash
cd code/acados
mvn clean test
```

