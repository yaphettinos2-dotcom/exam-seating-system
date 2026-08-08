package examsystem.model;

import examsystem.util.Strings;

import java.io.Serializable;
import java.util.Locale;

public class Department implements Serializable {
    private String code;
    private String name;
    private String description;

    public Department(String code, String name) {
        this(code, name, "");
    }

    public Department(String code, String name, String description) {
        setCode(code);
        setName(name);
        setDescription(description);
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = Strings.upper(code); }
    public String getName() { return name; }
    public void setName(String name) { this.name = Strings.clean(name); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = Strings.clean(description); }

    @Override
    public String toString() { return name + " (" + code + ")"; }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Department other && code.equalsIgnoreCase(other.code);
    }

    @Override
    public int hashCode() { return code.toLowerCase(Locale.ROOT).hashCode(); }
}
