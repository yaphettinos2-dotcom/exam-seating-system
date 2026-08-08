package examsystem.tests;

import examsystem.model.ExamRoom;
import examsystem.model.ExamSchedule;
import examsystem.model.Student;
import examsystem.service.SeatingService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class SeatingServiceRoomCapacityTest {
    public static void main(String[] args) {
        SeatingService service = new SeatingService();
        service.addDepartment("CS", "Computer Science");
        service.addRoom(101, "Main Building", 10);
        service.addStudent("Alice Johnson", "alice@example.com", "S001", "CS", 101);
        service.addStudent("Bob Smith", "bob@example.com", "S002", "CS", 101);

        ExamRoom room = service.findRoom(101);
        if (room == null || room.getRemainingCapacity() != 8 || !room.isOccupied()) {
            throw new AssertionError("Adding a student to a room should update its occupancy immediately");
        }

        service.createSchedule("CS101", "Intro to Programming", LocalDateTime.now(), 60);
        service.enrollStudentInExam("S001", "CS101");
        service.enrollStudentInExam("S002", "CS101");

        service.generateSeatingForAllExams();

        if (room == null) {
            throw new AssertionError("Expected room 101 to exist");
        }
        if (!room.canHost(3)) {
            throw new AssertionError("Room should remain available while it still has seats available");
        }
        if (room.getRemainingCapacity() != 8) {
            throw new AssertionError("Room should have 8 seats remaining after two students were assigned");
        }

        service.addStudent("Carol Jones", "carol@example.com", "S003", "CS", 101);
        if (room.getRemainingCapacity() != 7 || room.getAssignedStudents().size() != 3) {
            throw new AssertionError("Adding a student after seating generation should refresh room occupancy");
        }

        Student alice = service.findStudentById("S001");
        if (alice == null || alice.getAssignedRoomNumber() != 101) {
            throw new AssertionError("Student should be assigned to the selected room number");
        }

        SeatingService allocationService = new SeatingService();
        allocationService.addDepartment("ENG", "Engineering");
        allocationService.addRoom(201, "North", 10);
        allocationService.addRoom(202, "North", 10);
        allocationService.addRoom(203, "North", 10);
        for (int index = 1; index <= 3; index++) {
            String id = "E00" + index;
            String course = "ENG10" + index;
            allocationService.addStudent("Student " + index, "student" + index + "@example.com", id, "ENG");
            allocationService.createSchedule(course, "Exam " + index, LocalDateTime.now(), 60);
            allocationService.enrollStudentInExam(id, course);
        }
        allocationService.generateSeatingForAllExams();
        Set<Integer> allocatedRoomNumbers = new HashSet<>();
        for (ExamSchedule schedule : allocationService.getAllSchedules()) {
            if (schedule.getAssignedRoom() == null) {
                throw new AssertionError("Every exam should receive a room when sufficient rooms exist");
            }
            allocatedRoomNumbers.add(schedule.getAssignedRoom().getRoomNumber());
        }
        if (allocatedRoomNumbers.size() != 3) {
            throw new AssertionError("Each exam should have its own room in the seating chart");
        }

        System.out.println("Room capacity and distinct room allocation tests passed");
    }
}
