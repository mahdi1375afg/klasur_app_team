package com.klasurapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a closed task with specific answer options.
 */
public class ClosedTask extends Task {
    private ClosedTaskType closedTaskType;
    private String correctAnswer;
    private List<String> options = new ArrayList<>();

    public ClosedTask() {
        super();
        setFormat(TaskFormat.CLOSED);
    }

    public ClosedTask(String name, String text, int estimatedTimeMinutes, BloomLevel bloomLevel,
                     Module module, ClosedTaskType closedTaskType) {
        super(name, text, estimatedTimeMinutes, bloomLevel, TaskFormat.CLOSED, module);
        this.closedTaskType = closedTaskType;
    }

    public ClosedTaskType getClosedTaskType() {
        return closedTaskType;
    }

    public void setClosedTaskType(ClosedTaskType closedTaskType) {
        this.closedTaskType = closedTaskType;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void addOption(String option) {
        this.options.add(option);
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    @Override
    public String getSolution() {
        return correctAnswer;
    }
}