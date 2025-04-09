package com.klasurapp.model;

import java.util.Objects;

/**
 * Represents a user in the system.
 */
public class Nutzer {
    private Long id;
    private String vorname;
    private String nachname;
    private String email;
    private String rolle; // e.g., "ADMIN", "DOZENT", "STUDENT"

    public Nutzer() {
    }

    public Nutzer(String vorname, String nachname, String email, String rolle) {
        this.vorname = vorname;
        this.nachname = nachname;
        this.email = email;
        this.rolle = rolle;
    }

    public Nutzer(Long id, String vorname, String nachname, String email, String rolle) {
        this.id = id;
        this.vorname = vorname;
        this.nachname = nachname;
        this.email = email;
        this.rolle = rolle;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRolle() {
        return rolle;
    }

    public void setRolle(String rolle) {
        this.rolle = rolle;
    }

    public String getVollerName() {
        return vorname + " " + nachname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nutzer nutzer = (Nutzer) o;
        return Objects.equals(id, nutzer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Nutzer{" +
                "id=" + id +
                ", vorname='" + vorname + '\'' +
                ", nachname='" + nachname + '\'' +
                ", email='" + email + '\'' +
                ", rolle='" + rolle + '\'' +
                '}';
    }
}