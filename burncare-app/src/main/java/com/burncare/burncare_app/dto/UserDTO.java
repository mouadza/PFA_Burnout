package com.burncare.burncare_app.dto;

public class UserDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String profession;
    private boolean enabled; // ✅ Champ crucial pour activer/désactiver

    public UserDTO() {}

    public UserDTO(String firstName, String lastName, String email, String role, String profession, boolean enabled) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.profession = profession;
        this.enabled = enabled;
    }

    // Getters
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getProfession() { return profession; }
    public boolean isEnabled() { return enabled; }

    // Setters
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setProfession(String profession) { this.profession = profession; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}