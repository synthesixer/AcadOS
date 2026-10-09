# AcadOS v4 — Sequence Diagrams (PlantUML)

> อ้างอิง Implement Plan (Acad Lastest v6 + ข้อตกลงทีม) §20 · ไฟล์ `.puml` อยู่ใน `doc/diagrams/` · ภาพ PNG อยู่ในโฟลเดอร์ `sequence/` วางไว้ข้างไฟล์นี้

## สารบัญ

1. 4. ขั้นตอนการลงทะเบียนเรียน (Student Registration)
2. 5. ขั้นตอนการจัดตารางอัตโนมัติ (Generate / Publish / Discard)
3. 6. ขั้นตอนการแลกคาบของอาจารย์ (Teacher Swap)

---

## 4. ขั้นตอนการลงทะเบียนเรียน (Student Registration)

**อ้างอิง:** §14.1 · §10.2 RegistrationService · BR-03, BR-04, BR-05, BR-09 · §14.6 Email · §23.2 Scenario 1 · ไฟล์ `04-sequence-registration.puml`

![4. ขั้นตอนการลงทะเบียนเรียน (Student Registration)](sequence/04-sequence-registration.png)

```plantuml
@startuml 04-sequence-registration
title AcadOS - Sequence Diagram: Student Registration (S03)
autonumber
skinparam sequenceMessageAlign left
skinparam responseMessageBelowArrow true

actor Student
participant "Security Filter\n(JWT)" as Sec
participant "RegistrationApiController" as Ctrl
participant "RegistrationService" as Svc
participant "AcademicEventRepository" as EventRepo
participant "SectionRepository" as SectionRepo
participant "RegistrationRepository" as RegRepo
participant "ScheduleRepository" as SchRepo
participant "NotificationService" as Notif
participant "NotificationStrategy\n(InApp / Email)" as Channel
participant "GlobalExceptionHandler" as Err

Student -> Sec : POST /api/v1/registrations\n{sectionId} + JWT
Sec -> Sec : validate JWT, role = STUDENT
Sec -> Ctrl : forward request
Ctrl -> Svc : register(studentId, sectionId)
activate Svc

Svc -> EventRepo : find REGISTRATION_PERIOD
EventRepo --> Svc : period
alt today outside Registration Period (BR-09)
  Svc --> Err : throw BusinessRuleException
  Err --> Student : ErrorResponse (BR-09)
end

Svc -> SectionRepo : find section by id
SectionRepo --> Svc : section (+ course)
alt section status = CANCELLED
  Svc --> Err : throw BusinessRuleException
  Err --> Student : ErrorResponse (section cancelled)
end

Svc -> Svc : checkDuplicate()
Svc -> RegRepo : student already registered this course?
RegRepo --> Svc : result
alt same course already registered (BR-04)
  Svc --> Err : throw BusinessRuleException
  Err --> Student : ErrorResponse (BR-04)
end

Svc -> RegRepo : count registrations of section
RegRepo --> Svc : count
alt count >= section capacity (BR-05)
  Svc --> Err : throw BusinessRuleException
  Err --> Student : ErrorResponse (BR-05)
end

Svc -> Svc : checkConflict()
Svc -> SchRepo : PUBLISHED schedules of this section\nand of student's registered sections
SchRepo --> Svc : schedules (TimeSlots)
alt time overlap found (BR-03)
  Svc -> Notif : sendNotification(student, CONFLICT_DETECTED)
  Notif -> Channel : send() [InApp]
  note right of Notif
    saved in a new transaction (REQUIRES_NEW)
    so it is kept when register() rolls back
  end note
  Svc --> Err : throw BusinessRuleException
  Err --> Student : ErrorResponse (BR-03)
end

Svc -> RegRepo : save Registration\n(student, section, course = section.course)
RegRepo --> Svc : saved
Svc -> Notif : sendNotification(student, REGISTRATION_SUCCESS)
Notif -> Channel : send() [InApp]
Notif -> Channel : send() [Email]
Svc --> Ctrl : registration
deactivate Svc
Ctrl --> Student : 201 Created (Response DTO)

note over Svc, RegRepo
  DB safety net: UNIQUE (student, section) and UNIQUE (student, course)
end note
@enduml
```

---

## 5. ขั้นตอนการจัดตารางอัตโนมัติ (Generate / Publish / Discard)

**อ้างอิง:** §6.2 Schedule Status · §10.2–10.3 · §12 · D26, D28, D32, D34, D35, D36 · §17.2 Observer, Strategy · §23.2 Scenario 2 · ไฟล์ `05-sequence-scheduling.puml`

![5. ขั้นตอนการจัดตารางอัตโนมัติ (Generate / Publish / Discard)](sequence/05-sequence-scheduling.png)

```plantuml
@startuml 05-sequence-scheduling
title AcadOS - Sequence Diagram: Automatic Scheduling (Generate / Publish / Discard)
autonumber
skinparam sequenceMessageAlign left
skinparam responseMessageBelowArrow true

actor Admin
participant "Security Filter\n(JWT)" as Sec
participant "ScheduleApiController" as Ctrl
participant "SchedulingService\n(Orchestrator)" as Svc
participant "ScheduleRepository" as SchRepo
participant "SectionRepository" as SectionRepo
participant "RoomRepository" as RoomRepo
participant "TeacherPreferenceRepository\nTeacherQualificationRepository" as TRepo
participant "ConstraintEvaluator" as CE
participant "ScheduleSelector" as Sel
participant "ScoringStrategy\n(Preference / Workload /\nRoomSuitability TBA)" as Score
participant "ScheduleChangePublisher" as Pub
participant "NotificationService" as Notif
participant "NotificationStrategy\n(InApp)" as Channel
participant "GlobalExceptionHandler" as Err

== 1. Generate (result = DRAFT, no notification) ==
Admin -> Sec : POST /api/v1/schedules/generate + JWT
Sec -> Sec : validate JWT, role = ADMIN
Sec -> Ctrl : forward request
Ctrl -> Svc : generateSchedule()
activate Svc
Svc -> SchRepo : delete all DRAFT schedules
Svc -> SectionRepo : find ACTIVE sections without complete schedule\n(+ course weekly hours, home room if any)
SectionRepo --> Svc : sections

loop each section
  Svc -> Svc : split periods\n(Define Schedule Format = TBA, D26)
  Svc -> TRepo : teacher preferences / qualifications
  TRepo --> Svc : data
  note right of Svc
    D32: teacher without Preference
    -> random course the teacher is Qualified for
  end note
  alt section has home room
    Svc -> Svc : room options = home room
  else no home room (room = null)
    Svc -> RoomRepo : open rooms (isAvailable = true)\nwith capacity >= section capacity
    RoomRepo --> Svc : room options
  end
  Svc -> Svc : build Candidates (teacher x room x TimeSlots)\n1 Candidate = all periods of the Section
  loop each candidate
    Svc -> CE : validate(candidate)
    note right of CE
      Hard constraints (12.3):
      1 Qualification  2 Availability
      3 Teacher conflict  4 Room conflict
      5 Room availability  6 Student conflict
      7 Room capacity
      (conflicts count DRAFT + PUBLISHED)
    end note
    CE --> Svc : pass / reject (+ reason)
  end
  alt no candidate passed
    Svc -> Svc : record section + main reason (D28)
  else candidates passed
    Svc -> Sel : selectBest(candidates)
    loop each ScoringStrategy
      Sel -> Score : calculateScore(candidate)
      Score --> Sel : score
    end
    Sel -> Sel : baseline +100 + scores\nrank; tie -> random (D36)
    Sel --> Svc : best candidate (teacher, room, TimeSlots)
    Svc -> SchRepo : save schedules (status = DRAFT)
  end
end
Svc --> Ctrl : DRAFT result + sections that could not be scheduled (reasons)
deactivate Svc
Ctrl --> Admin : 200 OK (DRAFT timetable + failure list)

== 2a. Publish ==
Admin -> Sec : PUT /api/v1/schedules/publish + JWT
Sec -> Ctrl : forward (ADMIN)
Ctrl -> Svc : publishSchedule()
activate Svc
Svc -> SchRepo : update all DRAFT -> PUBLISHED
SchRepo --> Svc : published schedules
Svc -> SectionRepo : save room chosen by Generate\nas section.room (sections that had none)
Svc -> Pub : schedule changed (affected sections)
Pub -> Notif : sendNotification(SCHEDULE_CHANGED)\nteachers with new periods +\nstudents of affected sections
Notif -> Channel : send() [InApp]
Svc --> Ctrl : done
deactivate Svc
Ctrl --> Admin : 200 OK

== 2b. Discard ==
Admin -> Sec : DELETE /api/v1/schedules/draft + JWT
Sec -> Ctrl : forward (ADMIN)
Ctrl -> Svc : discardDraft()
Svc -> SchRepo : delete all DRAFT schedules
Ctrl --> Admin : 204 No Content
note over Admin, Ctrl : Admin may Generate again (go to 1)

== Errors ==
note over Svc, Err
  Any BusinessRuleException / ResourceNotFoundException
  -> GlobalExceptionHandler -> ErrorResponse
end note
@enduml
```

---

## 6. ขั้นตอนการแลกคาบของอาจารย์ (Teacher Swap)

**อ้างอิง:** §14.3 · §14.5 SWAP_CANCELLED · §14.6 Email · §16 /teacher-swaps · BR-01, BR-06, BR-07 · §28 ข้อ 5 · §23.2 Scenario 3 · ไฟล์ `06-sequence-teacher-swap.puml`

![6. ขั้นตอนการแลกคาบของอาจารย์ (Teacher Swap)](sequence/06-sequence-teacher-swap.png)

```plantuml
@startuml 06-sequence-teacher-swap
title AcadOS - Sequence Diagram: Teacher Swap (exchange periods A <-> B)
autonumber
skinparam sequenceMessageAlign left
skinparam responseMessageBelowArrow true

actor "Teacher A" as A
actor "Teacher B" as B
actor Admin
participant "Security Filter\n(JWT)" as Sec
participant "TeacherSwapApiController" as Ctrl
participant "TeacherSwapService" as Svc
participant "TeacherSwapRequest\n(entity)" as Req
participant "TeacherSwapRequestRepository" as SwapRepo
participant "ScheduleRepository" as SchRepo
participant "TeacherQualificationRepository\nTeacherAvailabilityRepository" as TRepo
participant "ScheduleChangePublisher" as Pub
participant "NotificationService" as Notif
participant "NotificationStrategy\n(InApp / Email)" as Channel
participant "GlobalExceptionHandler" as Err

== 1. Teacher A creates request ==
A -> Sec : POST /api/v1/teacher-swaps + JWT\n{requestingScheduleId, targetTeacherId, targetScheduleId}
Sec -> Ctrl : forward (TEACHER)
Ctrl -> Svc : createSwapRequest(...)
activate Svc
Svc -> SchRepo : load both schedules (must be PUBLISHED)
Svc -> TRepo : qualifications / availabilities of A and B
Svc -> SchRepo : teaching periods of A and B (conflict check)
Svc -> SwapRepo : open request (PENDING / ACCEPTED) on either schedule?
alt B = A, BR-06, BR-07, BR-01 failed,\nsame time slot or open request exists
  Svc --> Err : throw BusinessRuleException
  Err --> A : ErrorResponse
else all checks pass
  Svc -> SwapRepo : save request (status = PENDING)
  Svc -> Notif : sendNotification(B, SWAP_REQUESTED)
  Notif -> Channel : send() [InApp]
  Svc --> Ctrl : request
  Ctrl --> A : 201 Created
end
deactivate Svc

== 2. Teacher A cancels (only while PENDING) ==
opt A cancels before B responds
  A -> Sec : PUT /api/v1/teacher-swaps/{id}/cancel + JWT
  Sec -> Ctrl : forward (TEACHER)
  Ctrl -> Svc : cancel request (Teacher A)
  Svc -> SwapRepo : find request
  alt not A's request or status != PENDING
    Svc --> Err : throw BusinessRuleException
    Err --> A : ErrorResponse
  else
    Svc -> Req : cancel()  [PENDING -> CANCELLED]
    Svc -> SwapRepo : save
    Svc -> Notif : sendNotification(B, SWAP_CANCELLED)
    Notif -> Channel : send() [InApp]
    Ctrl --> A : 200 OK
  end
end

== 3. Teacher B responds ==
B -> Sec : PUT /api/v1/teacher-swaps/{id}/respond + JWT
Sec -> Ctrl : forward (TEACHER)
Ctrl -> Svc : respondSwap(id, accept / reject)
note right of Ctrl : Request body of /respond = TBA (§28 item 5)
activate Svc
Svc -> SwapRepo : find request
alt caller is not B or status != PENDING
  Svc --> Err : throw BusinessRuleException
  Err --> B : ErrorResponse
else reject
  Svc -> Req : reject()  [PENDING -> REJECTED]
  Svc -> SwapRepo : save (respondedAt)
  Svc -> Notif : sendNotification(A, SWAP_RESPONDED)
  Notif -> Channel : send() [InApp]
  Ctrl --> B : 200 OK
else accept
  Svc -> TRepo : re-check Qualification / Availability
  Svc -> SchRepo : re-check conflicts
  Svc -> Req : accept()  [PENDING -> ACCEPTED]
  Svc -> SwapRepo : save (respondedAt)
  Svc -> Notif : sendNotification(A + Admin, SWAP_RESPONDED)
  Notif -> Channel : send() [InApp]
  Ctrl --> B : 200 OK
end
deactivate Svc

== 4. Admin reviews (only ACCEPTED) ==
Admin -> Sec : PUT /api/v1/teacher-swaps/{id}/approve\nor /reject + JWT
Sec -> Ctrl : forward (ADMIN)
Ctrl -> Svc : approveSwap(id) / rejectSwap(id)
activate Svc
Svc -> SwapRepo : find request
alt status != ACCEPTED
  Svc --> Err : throw BusinessRuleException
  Err --> Admin : ErrorResponse
else rejectSwap
  Svc -> Req : reject()  [ACCEPTED -> REJECTED]
  Svc -> SwapRepo : save (reviewedAt)
  Svc -> Notif : sendNotification(A + B, SWAP_REJECTED)
  Notif -> Channel : send() [InApp]
  Ctrl --> Admin : 200 OK
else approveSwap
  Svc -> SchRepo : schedules still belong to A and B?
  Svc -> TRepo : re-run BR-06, BR-07
  Svc -> SchRepo : re-run BR-01 (counts DRAFT + PUBLISHED)
  alt validation failed
    Svc --> Err : throw BusinessRuleException
    Err --> Admin : ErrorResponse
  else valid
    Svc -> Req : approve()  [ACCEPTED -> APPROVED]
    Svc -> SchRepo : swap teacher of the 2 schedules (permanent)
    Svc -> SwapRepo : save (reviewedAt)
    Svc -> Notif : sendNotification(A + B, SWAP_APPROVED)\n[InApp + Email]
    Notif -> Channel : send() [InApp + Email]
    Svc -> Pub : schedule changed (2 sections)
    Pub -> Notif : sendNotification(students, SCHEDULE_CHANGED)
    Notif -> Channel : send() [InApp]
    Ctrl --> Admin : 200 OK
  end
end
deactivate Svc
@enduml
```
