*** Settings ***
Documentation     AcadOS (Automated Academic Operations System) — End-to-End Automated Acceptance Test Suite
...               Automated test suite using Robot Framework and SeleniumLibrary
...               Covers Positive & Negative Flows: Authentication, Admin Dashboard & Management Pages,
...               Timetable Scheduling Engine, Student Registration & History, Teacher Swap Workflow,
...               Unauthorized Access Security Checks, and Swagger UI / Actuator Observability.
...               Captures step-by-step screenshots into the 'img/' directory for SQA reporting.

Library           SeleniumLibrary    run_on_failure=Capture Page Screenshot

Test Setup        Setup Browser Environment
Test Teardown     Close Browser

*** Variables ***
${BASE_URL}           http://localhost:8080
${BROWSER}            chrome
${IMG_DIR}            ${CURDIR}/../img
${DEFAULT_TIMEOUT}    15s
${PASSWORD}           password123

*** Test Cases ***
TC_NEG_01 - เข้าสู่ระบบด้วยรหัสผ่านไม่ถูกต้อง (Negative: Invalid Password)
    [Documentation]    ทดสอบกรอกรหัสผ่านผิด ระบบต้องแสดงกล่องแจ้งเตือนและไม่อนุญาตให้เข้าสู่ระบบ
    [Tags]             Negative    Auth
    Go To Login Page
    Login With Credentials     admin    wrongpassword123
    Wait Until Element Is Visible    id=login-error    timeout=${DEFAULT_TIMEOUT}
    Element Should Contain           id=login-error    Invalid University ID or password
    Location Should Contain          /login
    Capture Page Screenshot          ${IMG_DIR}/TC_NEG_01_invalid_password.png

TC_NEG_02 - เข้าสู่ระบบด้วยบัญชีผู้ใช้ที่ไม่มีในระบบ (Negative: Non-Existent User)
    [Documentation]    ทดสอบกรอก University ID ที่ไม่มีอยู่จริง ระบบต้องปฏิเสธการล็อกอิน
    [Tags]             Negative    Auth
    Go To Login Page
    Login With Credentials     UNKNOWN999    password123
    Wait Until Element Is Visible    id=login-error    timeout=${DEFAULT_TIMEOUT}
    Element Should Contain           id=login-error    Invalid University ID or password
    Location Should Contain          /login
    Capture Page Screenshot          ${IMG_DIR}/TC_NEG_02_unknown_user.png

TC_NEG_03 - พยายามเข้าถึงหน้า Protected โดยไม่ผ่านการล็อกอิน (Negative: Unauthorized Direct Access)
    [Documentation]    ทดสอบเข้าถึง URL /admin/dashboard โดยตรงขณะยังไม่ได้ล็อกอิน ระบบต้องปฏิเสธด้วย HTTP 401 Unauthorized
    [Tags]             Negative    Security
    Go To    ${BASE_URL}/admin/dashboard
    Wait Until Page Contains    Unauthorized    timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot     ${IMG_DIR}/TC_NEG_03_unauthorized_access.png

TC01 - เข้าสู่ระบบในฐานะ Admin และตรวจสอบเมนูการจัดการทั้งหมด (Positive: Admin Full Operations)
    [Documentation]    ทดสอบเข้าสู่ระบบด้วยบัญชี Admin (admin / password123) และตรวจสอบหน้าจัดการระบบของผู้ดูแลระบบครบทุกหน้า
    [Tags]             Auth    Admin    Positive
    Go To Login Page
    Capture Page Screenshot    ${IMG_DIR}/TC01_01_login_page.png
    Login With Credentials     admin    ${PASSWORD}
    Wait Until Location Contains    /admin/dashboard    timeout=${DEFAULT_TIMEOUT}
    Wait Until Page Contains        ภาพรวมระบบ          timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC01_02_admin_dashboard.png
    
    # ตรวจสอบหน้าจัดการรายวิชา
    Go To    ${BASE_URL}/admin/courses
    Wait Until Location Contains    /admin/courses      timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC01_03_admin_courses.png
    
    # ตรวจสอบหน้าจัดการห้องเรียน
    Go To    ${BASE_URL}/admin/rooms
    Wait Until Location Contains    /admin/rooms        timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC01_04_admin_rooms.png
    
    # ตรวจสอบหน้าจัดการกลุ่มเรียน
    Go To    ${BASE_URL}/admin/sections
    Wait Until Location Contains    /admin/sections     timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC01_05_admin_sections.png
    
    # ตรวจสอบหน้าจัดการผู้ใช้งาน
    Go To    ${BASE_URL}/admin/users
    Wait Until Location Contains    /admin/users        timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC01_06_admin_users.png

TC02 - ผู้ดูแลระบบสั่งจัดตารางสอนอัตโนมัติ (Positive: Automated Timetable Generation)
    [Documentation]    ทดสอบ Admin เข้าหน้า Timetable Workbench สั่ง Generate และแคปภาพตารางเรียน
    [Tags]             Scheduling    Admin    Positive
    Go To Login Page
    Login With Credentials     admin    ${PASSWORD}
    Wait Until Location Contains    /admin/dashboard    timeout=${DEFAULT_TIMEOUT}
    Go To    ${BASE_URL}/timetable
    Wait Until Location Contains    /timetable          timeout=${DEFAULT_TIMEOUT}
    Wait Until Page Contains        ตารางสอนและตารางเรียน    timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC02_01_timetable_workbench.png
    
    # สั่งกดปุ่ม Generate ตารางสอนอัตโนมัติ
    Wait Until Element Is Visible    id=btn-generate    timeout=${DEFAULT_TIMEOUT}
    Click Button    id=btn-generate
    Sleep    2s
    Capture Page Screenshot    ${IMG_DIR}/TC02_02_schedule_generated.png

TC03 - เข้าสู่ระบบในฐานะ Student และตรวจสอบระบบบริการนักศึกษา (Positive: Student Portal Tour)
    [Documentation]    ทดสอบเข้าสู่ระบบด้วยบัญชีนักศึกษา (S001 / password123) และตรวจสอบหน้าบริการนักศึกษา
    [Tags]             Student    Registration    Positive
    Go To Login Page
    Login With Credentials     S001    ${PASSWORD}
    Wait Until Location Contains    /student/dashboard    timeout=${DEFAULT_TIMEOUT}
    Wait Until Page Contains        ศูนย์บริการนักศึกษา    timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC03_01_student_dashboard.png
    
    # เดินทางไปยังหน้าค้นหาและลงทะเบียนรายวิชา
    Go To    ${BASE_URL}/student/courses
    Wait Until Location Contains    /student/courses    timeout=${DEFAULT_TIMEOUT}
    Wait Until Page Contains        ลงทะเบียน            timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC03_02_student_courses.png
    
    # เดินทางไปยังหน้ารายวิชาที่ลงทะเบียนแล้ว
    Go To    ${BASE_URL}/student/registrations
    Wait Until Location Contains    /student/registrations    timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC03_03_student_registrations.png

TC04 - เข้าสู่ระบบในฐานะ Teacher และตรวจสอบคำขอแลกคาบสอน (Positive: Teacher Swap Operations)
    [Documentation]    ทดสอบเข้าสู่ระบบด้วยบัญชีอาจารย์ (T001 / password123) และเปิดดูกล่องข้อความคำขอสลับคาบสอน
    [Tags]             Teacher    Swap    Positive
    Go To Login Page
    Login With Credentials     T001    ${PASSWORD}
    Wait Until Location Contains    /teacher/dashboard    timeout=${DEFAULT_TIMEOUT}
    Wait Until Page Contains        ศูนย์อาจารย์ผู้สอน    timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC04_01_teacher_dashboard.png
    
    # เดินทางไปยังหน้าแลกคาบสอน
    Go To    ${BASE_URL}/teacher/swaps
    Wait Until Location Contains    /teacher/swaps        timeout=${DEFAULT_TIMEOUT}
    Wait Until Page Contains        คำขอแลกคาบสอน        timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC04_02_teacher_swaps_inbox.png

TC05 - ตรวจสอบ Swagger UI และ Spring Actuator Healthcheck (Positive: System Observability)
    [Documentation]    ตรวจสอบการเข้าถึงเอกสาร API Swagger UI และ Health Liveness Endpoint
    [Tags]             API    Monitoring    Swagger    Positive
    Go To    ${BASE_URL}/swagger-ui.html
    Wait Until Page Contains    AcadOS    timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC05_01_swagger_ui.png
    
    Go To    ${BASE_URL}/actuator/health
    Wait Until Page Contains    UP    timeout=${DEFAULT_TIMEOUT}
    Capture Page Screenshot    ${IMG_DIR}/TC05_02_actuator_health.png

*** Keywords ***
Setup Browser Environment
    [Documentation]    ตั้งค่าการเปิด Web Browser แบบรองรับ CI/Headless ในสภาพแวดล้อมที่สะอาด
    ${chrome_options}=    Evaluate    sys.modules['selenium.webdriver'].ChromeOptions()    sys
    Call Method    ${chrome_options}    add_argument    --window-size\=1920,1080
    Call Method    ${chrome_options}    add_argument    --no-sandbox
    Call Method    ${chrome_options}    add_argument    --disable-dev-shm-usage
    Create Webdriver    Chrome    options=${chrome_options}
    Set Selenium Implicit Wait    5s

Go To Login Page
    [Documentation]    เปิดหน้าเข้าสู่ระบบและรอจนฟอร์มพร้อมใช้งาน
    Go To    ${BASE_URL}/login
    Wait Until Element Is Visible    id=university-id    timeout=${DEFAULT_TIMEOUT}
    Wait Until Element Is Visible    id=password         timeout=${DEFAULT_TIMEOUT}

Login With Credentials
    [Arguments]    ${username}    ${pass}
    [Documentation]    กรอกรหัสผู้ใช้งานและรหัสผ่าน จากนั้นกดปุ่มเข้าสู่ระบบ
    Wait Until Element Is Visible    id=university-id    timeout=${DEFAULT_TIMEOUT}
    Clear Element Text               id=university-id
    Input Text                       id=university-id    ${username}
    Wait Until Element Is Visible    id=password         timeout=${DEFAULT_TIMEOUT}
    Clear Element Text               id=password
    Input Text                       id=password         ${pass}
    Wait Until Element Is Enabled    id=submit-button    timeout=${DEFAULT_TIMEOUT}
    Click Button                     id=submit-button
