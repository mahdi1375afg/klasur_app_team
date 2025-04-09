package com.klasurapp.model;


/**
 * Represents an answer to a closed task.
 */
public class ClosedAnswer extends Answer {
    private String selectedOption;

    public ClosedAnswer() {
        super();
    }

    public ClosedAnswer(Long taskId, Long userId, String selectedOption) {
        super(taskId, userId);
        this.selectedOption = selectedOption;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    @Override
    public String getAnswerContent() {
        return selectedOption;
    }

    @Override
    public boolean isCorrect(Task task) {
        if (!(task instanceof ClosedTask)) {
            return false;
        }
        
        ClosedTask closedTask = (ClosedTask) task;
        
        // Compare the selected option with the correct answer
        // Format depends on the closed task type
        switch (closedTask.getClosedTaskType()) {
            case SINGLE_CHOICE:
            case TRUE_FALSE:
                return selectedOption.equals(closedTask.getCorrectAnswer());
                
            case MULTIPLE_CHOICE:
                // For multiple choice, we expect comma-separated indices
                return selectedOption.equals(closedTask.getCorrectAnswer());
                
            case GAP_TEXT:
                // For gap text, we might expect exact match
                return selectedOption.equals(closedTask.getCorrectAnswer());
                
            case MATCHING:
            case RANKING:
                // For matching and ranking, we expect special formats
                // that would need to be parsed and compared
                return selectedOption.equals(closedTask.getCorrectAnswer());
                
            default:
                return false;
        }
    }
}