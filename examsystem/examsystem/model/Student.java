package examsystem.model;

import examsystem.util.Strings;

public class Student extends Person {
    private String studentId;
    private Department department;
    private int year = 1;
    private Integer assignedRoomNumber;

    public Student(String name, String email, String studentId, Department department) {
        super(name, email);
        setStudentId(studentId);
        this.department = department;
    }

    private String departmentName() {
        return department != null ? department.getName() : "None";
    }

    @Override
    public void display() {
        System.out.println("Student: " + name + " (ID: " + studentId + ")");
        System.out.println("  Department: " + departmentName());
        System.out.println("  Year: " + year);
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = Strings.upper(studentId); }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public Integer getAssignedRoomNumber() { return assignedRoomNumber; }
    public void setAssignedRoomNumber(Integer assignedRoomNumber) { this.assignedRoomNumber = assignedRoomNumber; }

    @Override
    public String toString() {
        return String.format("%s | %s | %s", studentId, name, departmentName());
    }
}
