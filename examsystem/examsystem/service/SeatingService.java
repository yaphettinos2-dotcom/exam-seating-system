package examsystem.service;

import examsystem.model.Department;
import examsystem.model.ExamRoom;
import examsystem.model.ExamSchedule;
import examsystem.model.Student;
import examsystem.util.Strings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * In-memory facade over every entity list: CRUD, search and seating generation.
 */
public class SeatingService {
    private final List<Department> departments = new ArrayList<>();
    private final List<Student> students = new ArrayList<>();
    private final List<ExamRoom> rooms = new ArrayList<>();
    private final List<ExamSchedule> schedules = new ArrayList<>();

    // ============ GENERIC HELPERS ============
    private static <T> T first(List<T> items, Predicate<T> match) {
        return items.stream().filter(match).findFirst().orElse(null);
    }

    /** Returns every item whose searchable fields contain the query (all items when blank). */
    private static <T> List<T> search(List<T> items, String query, Function<T, String[]> fields) {
        if (Strings.isBlank(query)) {
            return new ArrayList<>(items);
        }
        return items.stream()
                .filter(item -> Arrays.stream(fields.apply(item)).anyMatch(value -> Strings.contains(value, query)))
                .collect(Collectors.toList());
    }

    private static boolean anyBlank(String... values) {
        return Arrays.stream(values).anyMatch(Strings::isBlank);
    }

    /** True when the room exists and can still take a seat (the student's own seat excluded). */
    private boolean roomUnavailable(Integer roomNumber, Integer currentRoomNumber) {
        if (roomNumber == null) {
            return false;
        }
        ExamRoom room = findRoom(roomNumber);
        return room == null || (room.isFull() && !roomNumber.equals(currentRoomNumber));
    }

    // ============ DEPARTMENT OPERATIONS ============
    public boolean addDepartment(String code, String name) {
        if (anyBlank(code, name) || findDepartmentByCode(code) != null || findDepartmentByName(name) != null) {
            return false;
        }
        return departments.add(new Department(code, name));
    }

    public boolean updateDepartment(String currentCode, String newCode, String newName, String description) {
        Department department = findDepartmentByCode(currentCode);
        if (department == null || (!Strings.same(currentCode, newCode) && findDepartmentByCode(newCode) != null)) {
            return false;
        }
        department.setCode(newCode);
        department.setName(newName);
        department.setDescription(description);
        return true;
    }

    public boolean deleteDepartment(String code) {
        Department department = findDepartmentByCode(code);
        if (department == null || students.stream().anyMatch(student -> department.equals(student.getDepartment()))) {
            return false;
        }
        return departments.remove(department);
    }

    public List<Department> getAllDepartments() {
        return new ArrayList<>(departments);
    }

    public Department findDepartmentByCode(String code) {
        return code == null ? null : first(departments, department -> Strings.same(department.getCode(), Strings.upper(code)));
    }

    public Department findDepartmentByName(String name) {
        return name == null ? null : first(departments, department -> Strings.contains(department.getName(), name));
    }

    public List<Department> searchDepartments(String query) {
        return search(departments, query, department -> new String[]{department.getCode(), department.getName()});
    }

    // ============ STUDENT OPERATIONS ============
    public boolean addStudent(Student student) {
        if (student == null || Strings.isBlank(student.getStudentId()) || student.getDepartment() == null
                || findStudentById(student.getStudentId()) != null
                || roomUnavailable(student.getAssignedRoomNumber(), null)) {
            return false;
        }
        students.add(student);
        refreshSeatingAssignments();
        return true;
    }

    public boolean addStudent(String name, String email, String studentId, String departmentCode) {
        return addStudent(name, email, studentId, departmentCode, null);
    }

    public boolean addStudent(String name, String email, String studentId, String departmentCode, Integer roomNumber) {
        Department department = findDepartmentByCode(departmentCode);
        if (anyBlank(name, email, studentId, departmentCode) || department == null
                || findStudentById(studentId) != null || roomUnavailable(roomNumber, null)) {
            return false;
        }
        Student student = new Student(name, email, studentId, department);
        student.setAssignedRoomNumber(roomNumber);
        return addStudent(student);
    }

    public List<Student> getAllStudents() {
        return new ArrayList<>(students);
    }

    public List<Student> searchStudents(String query) {
        return search(students, query, student -> new String[]{student.getStudentId(), student.getName(),
                student.getDepartment() != null ? student.getDepartment().getName() : ""});
    }

    public Student findStudentById(String studentId) {
        return studentId == null ? null : first(students, student -> Strings.same(student.getStudentId(), Strings.upper(studentId)));
    }

    public Student findStudentByName(String name) {
        return name == null ? null : first(students, student -> Strings.contains(student.getName(), name));
    }

    public boolean updateStudent(String studentId, String newName, String newEmail, String newDepartmentCode) {
        return updateStudent(studentId, newName, newEmail, newDepartmentCode, null);
    }

    public boolean updateStudent(String studentId, String newName, String newEmail, String newDepartmentCode, Integer roomNumber) {
        Student student = findStudentById(studentId);
        Department department = findDepartmentByCode(newDepartmentCode);
        if (student == null || department == null || roomUnavailable(roomNumber, student.getAssignedRoomNumber())) {
            return false;
        }
        student.setName(newName);
        student.setEmail(newEmail);
        student.setDepartment(department);
        student.setAssignedRoomNumber(roomNumber);
        refreshSeatingAssignments();
        return true;
    }

    public boolean deleteStudent(String studentId) {
        Student toRemove = findStudentById(studentId);
        if (toRemove == null) {
            return false;
        }
        students.remove(toRemove);
        schedules.forEach(schedule -> schedule.removeStudent(toRemove));
        refreshSeatingAssignments();
        return true;
    }

    // ============ ROOM OPERATIONS ============
    public boolean addRoom(ExamRoom room) {
        if (room == null || findRoom(room.getRoomNumber()) != null) {
            return false;
        }
        return rooms.add(room);
    }

    public boolean addRoom(int roomNumber, String building, int capacity) {
        return capacity >= 1 && addRoom(new ExamRoom(roomNumber, building, capacity));
    }

    public List<ExamRoom> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public List<ExamRoom> searchRooms(String query) {
        return search(rooms, query, room -> new String[]{String.valueOf(room.getRoomNumber()), room.getBuilding()});
    }

    public ExamRoom findRoom(int roomNumber) {
        return first(rooms, room -> room.getRoomNumber() == roomNumber);
    }

    public ExamRoom findAvailableRoom(int requiredCapacity) {
        return first(rooms, room -> room.canHost(requiredCapacity));
    }

    public boolean updateRoom(int currentRoomNumber, int newRoomNumber, String building, int capacity) {
        ExamRoom room = findRoom(currentRoomNumber);
        if (room == null || capacity < room.getAssignedStudents().size()
                || (currentRoomNumber != newRoomNumber && findRoom(newRoomNumber) != null)) {
            return false;
        }
        room.setRoomNumber(newRoomNumber);
        room.setBuilding(building);
        room.setCapacity(capacity);
        refreshSeatingAssignments();
        return true;
    }

    public boolean deleteRoom(int roomNumber) {
        ExamRoom toRemove = findRoom(roomNumber);
        if (toRemove == null) {
            return false;
        }
        rooms.remove(toRemove);
        schedules.stream().filter(schedule -> schedule.getAssignedRoom() == toRemove)
                .forEach(schedule -> schedule.assignRoom(null));
        refreshSeatingAssignments();
        return true;
    }

    // ============ SCHEDULE OPERATIONS ============
    public boolean addSchedule(ExamSchedule schedule) {
        if (schedule == null || findSchedule(schedule.getCourseCode()) != null) {
            return false;
        }
        return schedules.add(schedule);
    }

    public boolean createSchedule(String courseCode, String courseName, LocalDateTime dateTime, int duration) {
        if (anyBlank(courseCode, courseName) || duration < 1) {
            return false;
        }
        return addSchedule(new ExamSchedule(courseCode, courseName, dateTime, duration));
    }

    public boolean updateSchedule(String courseCode, String newCode, String newName, LocalDateTime dateTime, int duration) {
        ExamSchedule schedule = findSchedule(courseCode);
        if (schedule == null || (!Strings.same(courseCode, newCode) && findSchedule(newCode) != null)) {
            return false;
        }
        schedule.setCourseCode(newCode);
        schedule.setCourseName(newName);
        schedule.setDateTime(dateTime);
        schedule.setDuration(duration);
        return true;
    }

    public boolean deleteSchedule(String courseCode) {
        ExamSchedule toRemove = findSchedule(courseCode);
        if (toRemove == null) {
            return false;
        }
        schedules.remove(toRemove);
        refreshSeatingAssignments();
        return true;
    }

    public List<ExamSchedule> getAllSchedules() {
        return new ArrayList<>(schedules);
    }

    public List<ExamSchedule> searchSchedules(String query) {
        return search(schedules, query, schedule -> new String[]{schedule.getCourseCode(), schedule.getCourseName()});
    }

    public ExamSchedule findSchedule(String courseCode) {
        return courseCode == null ? null : first(schedules, schedule -> Strings.same(schedule.getCourseCode(), Strings.upper(courseCode)));
    }

    public boolean enrollStudentInExam(String studentId, String courseCode) {
        Student student = findStudentById(studentId);
        ExamSchedule schedule = findSchedule(courseCode);
        if (student == null || schedule == null || schedule.containsStudent(student)) {
            return false;
        }
        schedule.addStudent(student);
        refreshSeatingAssignments();
        return true;
    }

    public boolean removeStudentFromExam(String studentId, String courseCode) {
        Student student = findStudentById(studentId);
        ExamSchedule schedule = findSchedule(courseCode);
        if (student == null || schedule == null) {
            return false;
        }
        schedule.removeStudent(student);
        refreshSeatingAssignments();
        return true;
    }

    // ============ SEATING GENERATION ============
    /** Clears every allocation and gives each non-empty exam its own room. */
    public boolean generateSeatingForAllExams() {
        resetRoomAssignments();
        Set<ExamRoom> allocated = new HashSet<>();
        boolean assignedAny = false;
        for (ExamSchedule schedule : schedules) {
            ExamRoom room = schedule.getStudentCount() > 0
                    ? findUnallocatedRoom(schedule.getStudentCount(), allocated)
                    : null;
            schedule.assignRoom(room);
            if (room != null) {
                allocated.add(room);
                assignedAny = true;
            }
        }
        return assignedAny;
    }

    public boolean generateSeatingForExam(String courseCode) {
        ExamSchedule schedule = findSchedule(courseCode);
        if (schedule == null || schedule.getStudentCount() == 0) {
            return false;
        }
        Set<ExamRoom> allocated = schedules.stream()
                .filter(other -> other != schedule && other.getAssignedRoom() != null)
                .map(ExamSchedule::getAssignedRoom)
                .collect(Collectors.toCollection(HashSet::new));
        ExamRoom room = findUnallocatedRoom(schedule.getStudentCount(), allocated);
        schedule.assignRoom(room);
        return room != null;
    }

    public void refreshSeatingAssignments() {
        generateSeatingForAllExams();
    }

    private ExamRoom findUnallocatedRoom(int requiredCapacity, Set<ExamRoom> allocatedRooms) {
        return first(rooms, room -> !allocatedRooms.contains(room) && room.canHost(requiredCapacity));
    }

    /** Detaches every exam from its room and restores the manual student-to-room reservations. */
    private void resetRoomAssignments() {
        schedules.forEach(schedule -> schedule.assignRoom(null));
        rooms.forEach(ExamRoom::clearSeating);
        for (Student student : students) {
            ExamRoom room = student.getAssignedRoomNumber() == null ? null : findRoom(student.getAssignedRoomNumber());
            if (room != null) {
                room.assignStudent(student);
            }
        }
    }

    // ============ STATISTICS ============
    public int getTotalDepartments() { return departments.size(); }
    public int getTotalStudents() { return students.size(); }
    public int getTotalRooms() { return rooms.size(); }
    public int getTotalSchedules() { return schedules.size(); }

    public int getTotalAssignedStudents() {
        return schedules.stream().mapToInt(ExamSchedule::getStudentCount).sum();
    }

    public String getSummary() {
        return "=== SYSTEM SUMMARY ===\n"
                + "Total Departments: " + getTotalDepartments() + "\n"
                + "Total Students: " + getTotalStudents() + "\n"
                + "Total Rooms: " + getTotalRooms() + "\n"
                + "Total Schedules: " + getTotalSchedules() + "\n"
                + "Total Enrollments: " + getTotalAssignedStudents() + "\n";
    }
}
