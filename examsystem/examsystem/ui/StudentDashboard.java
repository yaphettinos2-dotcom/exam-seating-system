package examsystem.ui;

import examsystem.auth.UserSession;
import examsystem.model.ExamRoom;
import examsystem.model.ExamSchedule;
import examsystem.model.Student;
import examsystem.service.SeatingService;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** A deliberately read-only, student-focused view of the existing seating data. */
final class StudentDashboard extends JPanel {
    private static final Color NAVY = new Color(15, 39, 71);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color PALE_BLUE = new Color(239, 246, 255);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy · HH:mm");

    StudentDashboard(SeatingService service, UserSession session) {
        super(new BorderLayout(0, 20));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(28, 38, 34, 38));
        Student student = session.studentId() == null ? null : service.findStudentById(session.studentId());
        add(hero(student, session), BorderLayout.NORTH);
        add(content(service, student), BorderLayout.CENTER);
    }

    private JComponent hero(Student student, UserSession session) {
        JPanel hero = new JPanel(new BorderLayout(20, 0));
        hero.setBackground(NAVY); hero.setBorder(new EmptyBorder(25, 28, 25, 28));
        JLabel initials = new JLabel(initials(student == null ? session.displayName() : student.getName()), SwingConstants.CENTER);
        initials.setOpaque(true); initials.setBackground(BLUE); initials.setForeground(Color.WHITE); initials.setFont(new Font("Segoe UI", Font.BOLD, 25)); initials.setPreferredSize(new Dimension(68, 68));
        hero.add(initials, BorderLayout.WEST);
        String name = student == null ? session.displayName() : student.getName();
        String id = student == null ? "Student account" : "Student ID  " + student.getStudentId() + "   •   " + student.getDepartment().getName();
        JLabel welcome = new JLabel("<html><span style='font-size:25px; font-weight:700'>Hello, " + name + "</span><br><span style='font-size:13px; color:#bfdbfe'>" + id + "</span></html>");
        welcome.setForeground(Color.WHITE); hero.add(welcome, BorderLayout.CENTER);
        JLabel badge = new JLabel("STUDENT PORTAL", SwingConstants.CENTER); badge.setForeground(new Color(191, 219, 254)); badge.setFont(new Font("Segoe UI", Font.BOLD, 11)); hero.add(badge, BorderLayout.EAST);
        return hero;
    }

    private JComponent content(SeatingService service, Student student) {
        if (student == null) return empty("Your student profile could not be found. Please contact the examinations office.");
        List<ExamSchedule> exams = service.getAllSchedules().stream().filter(exam -> exam.containsStudent(student)).collect(Collectors.toList());
        JPanel body = new JPanel(new BorderLayout(0, 18)); body.setOpaque(false);
        JPanel summary = new JPanel(new GridLayout(1, 3, 16, 0)); summary.setOpaque(false);
        summary.add(metric("UPCOMING EXAMS", String.valueOf(exams.size()), "Your registered assessments"));
        long assigned = exams.stream().filter(exam -> exam.getAssignedRoom() != null).count();
        summary.add(metric("SEATING STATUS", assigned + " / " + exams.size(), "Exams with a room confirmed"));
        summary.add(metric("STUDENT NUMBER", student.getStudentId(), "Keep this ready on exam day"));
        body.add(summary, BorderLayout.NORTH);
        JPanel examsPanel = new JPanel(); examsPanel.setOpaque(false); examsPanel.setLayout(new BoxLayout(examsPanel, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Your examination timetable"); heading.setFont(new Font("Segoe UI", Font.BOLD, 21)); heading.setForeground(new Color(30, 41, 59)); heading.setAlignmentX(LEFT_ALIGNMENT); examsPanel.add(heading);
        JLabel subheading = new JLabel("View-only details issued by the examinations office."); subheading.setFont(new Font("Segoe UI", Font.PLAIN, 13)); subheading.setForeground(new Color(100, 116, 139)); subheading.setAlignmentX(LEFT_ALIGNMENT); examsPanel.add(subheading); examsPanel.add(Box.createVerticalStrut(14));
        if (exams.isEmpty()) examsPanel.add(empty("There are no examinations registered for you yet."));
        for (ExamSchedule exam : exams) { JComponent card = examCard(exam); card.setAlignmentX(LEFT_ALIGNMENT); examsPanel.add(card); examsPanel.add(Box.createVerticalStrut(12)); }
        JScrollPane scroll = new JScrollPane(examsPanel); scroll.setBorder(null); scroll.getViewport().setBackground(new Color(248, 250, 252)); scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); body.add(scroll, BorderLayout.CENTER);
        return body;
    }

    private JComponent examCard(ExamSchedule exam) {
        JPanel card = new JPanel(new BorderLayout(22, 0)); card.setBackground(Color.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)), new EmptyBorder(18, 20, 18, 20))); card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        JLabel date = new JLabel("<html><b style='font-size:14px; color:#1d4ed8'>" + exam.getDateTime().format(DateTimeFormatter.ofPattern("dd MMM")).toUpperCase() + "</b><br><span style='font-size:12px; color:#64748b'>" + exam.getDateTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "</span></html>", SwingConstants.CENTER); date.setPreferredSize(new Dimension(68, 60)); card.add(date, BorderLayout.WEST);
        ExamRoom room = exam.getAssignedRoom(); String place = room == null ? "Room allocation pending" : "Room " + room.getRoomNumber() + " · " + room.getBuilding();
        JLabel details = new JLabel("<html><b style='font-size:16px; color:#0f172a'>" + exam.getCourseCode() + " — " + exam.getCourseName() + "</b><br><span style='font-size:13px; color:#475569'>" + exam.getDateTime().format(DATE) + "  ·  " + exam.getDuration() + " minutes</span><br><span style='font-size:13px; color:#2563eb'>" + place + "</span></html>"); card.add(details, BorderLayout.CENTER);
        JLabel status = new JLabel(room == null ? "PENDING" : "CONFIRMED", SwingConstants.CENTER); status.setOpaque(true); status.setBackground(room == null ? new Color(254, 242, 242) : new Color(240, 253, 244)); status.setForeground(room == null ? new Color(185, 28, 28) : new Color(21, 128, 61)); status.setFont(new Font("Segoe UI", Font.BOLD, 11)); status.setPreferredSize(new Dimension(96, 34)); card.add(status, BorderLayout.EAST);
        return card;
    }

    private JComponent metric(String title, String value, String note) { JPanel p = new JPanel(); p.setBackground(title.equals("SEATING STATUS") ? PALE_BLUE : Color.WHITE); p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)), new EmptyBorder(16, 18, 16, 18))); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); JLabel a = new JLabel(title); a.setFont(new Font("Segoe UI", Font.BOLD, 11)); a.setForeground(new Color(100, 116, 139)); JLabel b = new JLabel(value); b.setFont(new Font("Segoe UI", Font.BOLD, 25)); b.setForeground(BLUE); JLabel c = new JLabel(note); c.setFont(new Font("Segoe UI", Font.PLAIN, 12)); c.setForeground(new Color(100, 116, 139)); p.add(a); p.add(Box.createVerticalStrut(6)); p.add(b); p.add(Box.createVerticalStrut(4)); p.add(c); return p; }
    private JComponent empty(String text) { JLabel label = new JLabel(text, SwingConstants.CENTER); label.setForeground(new Color(71, 85, 105)); label.setFont(new Font("Segoe UI", Font.PLAIN, 14)); JPanel p = new JPanel(new BorderLayout()); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(35, 20, 35, 20)); p.add(label); return p; }
    private static String initials(String name) { String[] parts = name.trim().split("\\s+"); return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "")).toUpperCase(); }
}
