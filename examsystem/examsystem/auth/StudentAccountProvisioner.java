package examsystem.auth;

import examsystem.model.Student;

/** Optional capability for identity stores that create and revoke student portal accounts. */
public interface StudentAccountProvisioner {
    /** Creates a student portal account selected by an administrator. */
    boolean provisionStudentAccount(Student student, String username, char[] password);

    void revokeStudentAccount(String studentId);
}
