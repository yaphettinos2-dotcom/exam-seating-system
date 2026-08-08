package examsystem.auth;

/** Replace this interface with a database-backed implementation when persistence is introduced. */
public interface AuthenticationProvider {
    UserSession authenticate(String username, char[] password);
}
