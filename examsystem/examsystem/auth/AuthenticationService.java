package examsystem.auth;

import examsystem.model.Student;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** In-memory authentication provider. Passwords are never stored in plain text. */
public final class AuthenticationService implements AuthenticationProvider, StudentAccountProvisioner {
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    private final Map<String, Account> accounts = new HashMap<>();

    public AuthenticationService() {
        register("admin", "Admin User", "admin123", UserSession.Role.ADMIN, null);
        // Built-in demonstration records are available immediately, before any user has signed in.
        register("alice", "Alice Johnson", "alice123", UserSession.Role.STUDENT, "S001");
        register("bob", "Bob Smith", "bob123", UserSession.Role.STUDENT, "S002");
        register("charlie", "Charlie Brown", "charlie123", UserSession.Role.STUDENT, "S003");
        register("diana", "Diana Prince", "diana123", UserSession.Role.STUDENT, "S004");
        register("ethan", "Ethan Hunt", "ethan123", UserSession.Role.STUDENT, "S005");
        register("fiona", "Fiona Apple", "fiona123", UserSession.Role.STUDENT, "S006");
    }

    private void register(String username, String name, String password, UserSession.Role role, String studentId) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        char[] secret = password.toCharArray();
        try {
            accounts.put(username, new Account(name, role, studentId, salt, hash(secret, salt)));
        } finally {
            Arrays.fill(secret, '\0');
        }
    }

    @Override
    public boolean provisionStudentAccount(Student student, String username, char[] password) {
        if (student == null || username == null || username.isBlank() || password == null || password.length < 4
                || student.getStudentId() == null) {
            return false;
        }
        String cleanUsername = username.trim().toLowerCase(Locale.ROOT);
        if (accounts.containsKey(cleanUsername)) {
            return false;
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        accounts.put(cleanUsername, new Account(student.getName(), UserSession.Role.STUDENT, student.getStudentId(), salt, hash(password, salt)));
        return true;
    }

    @Override
    public void revokeStudentAccount(String studentId) {
        accounts.entrySet().removeIf(entry -> studentId != null && studentId.equalsIgnoreCase(entry.getValue().studentId));
    }

    @Override
    public UserSession authenticate(String username, char[] password) {
        if (username == null || password == null) return null;
        Account account = accounts.get(username.trim().toLowerCase(Locale.ROOT));
        if (account == null) return null;
        byte[] candidate = hash(password, account.salt);
        try {
            return java.security.MessageDigest.isEqual(candidate, account.passwordHash)
                    ? new UserSession(username.trim(), account.name, account.role, account.studentId) : null;
        } finally {
            Arrays.fill(candidate, (byte) 0);
        }
    }

    private static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Password security is unavailable", error);
        } finally {
            spec.clearPassword();
        }
    }

    private record Account(String name, UserSession.Role role, String studentId, byte[] salt, byte[] passwordHash) { }
}
