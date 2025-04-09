package com.klasurapp.model;

/**
 * Represents different types of closed tasks.
 */
public enum ClosedTaskType {
    SINGLE_CHOICE("Single-Choice Fragen", "Der Prüfling wählt eine einzige Antwort aus mehreren Optionen aus."),
    MULTIPLE_CHOICE("Multiple-Choice Fragen", "Der Prüfling kann eine oder mehrere Antworten aus mehreren Optionen auswählen."),
    TRUE_FALSE("Wahr/Falsch Fragen", "Der Prüfling gibt an, ob eine Aussage wahr oder falsch ist."),
    GAP_TEXT("Lückentextaufgaben", "Der Prüfling ergänzt einen Text mit vorgegebenen Wörtern."),
    MATCHING("Zuordnungsaufgaben", "Der Prüfling ordnet Begriffe einander zu, wobei mehrere Zuordnungen möglich sind."),
    RANKING("Ranking-Aufgaben", "Der Prüfling bringt Elemente in eine festgelegte Reihenfolge.");

    private final String name;
    private final String description;

    ClosedTaskType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
