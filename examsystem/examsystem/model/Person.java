package examsystem.model;

import examsystem.util.Strings;

public abstract class Person {
    private static int nextId = 1;
    protected final int id;
    protected String name;
    protected String email;

    public Person(String name, String email) {
        this.id = nextId++;
        setName(name);
        setEmail(email);
    }

    public abstract void display();

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = Strings.clean(name); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = Strings.clean(email); }

    @Override
    public String toString() {
        return String.format("ID: %d | Name: %s | Email: %s", id, name, email);
    }
}
