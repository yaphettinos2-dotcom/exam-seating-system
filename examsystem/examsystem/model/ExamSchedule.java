package examsystem.model;

import examsystem.util.Strings;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExamSchedule {
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String courseCode;
    private String courseName;
    private LocalDateTime dateTime;
    private int duration;
    private final List<Student> enrolledStudents = new ArrayList<>();
    private ExamRoom assignedRoom;

    public ExamSchedule(String courseCode, String courseName, LocalDateTime dateTime, int duration) {
        setCourseCode(courseCode);
        setCourseName(courseName);
        setDateTime(dateTime);
        setDuration(duration);
    }

    public void addStudent(Student student) {
        if (student != null && !containsStudent(student)) {
            enrolledStudents.add(student);
        }
    }

    public void removeStudent(Student student) {
        enrolledStudents.removeIf(existing -> Strings.same(existing.getStudentId(), student.getStudentId()));
    }

    public boolean containsStudent(Student student) {
        return enrolledStudents.stream().anyMatch(existing -> Strings.same(existing.getStudentId(), student.getStudentId()));
    }

    /** Links this exam to a room and seats every enrolled student in it. */
    public void assignRoom(ExamRoom room) {
        this.assignedRoom = room;
        if (room != null) {
            enrolledStudents.forEach(room::assignStudent);
        }
    }

    public String getScheduleInfo() {
        StringBuilder sb = new StringBuilder()
                .append("Course: ").append(courseCode).append(" - ").append(courseName).append("\n")
                .append("Date/Time: ").append(dateTime.format(FORMAT)).append("\n")
                .append("Duration: ").append(duration).append(" minutes\n")
                .append("Students: ").append(enrolledStudents.size()).append("\n");
        if (assignedRoom == null) {
            return sb.append("No room assigned yet!\n").toString();
        }
        return sb.append("Assigned Room: ").append(assignedRoom.getRoomNumber())
                .append(" | Available Seats: ").append(assignedRoom.getAvailableSeats()).append("\n")
                .append(assignedRoom.getSeatingChartAsString())
                .toString();
    }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = Strings.upper(courseCode); }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = Strings.clean(courseName); }
    public LocalDateTime getDateTime() { return dateTime; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime == null ? LocalDateTime.now() : dateTime; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = Math.max(1, duration); }
    public List<Student> getEnrolledStudents() { return new ArrayList<>(enrolledStudents); }
    public ExamRoom getAssignedRoom() { return assignedRoom; }
    public int getStudentCount() { return enrolledStudents.size(); }

    @Override
    public String toString() {
        return String.format("%s | %s | %d students | Room: %s", courseCode, courseName, enrolledStudents.size(),
                assignedRoom != null ? String.valueOf(assignedRoom.getRoomNumber()) : "None");
    }
}
