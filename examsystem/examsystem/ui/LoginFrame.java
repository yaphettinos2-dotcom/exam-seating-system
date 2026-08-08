package examsystem.ui;

import examsystem.auth.AuthenticationProvider;
import examsystem.auth.UserSession;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.*;

/** Modern entry screen for the Exam Sitting System. */
public final class LoginFrame extends JFrame {
    private static final Color NAVY = new Color(15, 39, 71);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color INK = new Color(31, 41, 55);
    private final AuthenticationProvider authentication;
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel message = new JLabel(" ");
    private final JButton login = new JButton("Sign in");
    private final JProgressBar loading = new JProgressBar();
    private final JCheckBox remember = new JCheckBox("Remember me on this device");
    private final Preferences preferences = Preferences.userNodeForPackage(LoginFrame.class);

    public LoginFrame(AuthenticationProvider authentication) {
        this.authentication = authentication;
        setTitle("Exam Sitting System — Sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(880, 590));
        setSize(960, 620);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(login);
        username.setText(preferences.get("rememberedUsername", ""));
        remember.setSelected(!username.getText().isBlank());
        SwingUtilities.invokeLater(() -> (username.getText().isBlank() ? username : password).requestFocusInWindow());
    }

    private JComponent buildContent() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(Color.WHITE);
        root.add(buildBrandPanel());
        root.add(buildFormPanel());
        return root;
    }

    private JComponent buildBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(NAVY);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.insets = new Insets(8, 48, 8, 48); c.anchor = GridBagConstraints.WEST;
        JLabel crest = new JLabel("U");
        crest.setHorizontalAlignment(SwingConstants.CENTER);
        crest.setOpaque(true); crest.setBackground(new Color(255, 255, 255, 32)); crest.setForeground(Color.WHITE);
        crest.setFont(new Font("Segoe UI", Font.BOLD, 42)); crest.setPreferredSize(new Dimension(86, 86));
        crest.setBorder(new RoundedBorder(new Color(255, 255, 255, 100), 43));
        c.gridy = 0; panel.add(crest, c);
        JLabel university = label("UNIVERSITY EXAMINATIONS OFFICE", 12, Font.BOLD, new Color(191, 219, 254));
        c.gridy = 1; panel.add(university, c);
        JLabel title = label("Exam Sitting\nSystem", 35, Font.BOLD, Color.WHITE);
        c.gridy = 2; panel.add(title, c);
        JLabel copy = label("Plan, manage, and deliver examination\nseating with clarity and confidence.", 15, Font.PLAIN, new Color(219, 234, 254));
        c.gridy = 3; panel.add(copy, c);
        JLabel note = label("Secure access  •  Academic operations", 12, Font.PLAIN, new Color(147, 197, 253));
        c.gridy = 4; c.insets = new Insets(44, 48, 8, 48); panel.add(note, c);
        return panel;
    }

    private JComponent buildFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout()); wrapper.setBackground(new Color(248, 250, 252));
        JPanel form = new JPanel(); form.setOpaque(false); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(350, 440));
        form.add(label("Welcome back", 28, Font.BOLD, INK));
        form.add(Box.createVerticalStrut(7)); form.add(label("Sign in to manage examination seating.", 14, Font.PLAIN, new Color(100, 116, 139)));
        form.add(Box.createVerticalStrut(32)); form.add(fieldLabel("USERNAME")); form.add(Box.createVerticalStrut(7)); form.add(styleField(username));
        form.add(Box.createVerticalStrut(18)); form.add(fieldLabel("PASSWORD")); form.add(Box.createVerticalStrut(7));
        JPanel passwordRow = new JPanel(new BorderLayout(4, 0)); passwordRow.setOpaque(false); passwordRow.add(styleField(password), BorderLayout.CENTER);
        JButton show = flatButton("Show"); show.setPreferredSize(new Dimension(62, 40)); show.addActionListener(e -> togglePassword(show)); passwordRow.add(show, BorderLayout.EAST); form.add(passwordRow);
        form.add(Box.createVerticalStrut(9)); message.setFont(new Font("Segoe UI", Font.PLAIN, 12)); message.setForeground(new Color(185, 28, 28)); form.add(message);
        JPanel options = new JPanel(new BorderLayout()); options.setOpaque(false); remember.setOpaque(false); remember.setFont(new Font("Segoe UI", Font.PLAIN, 12)); options.add(remember, BorderLayout.WEST);
        JButton forgot = linkButton("Forgot password?"); forgot.addActionListener(e -> setMessage("Please contact your system administrator.", false)); options.add(forgot, BorderLayout.EAST); form.add(options);
        form.add(Box.createVerticalStrut(21)); stylePrimary(login); login.addActionListener(e -> authenticate()); form.add(login);
        form.add(Box.createVerticalStrut(11)); JButton clear = flatButton("Clear fields"); clear.addActionListener(e -> { username.setText(""); password.setText(""); setMessage(" ", false); username.requestFocusInWindow(); }); form.add(clear);
        form.add(Box.createVerticalStrut(12)); loading.setIndeterminate(true); loading.setVisible(false); loading.setBorderPainted(false); form.add(loading);
        form.add(Box.createVerticalGlue()); JButton exit = linkButton("Exit application"); exit.setAlignmentX(Component.CENTER_ALIGNMENT); exit.addActionListener(e -> dispose()); form.add(exit);
        wrapper.add(form); return wrapper;
    }

    private void authenticate() {
        if (username.getText().trim().isEmpty() || password.getPassword().length == 0) { setMessage("Enter your username and password.", true); return; }
        login.setEnabled(false); loading.setVisible(true); setMessage("Authenticating…", false);
        Timer timer = new Timer(300, null); timer.setRepeats(false); timer.addActionListener(e -> {
            char[] secret = password.getPassword(); UserSession session;
            try { session = authentication.authenticate(username.getText(), secret); } finally { Arrays.fill(secret, '\0'); }
            loading.setVisible(false); login.setEnabled(true);
            if (session == null) { setMessage("We couldn't verify those credentials.", true); password.setText(""); password.requestFocusInWindow(); return; }
            if (remember.isSelected()) preferences.put("rememberedUsername", session.username()); else preferences.remove("rememberedUsername");
            new MainFrame(session, authentication).setVisible(true); dispose();
        }); timer.start();
    }

    private void togglePassword(JButton button) { boolean hidden = password.getEchoChar() != (char) 0; password.setEchoChar(hidden ? (char) 0 : '•'); button.setText(hidden ? "Hide" : "Show"); }
    private void setMessage(String text, boolean error) { message.setForeground(error ? new Color(185, 28, 28) : new Color(71, 85, 105)); message.setText(text); }
    private static JLabel label(String text, int size, int style, Color color) { JLabel value = new JLabel("<html>" + text.replace("\n", "<br>") + "</html>"); value.setFont(new Font("Segoe UI", style, size)); value.setForeground(color); return value; }
    private static JLabel fieldLabel(String text) { return label(text, 11, Font.BOLD, new Color(71, 85, 105)); }
    private static JTextField styleField(JTextField field) { field.setFont(new Font("Segoe UI", Font.PLAIN, 15)); field.setPreferredSize(new Dimension(300, 42)); field.setBorder(new CompoundBorder(new RoundedBorder(new Color(203, 213, 225), 10), new EmptyBorder(0, 12, 0, 12))); field.setBackground(Color.WHITE); return field; }
    private static JButton flatButton(String text) { JButton button = new JButton(text); button.setFont(new Font("Segoe UI", Font.BOLD, 13)); button.setFocusPainted(false); button.setForeground(BLUE); button.setBackground(Color.WHITE); button.setBorder(new RoundedBorder(new Color(203, 213, 225), 10)); return button; }
    private static JButton linkButton(String text) { JButton button = new JButton(text); button.setBorderPainted(false); button.setContentAreaFilled(false); button.setFocusPainted(false); button.setForeground(BLUE); button.setFont(new Font("Segoe UI", Font.PLAIN, 12)); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return button; }
    private static void stylePrimary(JButton button) { button.setFont(new Font("Segoe UI", Font.BOLD, 14)); button.setForeground(Color.WHITE); button.setBackground(BLUE); button.setFocusPainted(false); button.setBorder(new RoundedBorder(BLUE, 10)); button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); button.setPreferredSize(new Dimension(350, 44)); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
    private static final class RoundedBorder extends AbstractBorder { private final Color color; private final int radius; RoundedBorder(Color color, int radius) { this.color = color; this.radius = radius; } public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius); g2.dispose(); } public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); } }
}
