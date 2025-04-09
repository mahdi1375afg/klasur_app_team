package com.klasurapp.model;

/**
 * Represents the format of a task (open or closed).
 */
public enum TaskFormat {
    CLOSED("Geschlossene Aufgaben", "Überprüfung von Faktenwissen, Sofortige Feedbackmöglichkeiten"),
    OPEN("Offene Aufgaben", "Förderung von kritischem Denken, Vertiefung der Analysefähigkeit");

    private final String name;
    private final String description;

    TaskFormat(String name, String description) {
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
