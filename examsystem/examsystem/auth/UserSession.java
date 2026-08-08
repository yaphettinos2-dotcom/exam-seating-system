package examsystem.auth;

/** Immutable identity for the current application session. */
public record UserSession(String username, String displayName, Role role, String studentId) {
    public enum Role { ADMIN, STUDENT }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }
}
