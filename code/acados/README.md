# AcadOS Application Core — Spring Boot 3.3.4 Backend & Web Application

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/)
[![Tests](https://img.shields.io/badge/JUnit%205-326%20Passed%20(100%25)-success.svg)](src/test/java/)
[![JaCoCo](https://img.shields.io/badge/Coverage-JaCoCo%20Report-blue.svg)](target/site/jacoco/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](Dockerfile)

**AcadOS Application Core** คือโมดูลหลักของระบบบริหารจัดการงานวิชาการและจัดสรรตารางคุมสอบ/ตารางสอนอัตโนมัติ พัฒนาตามแนวคิด **Clean 3-Tier Layered Architecture** และ **SOLID Principles 100%** โดยรวมทั้ง RESTful APIs, Spring Data JPA Repositories, Business Service Workflows, Design Patterns และ Thymeleaf Server-side Views ไว้ในโมดูลเดียว

---

## 1. โครงสร้างแพ็กเกจของระบบ (System Package Layout)

```text
code/acados/src/main/java/com/project/acados/
├── config/              # Spring Configuration (Security, OpenAPI/Swagger, WebMvc)
├── controller/
│   ├── api/             # REST API Controllers (11 ชุด: Course, Room, Section, Swap ฯลฯ)
│   └── web/             # Thymeleaf Web Controllers (Home, Dashboard, Timetable)
├── domain/entity/       # JPA Relational Entities (16 ตัว: User, Course, Schedule ฯลฯ)
├── dto/
│   ├── request/         # Input Request DTOs พร้อม Bean Validation (@NotNull, @Valid)
│   └── response/        # Standard Output Response DTOs
├── exception/           # Custom Domain Exceptions และ GlobalExceptionHandler
├── mapper/              # MapStruct Entity <-> DTO Mappers
├── notification/        # Notification Strategy Pattern (InApp, Email/Mailtrap)
├── pattern/
│   ├── holiday/         # Adapter Pattern สำหรับเชื่อมต่อ ThailandFormats API
│   ├── observer/        # Observer Pattern สำหรับแจ้งเตือนเมื่อตารางมีการเปลี่ยนแปลง
│   ├── scoring/         # Strategy Pattern สำหรับคำนวณคะแนนตาราง (Preference, Workload, Room)
│   └── state/           # State Pattern สำหรับบริหารสถานะกลุ่มเรียน (Active, Cancelled)
├── repository/          # Spring Data JPA Repository Interfaces (16 Repositories)
├── security/            # JWT Token Provider, Filters, และ CustomUserDetailsService
└── service/             # Service Interfaces และ Business Implementation Classes
```

---

## 2. ข้อกำหนดและการรันระบบในเครื่อง (Local Development)

### 2.1 สิ่งที่ต้องเตรียม (Prerequisites)
- **Java 21 LTS** (Eclipse Temurin หรือ OpenJDK)
- **Apache Maven 3.9+**
- **Docker & Docker Compose** (สำหรับ MySQL Database 8.x)

### 2.2 คำสั่งคอมไพล์และทดสอบ (Build & Test)
```bash
# คอมไพล์ซอร์สโค้ด
mvn clean compile

# รันชุดทดสอบอัตโนมัติทั้งหมด (326 Tests) พร้อมสร้าง JaCoCo Code Coverage
mvn clean test

# แพ็กเกจเป็น Executable Fat JAR
mvn -B -DskipTests package
```
*ไฟล์ผลลัพธ์ JAR จะอยู่ที่:* `target/acados-1.0-SNAPSHOT.jar`

### 2.3 การรันระบบด้วย Docker Compose
```bash
# สตาร์ท MySQL 8.4 Database, phpMyAdmin และ Spring Boot App
docker compose up -d --build

# ดูสถานะและ Log ของแอปพลิเคชัน
docker compose ps
docker compose logs -f app

# สั่งหยุดการทำงาน
docker compose down
```

---

## 3. ตัวแปรสภาพแวดล้อม (Environment Variables Configuration)

ระบบได้รับการออกแบบตามหลัก **12-Factor App** สามารถ Override ค่าคอนฟิกได้ผ่าน Environment Variables โดยมีค่าเริ่มต้นสำหรับ Local Fallback ดังนี้:

| Environment Variable | ค่าเริ่มต้น (Default Fallback) | รายละเอียดการใช้งาน |
| :--- | :--- | :--- |
| `PORT` | `8080` | พอร์ตของ HTTP Web Server (รองรับ Dynamic Port ของ Railway/Render) |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/acados_db?...` | JDBC Connection String สำหรับเชื่อมต่อไปยัง MySQL |
| `SPRING_DATASOURCE_USERNAME` | `root` | ชื่อผู้ใช้งานฐานข้อมูล |
| `SPRING_DATASOURCE_PASSWORD` | `root` | รหัสผ่านฐานข้อมูล |
| `SPRING_SQL_INIT_MODE` | `always` | สั่งให้รัน `data.sql` โหลด Seed Data 16 ตาราง (`always` / `never`) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | โหมดจัดการโครงสร้างตารางของ Hibernate |
| `ACADOS_JWT_SECRET` | *(256-bit Hex Default)* | รหัสลับสำหรับเข้ารหัสและตรวจสอบความถูกต้องของ JWT Token |
| `SPRING_MAIL_HOST` | `sandbox.smtp.mailtrap.io` | โฮสต์ของ Mail Server (พอร์ต 587) |
| `SPRING_MAIL_USERNAME` | `mailtrap_user` | ชื่อบัญชี Mailtrap SMTP Sandbox |
| `SPRING_MAIL_PASSWORD` | `mailtrap_pass` | รหัสผ่าน Mailtrap SMTP Sandbox |

---

## 4. พิกัด Service Endpoints สำคัญ

เมื่อแอปพลิเคชันเริ่มทำงานสำเร็จ สามารถเข้าถึง Endpoints ต่าง ๆ ได้ดังนี้:
- **Web Application Portal:** `http://localhost:8080/`
- **Swagger UI (Interactive API Docs):** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Schema (JSON):** `http://localhost:8080/api-docs`
- **Spring Actuator Healthcheck:** `http://localhost:8080/actuator/health`
- **Spring Actuator Metrics:** `http://localhost:8080/actuator/metrics`

---

## 5. บัญชีผู้ใช้งานสำหรับการทดสอบ (Default Seed Accounts)

ข้อมูลบัญชีผู้ใช้งานตัวอย่างใน `data.sql` (ทุกบัญชีใช้รหัสผ่าน `password123`):
- **Admin:** `admin` / `password123`
- **Teachers:** `T001`, `T002`, `T003` / `password123`
- **Students:** `S001`, `S002`, `S003` / `password123`

---

## 6. การทดสอบและการรับประกันคุณภาพ (Quality Assurance)

### 6.1 ชุดทดสอบ Backend (Unit & Integration Tests)
- **จำนวนชุดทดสอบ:** 326 Tests ผ่าน 100% (0 Failures, 0 Errors, 0 Skipped)
- **สถาปัตยกรรมชุดทดสอบ:**
  - Service Unit Tests แบบแยกส่วนด้วย Mockito
  - Web Slice Tests ด้วย `@WebMvcTest`
  - Data Slice Tests ด้วย `@DataJpaTest` (H2 In-Memory)
  - End-to-End Integration Tests ด้วย `@SpringBootTest`

### 6.2 การรันและเปิดดูรายงาน JaCoCo Code Coverage ในเครื่อง
```bash
# รันชุดทดสอบทั้งหมดและสร้างรายงานความครอบคลุม
mvn clean test

# เปิดดูรายงานผ่านเบราว์เซอร์ (Windows PowerShell)
Start-Process target/site/jacoco/index.html
```
*รายงานฉบับถาวรถูกบันทึกไว้ใน Repository ที่ [**`test/jacoco/index.html`**](../../test/jacoco/index.html) ครอบคลุมทั้ง 94 คลาส*

### 6.3 การทดสอบ End-to-End Acceptance Tests ด้วย Robot Framework
```bash
# ติดตั้งเครื่องมือ
pip install robotframework robotframework-seleniumlibrary

# รันชุดทดสอบ 8 Scenarios (Positive, Negative, วนตรวจทุกหน้า)
robot -d ../../img/results ../../img/robot_testcase.robot
```
*ผลการทดสอบผ่าน 8/8 Scenarios (100% Green) พร้อมบันทึกภาพหน้าจอหลักฐาน 17 ภาพใน [**`img/`**](../../img/)*


