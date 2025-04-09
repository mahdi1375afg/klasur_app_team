package com.klasurapp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a user account with authentication details and task storage.
 */
public class NutzerKonto {
    private Long id;
    private String benutzername;
    private String passwortHash;
    private LocalDateTime letzteAnmeldung;
    private boolean aktiv;
    private Nutzer nutzer;
    
    // Maps to store user-specific tasks and their answers
    private List<Task> erstellteAufgaben = new ArrayList<>();
    private Map<Long, String> aufgabenAntworten = new HashMap<>(); // TaskId -> Answer

    public NutzerKonto() {
    }

    public NutzerKonto(String benutzername, String passwortHash, Nutzer nutzer) {
        this.benutzername = benutzername;
        this.passwortHash = passwortHash;
        this.aktiv = true;
        this.nutzer = nutzer;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBenutzername() {
        return benutzername;
    }

    public void setBenutzername(String benutzername) {
        this.benutzername = benutzername;
    }

    public String getPasswortHash() {
        return passwortHash;
    }

    public void setPasswortHash(String passwortHash) {
        this.passwortHash = passwortHash;
    }

    public LocalDateTime getLetzteAnmeldung() {
        return letzteAnmeldung;
    }

    public void setLetzteAnmeldung(LocalDateTime letzteAnmeldung) {
        this.letzteAnmeldung = letzteAnmeldung;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Nutzer getNutzer() {
        return nutzer;
    }

    public void setNutzer(Nutzer nutzer) {
        this.nutzer = nutzer;
    }

    // Task and answer management
    public List<Task> getErstellteAufgaben() {
        return erstellteAufgaben;
    }

    public void setErstellteAufgaben(List<Task> erstellteAufgaben) {
        this.erstellteAufgaben = erstellteAufgaben;
    }

    public void addAufgabe(Task aufgabe) {
        this.erstellteAufgaben.add(aufgabe);
    }

    public Map<Long, String> getAufgabenAntworten() {
        return aufgabenAntworten;
    }

    public void setAufgabenAntworten(Map<Long, String> aufgabenAntworten) {
        this.aufgabenAntworten = aufgabenAntworten;
    }

    public void speichereAntwort(Long aufgabeId, String antwort) {
        this.aufgabenAntworten.put(aufgabeId, antwort);
    }

    public String getAntwort(Long aufgabeId) {
        return this.aufgabenAntworten.get(aufgabeId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NutzerKonto konto = (NutzerKonto) o;
        return Objects.equals(id, konto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NutzerKonto{" +
                "id=" + id +
                ", benutzername='" + benutzername + '\'' +
                ", aktiv=" + aktiv +
                ", nutzer=" + nutzer +
                '}';
    }
}