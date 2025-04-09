package com.klasurapp.model;

/**
 * Represents cognitive levels according to Bloom's taxonomy.
 */
public enum BloomLevel {
    REMEMBER(1, "erinnern", "Gelerntes auswendig wiedergeben, Ausführen von Routinen."),
    UNDERSTAND(2, "verstehen", "Gelerntes erklären, reformulieren oder paraphrasieren."),
    APPLY(3, "anwenden", "Gelerntes in neuem Kontext / neuer Situation anwenden"),
    ANALYZE(4, "analysieren", "Gelerntes in Bestandteile zerlegen, Strukturen erläutern."),
    EVALUATE(5, "bewerten", "Gelerntes nach (meist selbst) gewählten Kriterien kritisch beurteilen"),
    CREATE(6, "erschaffen", "Gelerntes neu zusammenfügen oder neue Inhalte generieren.");

    private final int level;
    private final String name;
    private final String description;

    BloomLevel(int level, String name, String description) {
        this.level = level;
        this.name = name;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}