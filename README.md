# Exam Seating System

A Java Swing desktop application for managing exam schedules, rooms, and student seating — with role-based login for admins and students.

## Features

- **Role-based authentication** — separate admin and student accounts, with passwords hashed using **PBKDF2WithHmacSHA256** (salted, never stored in plain text)
- **Admin dashboard** — full CRUD for departments, students, exam rooms, and exam schedules
- **Automatic seat assignment** — generates a seating grid per room based on capacity and assigns enrolled students to available seats
- **Student portal accounts** — admins can provision a login for a student directly from their record
- **Student dashboard** — a read-only view where students can see their upcoming exams and assigned seat
- **In-memory data layer** — a `SeatingService` facade backs everything; no database required to run it

## Tech stack

- **Java** (Swing for the UI)
- No external dependencies — built on the standard JDK (`javax.swing`, `javax.crypto`)

## Project structure

```
examsystem/
├── auth/
│   ├── AuthenticationProvider.java     # authentication interface
│   ├── AuthenticationService.java      # PBKDF2 password hashing + verification
│   ├── StudentAccountProvisioner.java  # creates student portal accounts
│   └── UserSession.java                # current session identity (role, student id)
├── model/
│   ├── Person.java, Student.java       # core entities
│   ├── Department.java
│   ├── ExamRoom.java                   # seating grid logic
│   └── ExamSchedule.java
├── service/
│   └── SeatingService.java             # CRUD + seating generation facade
├── ui/
│   ├── LoginFrame.java
│   ├── MainFrame.java                  # admin dashboard
│   ├── StudentDashboard.java           # student-facing read-only view
│   └── CrudTab.java
├── tests/
│   └── SeatingServiceRoomCapacityTest.java
└── util/
    └── Strings.java
```

## Running it

No build tool required — compile and run directly with the JDK.

```bash
# from the folder containing the examsystem/ package
javac examsystem/**/*.java -d out
java -cp out examsystem.ui.MainFrame
```

The login screen will open first. Provision an account through the admin flow, or check `AuthenticationService` for how accounts are seeded.

## Running the tests

The test suite is a standalone assertion-based runner (no JUnit dependency):

```bash
javac examsystem/**/*.java -d out
java -cp out examsystem.tests.SeatingServiceRoomCapacityTest
```

It exits silently on success and throws an `AssertionError` on failure.

## License

MIT — see [`LICENSE`](./LICENSE).
