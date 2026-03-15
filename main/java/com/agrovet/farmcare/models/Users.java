package com.agrovet.farmcare.models;

public class Users {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String IDNumber;
    private String role;

    public Users(String username, String password, String email, String firstName, String lastName, String IDNumber, String role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.IDNumber = IDNumber;
        this.role = role;
    }
    public Users() {}

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getPassword() {
        return password;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getIDNumber() {
        return IDNumber;
    }
    public void setIDNumber(String IDNumber) {
        this.IDNumber = IDNumber;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "user : {" +
                "User ID :" + IDNumber + '\'' +
                "First Name : " + firstName + '\'' +
                "Last Name : " + lastName + '\'' +
                "Role : " + role + '\'' +
                '}';
    }
}
