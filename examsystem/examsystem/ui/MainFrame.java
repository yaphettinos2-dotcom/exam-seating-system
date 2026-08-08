package examsystem.ui;

import examsystem.auth.AuthenticationProvider;
import examsystem.auth.AuthenticationService;
import examsystem.auth.StudentAccountProvisioner;
import examsystem.auth.UserSession;
import examsystem.model.Department;
import examsystem.model.ExamRoom;
import examsystem.model.ExamSchedule;
import examsystem.model.Student;
import examsystem.service.SeatingService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainFrame extends JFrame {
    private static final Color APP_BACKGROUND = new Color(244, 247, 251);
    private static final Color SURFACE = Color.WHITE;
    private static final Color ACCENT = new Color(37, 99, 235);
    private static final Color BORDER = new Color(220, 227, 238);
    private static final DateTimeFormatter DATE_TIME = ExamSchedule.FORMAT;

    private final SeatingService service = new SeatingService();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final UserSession session;
    private final AuthenticationProvider authentication;

    private CrudTab departments;
    private CrudTab students;
    private CrudTab rooms;
    private CrudTab schedules;

    private JTextArea seatingDisplayArea;
    private JTextArea reportArea;
    private final JLabel[] metrics = new JLabel[5];

    public MainFrame(UserSession session, AuthenticationProvider authentication) {
        this.session = session;
        this.authentication = authentication;
        configureAppearance();
        setTitle("Exam Sitting System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 820);
        setMinimumSize(new Dimension(980, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(APP_BACKGROUND);

        // Students need only their own read-only portal. Do not construct the management UI first.
        if (session.isStudent()) {
            seedSampleData();
            setJMenuBar(buildMenuBar());
            add(new StudentDashboard(service, session));
            return;
        }

        departments = buildCrudTab("Department Details", "Save Department",
                new String[]{"Code:", "Name:", "Description:"},
                new String[]{"Code", "Name", "Description"},
                this::editDepartment, this::saveDepartment, this::deleteDepartment, this::refreshDepartmentTable, null);
        students = buildCrudTab("Student Details", "Add Student",
                new String[]{"Name:", "Email:", "Student ID:", "Department Code:", "Room Number:", "Portal Username:", "Portal Password:"},
                new String[]{"ID", "Name", "Email", "Student ID", "Department"},
                this::editStudent, this::saveStudent, this::deleteStudent, this::refreshStudentTable, null);
        students.fields[4].setToolTipText("Choose an existing room number to reserve a seat immediately.");
        students.fields[5].setToolTipText("The student will use this username to access the student portal.");
        students.fields[6].setToolTipText("Use at least 4 characters. This field is only used when creating an account.");
        rooms = buildCrudTab("Room Details", "Save Room",
                new String[]{"Room Number:", "Building:", "Capacity:"},
                new String[]{"Room #", "Building", "Capacity", "Used", "Remaining", "Status"},
                this::editRoom, this::saveRoom, this::deleteRoom, this::refreshRoomTable, null);
        schedules = buildCrudTab("Schedule Details", "Save Schedule",
                new String[]{"Course Code:", "Course Name:", "Date/Time (yyyy-MM-dd HH:mm):", "Duration (minutes):"},
                new String[]{"Course Code", "Course Name", "Date/Time", "Duration", "Students", "Room"},
                this::editSchedule, this::saveSchedule, this::deleteSchedule, this::refreshScheduleTable,
                new JButton[]{primary(button("Generate Seating", this::generateSeating)),
                        primary(button("Enroll Student", this::enrollStudent))});

        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(APP_BACKGROUND);
        tabbedPane.addTab("Departments", departments.panel);
        tabbedPane.addTab("Students", students.panel);
        tabbedPane.addTab("Rooms", rooms.panel);
        tabbedPane.addTab("Schedules", schedules.panel);
        tabbedPane.addTab("Seating Charts", createSeatingPanel());
        tabbedPane.addTab("Reports", createReportPanel());
        tabbedPane.addChangeListener(event -> refreshVisibleData());

        addSampleData();
        setJMenuBar(buildMenuBar());
        add(tabbedPane);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        JLabel brand = new JLabel("  EXAM SITTING SYSTEM");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 14));
        brand.setForeground(new Color(30, 64, 175));
        menuBar.add(brand);
        menuBar.add(Box.createHorizontalGlue());
        JLabel identity = new JLabel(session.displayName() + "  ·  " + session.role());
        identity.setForeground(new Color(71, 85, 105));
        identity.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        menuBar.add(identity);
        JButton logout = button("Log out", this::logout);
        logout.setMargin(new Insets(4, 10, 4, 10));
        menuBar.add(Box.createHorizontalStrut(12));
        menuBar.add(logout);
        return menuBar;
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "End your current session?", "Log out", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            LoginFrame login = new LoginFrame(authentication);
            login.setVisible(true);
        }
    }

    // ============ GENERIC UI BUILDERS ============
    private JPanel basePanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(APP_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JButton primary(JButton button) {
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        return button;
    }

    private JPanel flow(int alignment, JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(alignment));
        for (JComponent component : components) {
            panel.add(component);
        }
        return panel;
    }

    /**
     * Builds a complete management tab: labelled form, action buttons, search box and results table.
     */
    private CrudTab buildCrudTab(String title, String saveText, String[] labels, String[] columns,
                                 Runnable onEdit, Runnable onSave, Runnable onDelete, Runnable onRefresh,
                                 JButton[] extraFooterButtons) {
        JPanel inputPanel = new JPanel(new GridLayout(labels.length, 2, 10, 5));
        JTextField[] fields = new JTextField[labels.length];
        for (int index = 0; index < labels.length; index++) {
            fields[index] = labels[index].contains("Password") ? new JPasswordField() : new JTextField();
            inputPanel.add(new JLabel(labels[index]));
            inputPanel.add(fields[index]);
        }

        CrudTab[] holder = new CrudTab[1];
        JPanel actions = flow(FlowLayout.LEFT,
                button("New", () -> holder[0].clear()),
                button("Edit", onEdit),
                primary(button(saveText, onSave)),
                button("Cancel", () -> holder[0].clear()),
                button("Delete", onDelete));

        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.setBorder(BorderFactory.createTitledBorder(title));
        form.add(inputPanel, BorderLayout.CENTER);
        form.add(actions, BorderLayout.SOUTH);

        JTextField searchField = new JTextField();
        JPanel search = new JPanel(new BorderLayout(5, 0));
        search.add(searchField, BorderLayout.CENTER);
        search.add(button("Search", onRefresh), BorderLayout.EAST);

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        styleTable(table);

        JPanel footer = flow(FlowLayout.RIGHT, button("Refresh", onRefresh));
        if (extraFooterButtons != null) {
            for (JButton extra : extraFooterButtons) {
                footer.add(extra);
            }
        }

        JPanel results = new JPanel(new BorderLayout(0, 8));
        results.add(search, BorderLayout.NORTH);
        results.add(new JScrollPane(table), BorderLayout.CENTER);
        results.add(footer, BorderLayout.SOUTH);

        JPanel panel = basePanel(new BorderLayout(10, 10));
        panel.add(form, BorderLayout.NORTH);
        panel.add(results, BorderLayout.CENTER);

        holder[0] = new CrudTab(panel, fields, searchField, model, table);
        return holder[0];
    }

    private JTextArea createTextArea(Font font) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(font);
        area.setBackground(SURFACE);
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return area;
    }

    private JScrollPane titledScroll(String title, Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createTitledBorder(title));
        return scroll;
    }

    private JPanel createSeatingPanel() {
        seatingDisplayArea = createTextArea(new Font("Monospaced", Font.PLAIN, 13));
        JPanel panel = basePanel(new BorderLayout(10, 10));
        panel.add(flow(FlowLayout.RIGHT,
                button("Show Seating Charts", this::displaySeatingCharts),
                button("Clear", () -> seatingDisplayArea.setText(""))), BorderLayout.NORTH);
        panel.add(titledScroll("Seating Charts", seatingDisplayArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReportPanel() {
        String[] titles = {"Departments", "Students", "Rooms", "Exams", "Enrollments"};
        JPanel metricsPanel = new JPanel(new GridLayout(1, titles.length, 10, 0));
        metricsPanel.setBackground(APP_BACKGROUND);
        for (int index = 0; index < titles.length; index++) {
            metrics[index] = new JLabel("0");
            metricsPanel.add(createMetricCard(titles[index], metrics[index]));
        }

        reportArea = createTextArea(new Font("Segoe UI", Font.PLAIN, 14));
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setBackground(APP_BACKGROUND);
        header.add(metricsPanel, BorderLayout.CENTER);
        header.add(flow(FlowLayout.RIGHT,
                primary(button("Generate Report", this::refreshReport)),
                button("Clear", () -> reportArea.setText(""))), BorderLayout.SOUTH);

        JPanel panel = basePanel(new BorderLayout(10, 10));
        panel.add(header, BorderLayout.NORTH);
        panel.add(titledScroll("Operational summary", reportArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(ACCENT);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void configureAppearance() {
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Panel.background", APP_BACKGROUND);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
    }

    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(new Color(30, 41, 59));
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(51, 65, 85));
        table.getTableHeader().setReorderingAllowed(false);
    }

    // ============ SHARED CRUD FLOW ============
    /** Loads the record selected in {@code tab} into its form, or reports that nothing is selected. */
    private <T> T beginEdit(CrudTab tab, int keyColumn, String entity, Function<Object, T> lookup) {
        Object key = tab.selectedValue(keyColumn);
        if (key == null) {
            showError("Please select a " + entity + " to edit.");
            return null;
        }
        return lookup.apply(key);
    }

    /** Asks for confirmation, deletes the selected record and refreshes the affected tables. */
    private void deleteSelected(CrudTab tab, int keyColumn, String entity,
                                Function<Object, Boolean> deleter, String failure, Runnable... refreshers) {
        Object key = tab.selectedValue(keyColumn);
        if (key == null) {
            showError("Please select a " + entity + " to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this " + entity + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        if (!deleter.apply(key)) {
            showError(failure);
            return;
        }
        for (Runnable refresher : refreshers) {
            refresher.run();
        }
        showInfo(Character.toUpperCase(entity.charAt(0)) + entity.substring(1) + " deleted.");
    }

    /** Applies a create-or-update outcome: on success clears the form, refreshes and confirms. */
    private void finishSave(CrudTab tab, boolean succeeded, String failure, String entity, Runnable... refreshers) {
        if (!succeeded) {
            showError(failure);
            return;
        }
        tab.clear();
        for (Runnable refresher : refreshers) {
            refresher.run();
        }
        showInfo(entity + " saved successfully.");
    }

    private void refreshVisibleData() {
        refreshDepartmentTable();
        refreshStudentTable();
        refreshRoomTable();
        refreshScheduleTable();
        refreshReport();
    }

    // ============ DEPARTMENTS ============
    private void editDepartment() {
        Department department = beginEdit(departments, 0, "department", key -> service.findDepartmentByCode(key.toString()));
        if (department != null) {
            departments.editingKey = department.getCode();
            departments.setTexts(department.getCode(), department.getName(), department.getDescription());
        }
    }

    private void saveDepartment() {
        String code = departments.text(0);
        String name = departments.text(1);
        if (code.isEmpty() || name.isEmpty()) {
            showError("Department code and name are required.");
            return;
        }
        boolean creating = departments.editingKey == null;
        boolean saved = creating
                ? service.addDepartment(code, name)
                : service.updateDepartment(departments.editingKey.toString(), code, name, departments.text(2));
        finishSave(departments, saved, creating
                        ? "Department already exists or the input is invalid."
                        : "Unable to update the department. Check the code and try again.",
                "Department", this::refreshDepartmentTable);
    }

    private void deleteDepartment() {
        deleteSelected(departments, 0, "department", key -> service.deleteDepartment(key.toString()),
                "This department is currently in use by students and cannot be deleted.", this::refreshDepartmentTable);
    }

    private void refreshDepartmentTable() {
        departments.setRows(rowsOf(service.searchDepartments(departments.search.getText()),
                department -> new Object[]{department.getCode(), department.getName(), department.getDescription()}));
    }

    // ============ STUDENTS ============
    private void editStudent() {
        Student student = beginEdit(students, 3, "student", key -> service.findStudentById(key.toString()));
        if (student != null) {
            students.editingKey = student.getStudentId();
            students.setTexts(student.getName(), student.getEmail(), student.getStudentId(),
                    student.getDepartment() != null ? student.getDepartment().getCode() : "",
                    student.getAssignedRoomNumber() != null ? String.valueOf(student.getAssignedRoomNumber()) : "", "", "");
        }
    }

    private void saveStudent() {
        String name = students.text(0);
        String email = students.text(1);
        String studentId = students.text(2);
        String departmentCode = students.text(3);
        if (name.isEmpty() || email.isEmpty() || studentId.isEmpty() || departmentCode.isEmpty()) {
            showError("All student fields are required.");
            return;
        }
        Integer roomNumber;
        try {
            roomNumber = students.text(4).isEmpty() ? null : Integer.valueOf(students.text(4));
        } catch (NumberFormatException ex) {
            showError("Room number must be a valid integer.");
            return;
        }
        boolean creating = students.editingKey == null;
        String portalUsername = students.text(5);
        char[] portalPassword = students.fields[6] instanceof JPasswordField passwordField
                ? passwordField.getPassword() : students.fields[6].getText().toCharArray();
        boolean portalDetailsProvided = !portalUsername.isEmpty() && portalPassword.length >= 4;
        if (creating && !portalDetailsProvided) {
            Arrays.fill(portalPassword, '\0');
            showError("Portal username and a password of at least 4 characters are required for a new student.");
            return;
        }
        boolean saved = creating
                ? service.addStudent(name, email, studentId, departmentCode, roomNumber)
                : service.updateStudent(students.editingKey.toString(), name, email, departmentCode, roomNumber);
        if (saved && portalDetailsProvided) {
            provisionStudentAccount(service.findStudentById(studentId), portalUsername, portalPassword);
        }
        Arrays.fill(portalPassword, '\0');
        finishSave(students, saved, creating
                        ? "Unable to add student. Check for duplicate ID, invalid department code, or room selection."
                        : "Unable to update student. Check the department code and try again.",
                "Student", this::refreshStudentTable, this::refreshRoomTable, this::refreshScheduleTable);
    }

    private void deleteStudent() {
        deleteSelected(students, 3, "student", key -> {
                    boolean deleted = service.deleteStudent(key.toString());
                    if (deleted && authentication instanceof StudentAccountProvisioner provisioner) {
                        provisioner.revokeStudentAccount(key.toString());
                    }
                    return deleted;
                },
                "Unable to delete student.", this::refreshStudentTable, this::refreshRoomTable, this::refreshScheduleTable);
    }

    private void provisionStudentAccount(Student student, String username, char[] password) {
        if (!(authentication instanceof StudentAccountProvisioner provisioner) || student == null) {
            return;
        }
        if (!provisioner.provisionStudentAccount(student, username, password)) {
            showInfo("Student saved, but the portal account was not created. Choose a different username and edit the student to try again.");
        }
    }

    private void refreshStudentTable() {
        students.setRows(rowsOf(service.searchStudents(students.search.getText()),
                student -> new Object[]{student.getId(), student.getName(), student.getEmail(), student.getStudentId(),
                        student.getDepartment() != null ? student.getDepartment().getCode() : "None"}));
    }

    // ============ ROOMS ============
    private void editRoom() {
        ExamRoom room = beginEdit(rooms, 0, "room", key -> service.findRoom((int) key));
        if (room != null) {
            rooms.editingKey = room.getRoomNumber();
            rooms.setTexts(String.valueOf(room.getRoomNumber()), room.getBuilding(), String.valueOf(room.getCapacity()));
        }
    }

    private void saveRoom() {
        try {
            int roomNumber = Integer.parseInt(rooms.text(0));
            String building = rooms.text(1);
            int capacity = Integer.parseInt(rooms.text(2));
            if (building.isEmpty()) {
                showError("Building name is required.");
                return;
            }
            boolean creating = rooms.editingKey == null;
            boolean saved = creating
                    ? service.addRoom(roomNumber, building, capacity)
                    : service.updateRoom((Integer) rooms.editingKey, roomNumber, building, capacity);
            finishSave(rooms, saved, creating
                            ? "Room already exists or the capacity is invalid."
                            : "Unable to update room. Check for duplicate room number or insufficient capacity.",
                    "Room", this::refreshRoomTable, this::refreshScheduleTable);
        } catch (NumberFormatException ex) {
            showError("Room number and capacity must be valid integers.");
        }
    }

    private void deleteRoom() {
        deleteSelected(rooms, 0, "room", key -> service.deleteRoom((int) key),
                "Unable to delete the room.", this::refreshRoomTable, this::refreshScheduleTable);
    }

    private void refreshRoomTable() {
        rooms.setRows(rowsOf(service.searchRooms(rooms.search.getText()), room -> {
            int used = room.getAssignedStudents().size();
            String status = room.isFull() ? "Full" : used > 0 ? "Occupied" : "Available";
            return new Object[]{room.getRoomNumber(), room.getBuilding(), room.getCapacity(), used,
                    room.getRemainingCapacity(), status};
        }));
    }

    // ============ SCHEDULES ============
    private void editSchedule() {
        ExamSchedule schedule = beginEdit(schedules, 0, "schedule", key -> service.findSchedule(key.toString()));
        if (schedule != null) {
            schedules.editingKey = schedule.getCourseCode();
            schedules.setTexts(schedule.getCourseCode(), schedule.getCourseName(),
                    schedule.getDateTime().format(DATE_TIME), String.valueOf(schedule.getDuration()));
        }
    }

    private void saveSchedule() {
        String code = schedules.text(0);
        String name = schedules.text(1);
        String dateTimeText = schedules.text(2);
        String durationText = schedules.text(3);
        if (code.isEmpty() || name.isEmpty() || dateTimeText.isEmpty() || durationText.isEmpty()) {
            showError("All schedule fields are required.");
            return;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeText, DATE_TIME);
            int duration = Integer.parseInt(durationText);
            boolean creating = schedules.editingKey == null;
            boolean saved = creating
                    ? service.createSchedule(code, name, dateTime, duration)
                    : service.updateSchedule(schedules.editingKey.toString(), code, name, dateTime, duration);
            finishSave(schedules, saved, creating
                            ? "Schedule already exists or the input is invalid."
                            : "Unable to update schedule. Check the course code and try again.",
                    "Schedule", this::refreshScheduleTable);
        } catch (NumberFormatException ex) {
            showError("Duration must be a valid whole number.");
        } catch (DateTimeParseException ex) {
            showError("Please enter the date and time in yyyy-MM-dd HH:mm format.");
        }
    }

    private void deleteSchedule() {
        deleteSelected(schedules, 0, "schedule", key -> service.deleteSchedule(key.toString()),
                "Unable to delete the schedule.", this::refreshScheduleTable);
    }

    private void refreshScheduleTable() {
        schedules.setRows(rowsOf(service.searchSchedules(schedules.search.getText()),
                schedule -> new Object[]{schedule.getCourseCode(), schedule.getCourseName(),
                        schedule.getDateTime().format(DATE_TIME), schedule.getDuration(), schedule.getStudentCount(),
                        schedule.getAssignedRoom() != null ? schedule.getAssignedRoom().getRoomNumber() : "None"}));
    }

    private static <T> List<Object[]> rowsOf(List<T> items, Function<T, Object[]> mapper) {
        return items.stream().map(mapper).collect(Collectors.toCollection(ArrayList::new));
    }

    // ============ SEATING AND REPORTS ============
    private void generateSeating() {
        if (service.getAllSchedules().isEmpty()) {
            showInfo("No schedules available to seat yet.");
            return;
        }
        service.generateSeatingForAllExams();
        refreshScheduleTable();
        refreshRoomTable();
        displaySeatingCharts();
        showInfo("Seating generated successfully.");
    }

    private void enrollStudent() {
        if (service.getAllStudents().isEmpty() || service.getAllSchedules().isEmpty()) {
            showError("Add at least one student and one schedule before enrolling.");
            return;
        }
        JComboBox<Student> studentInput = new JComboBox<>(service.getAllStudents().toArray(new Student[0]));
        JComboBox<ExamSchedule> courseInput = new JComboBox<>(service.getAllSchedules().toArray(new ExamSchedule[0]));
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(new JLabel("Student:"));
        form.add(studentInput);
        form.add(new JLabel("Exam:"));
        form.add(courseInput);

        int result = JOptionPane.showConfirmDialog(this, form, "Enroll Student in an Exam",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        Student student = (Student) studentInput.getSelectedItem();
        ExamSchedule schedule = (ExamSchedule) courseInput.getSelectedItem();
        if (student == null || schedule == null
                || !service.enrollStudentInExam(student.getStudentId(), schedule.getCourseCode())) {
            showError("This student is already enrolled in the selected exam.");
            return;
        }
        refreshScheduleTable();
        refreshRoomTable();
        refreshReport();
        displaySeatingCharts();
        showInfo("Student enrolled and room availability updated.");
    }

    private void displaySeatingCharts() {
        StringBuilder sb = new StringBuilder("EXAM SEATING PLAN\n")
                .append("=".repeat(76)).append("\n")
                .append("Each exam is assigned its own room. [S] = occupied seat, [ ] = available seat.\n\n");
        for (ExamSchedule schedule : service.getAllSchedules()) {
            sb.append(schedule.getCourseCode()).append("  |  ").append(schedule.getCourseName()).append("\n")
                    .append("Time: ").append(schedule.getDateTime().format(DATE_TIME))
                    .append("  \u2022  Students: ").append(schedule.getStudentCount());
            ExamRoom room = schedule.getAssignedRoom();
            if (room == null) {
                sb.append("  \u2022  ROOM NOT ASSIGNED\n");
            } else {
                sb.append("  \u2022  Room: ").append(room.getRoomNumber())
                        .append("  \u2022  Free seats: ").append(room.getRemainingCapacity()).append("\n")
                        .append(room.getSeatingChartAsString());
            }
            sb.append("\n").append("-".repeat(76)).append("\n\n");
        }
        seatingDisplayArea.setText(sb.toString());
        seatingDisplayArea.setCaretPosition(0);
    }

    private void refreshReport() {
        if (reportArea == null) {
            return;
        }
        int[] values = {service.getTotalDepartments(), service.getTotalStudents(), service.getTotalRooms(),
                service.getTotalSchedules(), service.getTotalAssignedStudents()};
        for (int index = 0; index < metrics.length; index++) {
            metrics[index].setText(String.valueOf(values[index]));
        }

        long occupiedRooms = service.getAllRooms().stream().filter(ExamRoom::isOccupied).count();
        long unassignedExams = service.getAllSchedules().stream()
                .filter(schedule -> schedule.getStudentCount() > 0 && schedule.getAssignedRoom() == null).count();

        reportArea.setText("Seating readiness\n\n"
                + occupiedRooms + " of " + service.getTotalRooms() + " rooms currently have assigned seats.\n"
                + (unassignedExams == 0
                ? "All exams with enrolled students have a room assignment."
                : unassignedExams + " exam(s) still need a room with enough capacity.")
                + "\n\nUse the Schedules tab to enroll students, then select Generate Seating to rebuild the plan.");
        reportArea.setCaretPosition(0);
    }

    private void addSampleData() {
        seedSampleData();
        refreshVisibleData();
        displaySeatingCharts();
    }

    /** Loads the demonstration domain data without touching administrator widgets. */
    private void seedSampleData() {
        String[][] sampleDepartments = {{"SC", "Science"}, {"ENG", "Engineering"}, {"MATH", "Mathematics"}};
        for (String[] department : sampleDepartments) {
            service.addDepartment(department[0], department[1]);
        }
        String[][] sampleStudents = {
                {"Alice Johnson", "alice@email.com", "S001", "SC"}, {"Bob Smith", "bob@email.com", "S002", "ENG"},
                {"Charlie Brown", "charlie@email.com", "S003", "MATH"}, {"Diana Prince", "diana@email.com", "S004", "SC"},
                {"Ethan Hunt", "ethan@email.com", "S005", "ENG"}, {"Fiona Apple", "fiona@email.com", "S006", "MATH"}};
        for (String[] student : sampleStudents) {
            service.addStudent(student[0], student[1], student[2], student[3]);
        }
        Object[][] sampleRooms = {{101, "Science Building", 30}, {102, "Science Building", 25},
                {201, "Engineering Building", 40}, {202, "Engineering Building", 20}};
        for (Object[] room : sampleRooms) {
            service.addRoom((int) room[0], (String) room[1], (int) room[2]);
        }
        Object[][] sampleSchedules = {{"CS101", "Intro to Programming", 2, 120}, {"CS201", "Data Structures", 4, 90},
                {"MATH101", "Calculus I", 6, 150}};
        for (Object[] schedule : sampleSchedules) {
            service.createSchedule((String) schedule[0], (String) schedule[1],
                    LocalDateTime.now().plusDays((int) schedule[2]), (int) schedule[3]);
        }
        String[][] enrollments = {{"S001", "CS101"}, {"S002", "CS101"}, {"S003", "CS101"}, {"S004", "CS201"},
                {"S005", "CS201"}, {"S006", "MATH101"}, {"S001", "MATH101"}, {"S003", "MATH101"}};
        for (String[] enrollment : enrollments) {
            service.enrollStudentInExam(enrollment[0], enrollment[1]);
        }
        service.generateSeatingForAllExams();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "System Message", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame(new AuthenticationService()).setVisible(true));
    }
}
