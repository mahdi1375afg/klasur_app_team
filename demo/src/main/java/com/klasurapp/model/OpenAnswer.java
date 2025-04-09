package com.klasurapp.model;

/**
 * Represents an answer to an open-ended task.
 */
public class OpenAnswer extends Answer {
    private String text;

    public OpenAnswer() {
        super();
    }

    public OpenAnswer(Long taskId, Long userId, String text) {
        super(taskId, userId);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getAnswerContent() {
        return text;
    }

    @Override
    public boolean isCorrect(Task task) {
        // For open answers, we can't automatically determine correctness
        // This would typically be done by a human grader
        return isGraded() && getScore() != null && getScore() > 0;
    }
}