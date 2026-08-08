package examsystem.model;

import examsystem.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ExamRoom {
    private int roomNumber;
    private String building;
    private int capacity;
    private int totalRows;
    private int totalColumns;
    private String[][] seatingGrid;
    private final List<Student> assignedStudents = new ArrayList<>();

    public ExamRoom(int roomNumber, String building, int capacity) {
        this.roomNumber = roomNumber;
        setBuilding(building);
        resizeGrid(capacity);
    }

    /** Recomputes the (near square) grid shape and drops every seat allocation. */
    private void resizeGrid(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.totalRows = (int) Math.ceil(Math.sqrt(this.capacity));
        this.totalColumns = (int) Math.ceil((double) this.capacity / totalRows);
        this.seatingGrid = new String[totalRows][totalColumns];
    }

    /** Runs {@code match} over the grid in reading order and returns the first hit, or null. */
    private int[] findSeat(Predicate<String> match) {
        for (int row = 0; row < totalRows; row++) {
            for (int col = 0; col < totalColumns; col++) {
                if (match.test(seatingGrid[row][col])) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    public boolean assignStudent(Student student) {
        if (student == null || isFull() || containsStudent(student)) {
            return false;
        }
        int[] seat = findSeat(occupant -> occupant == null);
        if (seat == null) {
            return false;
        }
        seatingGrid[seat[0]][seat[1]] = student.getStudentId();
        assignedStudents.add(student);
        return true;
    }

    public void removeStudent(String studentId) {
        if (studentId == null) {
            return;
        }
        int[] seat = findSeat(studentId::equals);
        if (seat != null) {
            seatingGrid[seat[0]][seat[1]] = null;
            assignedStudents.removeIf(student -> Strings.same(student.getStudentId(), studentId));
        }
    }

    public void clearSeating() {
        assignedStudents.clear();
        seatingGrid = new String[totalRows][totalColumns];
    }

    public boolean containsStudent(Student student) {
        return assignedStudents.stream().anyMatch(existing -> Strings.same(existing.getStudentId(), student.getStudentId()));
    }

    public String getSeatingChartAsString() {
        StringBuilder sb = new StringBuilder("\n").append(this).append("\n").append("-".repeat(40)).append("\n");
        for (String[] row : seatingGrid) {
            for (String occupant : row) {
                sb.append(occupant != null ? " [S] " : " [ ] ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = Strings.clean(building); }
    public int getCapacity() { return capacity; }
    /** Resizes the room and re-seats the students that still fit. */
    public void setCapacity(int capacity) {
        List<Student> existing = new ArrayList<>(assignedStudents);
        assignedStudents.clear();
        resizeGrid(capacity);
        existing.forEach(this::assignStudent);
    }

    public int getTotalRows() { return totalRows; }
    public int getTotalColumns() { return totalColumns; }
    public String[][] getSeatingGrid() { return seatingGrid; }
    public List<Student> getAssignedStudents() { return new ArrayList<>(assignedStudents); }
    public int getRemainingCapacity() { return Math.max(0, capacity - assignedStudents.size()); }
    public int getAvailableSeats() { return getRemainingCapacity(); }
    public boolean isFull() { return assignedStudents.size() >= capacity; }
    public boolean isOccupied() { return !assignedStudents.isEmpty(); }
    public boolean canHost(int studentCount) { return studentCount > 0 && studentCount <= getRemainingCapacity(); }

    @Override
    public String toString() {
        return String.format("Room %d | %s | Capacity: %d | Available: %d",
                roomNumber, building, capacity, getAvailableSeats());
    }
}
