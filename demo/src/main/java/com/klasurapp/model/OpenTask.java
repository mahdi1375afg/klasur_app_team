package com.klasurapp.model;

/**
 * Represents an open-ended task.
 */
public class OpenTask extends Task {
    private String sampleSolution;

    public OpenTask() {
        super();
        setFormat(TaskFormat.OPEN);
    }

    public OpenTask(String name, String text, int estimatedTimeMinutes, BloomLevel bloomLevel,
                    Module module, String sampleSolution) {
        super(name, text, estimatedTimeMinutes, bloomLevel, TaskFormat.OPEN, module);
        this.sampleSolution = sampleSolution;
    }

    public String getSampleSolution() {
        return sampleSolution;
    }

    public void setSampleSolution(String sampleSolution) {
        this.sampleSolution = sampleSolution;
    }

    @Override
    public String getSolution() {
        return sampleSolution;
    }
}