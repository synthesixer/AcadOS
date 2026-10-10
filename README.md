# AcadOS
Automated Proctor Scheduling and Academic Operations System (Version v4)

AcadOS คือระบบบริหารจัดการงานวิชาการและจัดสรรตารางคุมสอบและตารางเรียน/ตารางสอนอัตโนมัติ พัฒนาด้วย Spring Boot 3 และ Java 21 ตามสถาปัตยกรรม Layered Architecture ระบบมีจุดเด่นในการใช้อัลกอริทึมสุ่มจัดตารางแบบมีเงื่อนไข (Constraint-based Randomization) และระบบการให้คะแนนแบบหลายปัจจัย เพื่อป้องกันปัญหาตารางชนกันและช่วยกระจายภาระงานของบุคลากรอย่างเป็นธรรม

> 📖 **เอกสารข้อกำหนดทางเทคนิคฉบับเต็ม:** ดูรายละเอียดทั้งหมดได้ที่ [doc/Implement_Plan-AcadOS.md](doc/Implement_Plan-AcadOS.md)

---

## สมาชิกกลุ่ม (Team Members)

| ลำดับ | ชื่อ - นามสกุล | รหัสนักศึกษา | Section | Git Branch | หน้าที่รับผิดชอบ |
| :---: | :--- | :---: | :---: | :--- | :--- |
| 1 | นายพุฒิเมธ ชมศรีสวัสดิ์ | 673380417-1 | Sec 3 | `puttimed_6733804171_03` | Scheduling Model, Constraint Engine, Scoring Strategies, Testing |
| 2 | นายวงศกร สงวนกลิ่น | 673380424-4 | Sec 4 | `wongsakorn_6733804244_04` | Backend Setup, MySQL, JPA Entity, Repositories, Security & JWT, DB Schema |
| 3 | นายจิรภัทร สีสาร | 673380574-5 | Sec 3 | `jirapat_6733805745_03` | Frontend UI (Thymeleaf), REST API Controller, DTO & Mapper, API Tests, Swagger |

---

## Tech Stack
- **Backend Framework:** Spring Boot 3.3.4 (Java 21 LTS)
- **Persistence & ORM:** Spring Data JPA / Hibernate
- **Database:** MySQL 8.x
- **Security:** Spring Security + JWT, BCrypt
- **API Documentation:** Springdoc OpenAPI 2.6.0 (Swagger UI)
- **Frontend:** Thymeleaf + HTML5 / CSS3 / JavaScript
- **Testing:** JUnit 5, Mockito, Spring Boot Test, Testcontainers
- **Containerization:** Docker & Docker Compose

---

## System Architecture
ระบบพัฒนาด้วยรูปแบบ **Layered Architecture** ร่วมกับ **MVC Pattern**:
```
Presentation Layer (Controller / REST API / Thymeleaf View)
       │
       ▼
Service Layer (Business Logic / Transaction Management)
       │
       ▼
Repository Layer (Spring Data JPA)
       │
       ▼
Database Layer (MySQL Database)
```
- **Architectural Patterns:** Layered Architecture, MVC, Repository, Service Layer, DTO Pattern + Mapper, Constructor Dependency Injection
- **GoF Design Patterns:** Strategy (Scoring & Notification), Observer (Schedule Change), State (Section State), Adapter (Holiday API)

---

## Database Design (ER Diagram)
- ข้อมูลโมเดลฐานข้อมูลมีทั้งหมด 16 Entities โดยมีความสัมพันธ์ครบถ้วนทั้ง One-to-One และ One-to-Many
- ดูรายละเอียดโครงสร้างเชิงแนวคิดได้ใน [doc/Implement_Plan-AcadOS.md](doc/Implement_Plan-AcadOS.md), [doc/database.md](doc/database.md) และ [doc/diagram/](doc/diagram/)

---

## Installation & Setup
1. **ติดตั้งเครื่องมือที่จำเป็น:**
   - Java Development Kit (JDK 21 LTS)
   - Apache Maven 3.9+
   - Docker & Docker Compose (สำหรับ Database)
2. **Clone Repository:**
   ```bash
   git clone https://github.com/synthesixer/AcadOS.git
   cd AcadOS/code/acados
   ```

---

## How to Run
1. **สตาร์ทฐานข้อมูล MySQL ด้วย Docker:**
   ```bash
   docker compose up -d
   ```
2. **รันแอปพลิเคชัน Spring Boot:**
   ```bash
   mvn spring-boot:run
   ```
3. **เข้าใช้งานระบบ:**
   - Web Application: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## API Documentation
- ระบบมีเอกสาร API แบบโต้ตอบได้ผ่าน **Swagger UI / OpenAPI v3** เข้าถึงได้ที่:
  `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON Spec: `http://localhost:8080/api-docs`

---

## How to Run Tests
รันชุดการทดสอบ Unit Tests และ Integration Tests ทั้งหมด:
```bash
mvn clean test
```

---

## Deployment URL
- **Public URL:** TBA (จะระบุเมื่อ Deploy ขึ้น Cloud/Server ในขั้นตอนต่อไป)

---

## Project Structure
```
AcadOS/
├── code/
│   └── acados/                   # Source code Spring Boot + Maven POM
│       ├── src/
│       │   ├── main/java/com/project/acados/
│       │   ├── main/resources/
│       │   └── test/java/com/project/acados/
│       ├── pom.xml
│       ├── Dockerfile
│       └── docker-compose.yml
├── test/                         # เอกสารและรายงานผลการทดสอบ
├── doc/                          # Technical Specifications & Diagrams
│   ├── Implement_Plan-AcadOS.md  # แผนการพัฒนาและสเปกระบบฉบับสมบูรณ์
│   ├── database.md               # รายละเอียด Schema & Data Dictionary
│   ├── prof_ruleset.md           # ข้อกำหนดตามอาจารย์ประจำวิชา
│   ├── PROJECT_STATUS.md         # สรุปสถานะและ Task Memory
│   ├── diagram/                  # ไดอะแกรมระบบ (Class, Component, Activity, Use Case, ER ฯลฯ)
│   └── slide/                    # สไลด์นำเสนอ (TBA)
├── img/                          # รูปภาพและ Asset ประกอบ
├── README.md
└── .gitignore
```
