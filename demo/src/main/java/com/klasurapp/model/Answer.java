package com.klasurapp.model;


import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class for all answers to exam tasks.
 */
public abstract class Answer {
    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime submissionTime;
    private boolean isGraded;
    private Double score;
    private String feedback;

    protected Answer() {
        this.submissionTime = LocalDateTime.now();
        this.isGraded = false;
    }

    protected Answer(Long taskId, Long userId) {
        this();
        this.taskId = taskId;
        this.userId = userId;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(LocalDateTime submissionTime) {
        this.submissionTime = submissionTime;
    }

    public boolean isGraded() {
        return isGraded;
    }

    public void setGraded(boolean graded) {
        isGraded = graded;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
        this.isGraded = (score != null);
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    /**
     * Get the answer content as a string representation.
     */
    public abstract String getAnswerContent();

    /**
     * Check if the answer is correct according to the task's solution.
     */
    public abstract boolean isCorrect(Task task);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Answer answer = (Answer) o;
        return Objects.equals(id, answer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}