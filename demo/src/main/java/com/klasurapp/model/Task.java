package com.klasurapp.model;
import java.util.Objects;

/**
 * Base class for all exam tasks.
 */
public abstract class Task {
    private Long id;
    private String name;
    private String text;
    private int estimatedTimeMinutes;
    private BloomLevel bloomLevel;
    private TaskFormat format;
    private Module module;

    // Constructors
    protected Task() {
    }

    protected Task(String name, String text, int estimatedTimeMinutes, BloomLevel bloomLevel, 
                  TaskFormat format, Module module) {
        this.name = name;
        this.text = text;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.bloomLevel = bloomLevel;
        this.format = format;
        this.module = module;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }

    public void setEstimatedTimeMinutes(int estimatedTimeMinutes) {
        this.estimatedTimeMinutes = estimatedTimeMinutes;
    }

    public BloomLevel getBloomLevel() {
        return bloomLevel;
    }

    public void setBloomLevel(BloomLevel bloomLevel) {
        this.bloomLevel = bloomLevel;
    }

    public TaskFormat getFormat() {
        return format;
    }

    public void setFormat(TaskFormat format) {
        this.format = format;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Abstract method to get the solution for this task.
     */
    public abstract String getSolution();
}