package com.klasurapp.dao;

import com.klasurapp.model.Task;
import com.klasurapp.model.Answer;
import com.klasurapp.model.ClosedAnswer;
import com.klasurapp.model.OpenAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object für Answer-Operationen.
 */
public class AnswerDAO {
    private static final Logger logger = LoggerFactory.getLogger(AnswerDAO.class);
    private final TaskDAO taskDAO;

    public AnswerDAO() {
        this.taskDAO = new TaskDAO();
    }

    /**
     * Erstellt die Datenbanktabellen für Antworten.
     */
    public void initializeTable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Basis-Antwort-Tabelle
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS answers (" +
                "id SERIAL PRIMARY KEY, " +
                "task_id INTEGER NOT NULL, " +
                "user_id INTEGER NOT NULL, " +
                "submission_time TIMESTAMP NOT NULL, " +
                "is_graded BOOLEAN NOT NULL DEFAULT FALSE, " +
                "score DOUBLE PRECISION, " +
                "feedback TEXT, " +
                "answer_type VARCHAR(10) NOT NULL" + // 'OPEN' or 'CLOSED'
                ")"
            );
            
            // Offene Antworten
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS open_answers (" +
                "answer_id INTEGER PRIMARY KEY REFERENCES answers(id) ON DELETE CASCADE, " +
                "text TEXT NOT NULL" +
                ")"
            );
            
            // Geschlossene Antworten
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS closed_answers (" +
                "answer_id INTEGER PRIMARY KEY REFERENCES answers(id) ON DELETE CASCADE, " +
                "selected_option TEXT NOT NULL" +
                ")"
            );
            
            logger.info("Antwort-Tabellen initialisiert");
        } catch (SQLException e) {
            logger.error("Fehler beim Initialisieren der Antwort-Tabellen", e);
        }
    }

    /**
     * Speichert eine Antwort in der Datenbank.
     */
    public Answer create(Answer answer) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                Long answerId = insertBaseAnswer(conn, answer);
                answer.setId(answerId);
                
                if (answer instanceof OpenAnswer) {
                    insertOpenAnswer(conn, (OpenAnswer) answer);
                } else if (answer instanceof ClosedAnswer) {
                    insertClosedAnswer(conn, (ClosedAnswer) answer);
                }
                
                conn.commit();
                logger.info("Antwort erstellt für Aufgabe ID: {}", answer.getTaskId());
                return answer;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Fehler beim Erstellen der Antwort", e);
                throw new RuntimeException("Fehler beim Erstellen der Antwort", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Datenbankverbindungsfehler", e);
            throw new RuntimeException("Datenbankverbindungsfehler", e);
        }
    }
    
    /**
     * Findet eine Antwort anhand ihrer ID.
     */
    public Optional<Answer> findById(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT a.id, a.task_id, a.user_id, a.submission_time, " +
                "a.is_graded, a.score, a.feedback, a.answer_type, " +
                "o.text, c.selected_option " +
                "FROM answers a " +
                "LEFT JOIN open_answers o ON a.id = o.answer_id " +
                "LEFT JOIN closed_answers c ON a.id = c.answer_id " +
                "WHERE a.id = ?")) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAnswer(rs));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen der Antwort nach ID", e);
            return Optional.empty();
        }
    }
    
    /**
     * Findet alle Antworten für eine bestimmte Aufgabe.
     */
    public List<Answer> findByTaskId(Long taskId) {
        List<Answer> answers = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT a.id, a.task_id, a.user_id, a.submission_time, " +
                "a.is_graded, a.score, a.feedback, a.answer_type, " +
                "o.text, c.selected_option " +
                "FROM answers a " +
                "LEFT JOIN open_answers o ON a.id = o.answer_id " +
                "LEFT JOIN closed_answers c ON a.id = c.answer_id " +
                "WHERE a.task_id = ? " +
                "ORDER BY a.submission_time DESC")) {
            
            stmt.setLong(1, taskId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen der Antworten nach Aufgabe", e);
        }
        
        return answers;
    }
    
    /**
     * Findet alle Antworten eines Benutzers.
     */
    public List<Answer> findByUserId(Long userId) {
        List<Answer> answers = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT a.id, a.task_id, a.user_id, a.submission_time, " +
                "a.is_graded, a.score, a.feedback, a.answer_type, " +
                "o.text, c.selected_option " +
                "FROM answers a " +
                "LEFT JOIN open_answers o ON a.id = o.answer_id " +
                "LEFT JOIN closed_answers c ON a.id = c.answer_id " +
                "WHERE a.user_id = ? " +
                "ORDER BY a.submission_time DESC")) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen der Antworten nach Benutzer", e);
        }
        
        return answers;
    }
    
    /**
     * Aktualisiert eine bestehende Antwort.
     */
    public Answer update(Answer answer) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                updateBaseAnswer(conn, answer);
                
                if (answer instanceof OpenAnswer) {
                    updateOpenAnswer(conn, (OpenAnswer) answer);
                } else if (answer instanceof ClosedAnswer) {
                    updateClosedAnswer(conn, (ClosedAnswer) answer);
                }
                
                conn.commit();
                logger.info("Antwort aktualisiert: {}", answer.getId());
                return answer;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Fehler beim Aktualisieren der Antwort", e);
                throw new RuntimeException("Fehler beim Aktualisieren der Antwort", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Datenbankverbindungsfehler", e);
            throw new RuntimeException("Datenbankverbindungsfehler", e);
        }
    }
    
    /**
     * Löscht eine Antwort.
     */
    public boolean delete(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM answers WHERE id = ?")) {
            
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            
            logger.info("Antwort mit ID {} gelöscht", id);
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Löschen der Antwort", e);
            return false;
        }
    }

    // Hilfsmethoden
    
    private Long insertBaseAnswer(Connection conn, Answer answer) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO answers (task_id, user_id, submission_time, is_graded, score, feedback, answer_type) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, answer.getTaskId());
            stmt.setLong(2, answer.getUserId());
            stmt.setTimestamp(3, Timestamp.valueOf(answer.getSubmissionTime()));
            stmt.setBoolean(4, answer.isGraded());
            
            if (answer.getScore() != null) {
                stmt.setDouble(5, answer.getScore());
            } else {
                stmt.setNull(5, Types.DOUBLE);
            }
            
            stmt.setString(6, answer.getFeedback());
            stmt.setString(7, answer instanceof OpenAnswer ? "OPEN" : "CLOSED");
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Erstellen der Antwort fehlgeschlagen, keine Zeilen betroffen.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Erstellen der Antwort fehlgeschlagen, keine ID erhalten.");
                }
            }
        }
    }
    
    private void insertOpenAnswer(Connection conn, OpenAnswer answer) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO open_answers (answer_id, text) VALUES (?, ?)")) {
            
            stmt.setLong(1, answer.getId());
            stmt.setString(2, answer.getText());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Erstellen der offenen Antwort fehlgeschlagen, keine Zeilen betroffen.");
            }
        }
    }
    
    private void insertClosedAnswer(Connection conn, ClosedAnswer answer) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO closed_answers (answer_id, selected_option) VALUES (?, ?)")) {
            
            stmt.setLong(1, answer.getId());
            stmt.setString(2, answer.getSelectedOption());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Erstellen der geschlossenen Antwort fehlgeschlagen, keine Zeilen betroffen.");
            }
        }
    }
    
    private void updateBaseAnswer(Connection conn, Answer answer) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE answers SET submission_time = ?, is_graded = ?, score = ?, feedback = ? " +
            "WHERE id = ?")) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(answer.getSubmissionTime()));
            stmt.setBoolean(2, answer.isGraded());
            
            if (answer.getScore() != null) {
                stmt.setDouble(3, answer.getScore());
            } else {
                stmt.setNull(3, Types.DOUBLE);
            }
            
            stmt.setString(4, answer.getFeedback());
            stmt.setLong(5, answer.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Aktualisieren der Antwort fehlgeschlagen, keine Zeilen betroffen.");
            }
        }
    }
    
    private void updateOpenAnswer(Connection conn, OpenAnswer answer) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE open_answers SET text = ? WHERE answer_id = ?")) {
            
            stmt.setString(1, answer.getText());
            stmt.setLong(2, answer.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                insertOpenAnswer(conn, answer); // Falls noch nicht existiert
            }
        }
    }
    
    private void updateClosedAnswer(Connection conn, ClosedAnswer answer) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE closed_answers SET selected_option = ? WHERE answer_id = ?")) {
            
            stmt.setString(1, answer.getSelectedOption());
            stmt.setLong(2, answer.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                insertClosedAnswer(conn, answer); // Falls noch nicht existiert
            }
        }
    }
    
    private Answer mapResultSetToAnswer(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long taskId = rs.getLong("task_id");
        Long userId = rs.getLong("user_id");
        LocalDateTime submissionTime = rs.getTimestamp("submission_time").toLocalDateTime();
        boolean isGraded = rs.getBoolean("is_graded");
        Double score = rs.getObject("score") != null ? rs.getDouble("score") : null;
        String feedback = rs.getString("feedback");
        String answerType = rs.getString("answer_type");
        
        Answer answer;
        if ("OPEN".equals(answerType)) {
            OpenAnswer openAnswer = new OpenAnswer();
            openAnswer.setText(rs.getString("text"));
            answer = openAnswer;
        } else {
            ClosedAnswer closedAnswer = new ClosedAnswer();
            closedAnswer.setSelectedOption(rs.getString("selected_option"));
            answer = closedAnswer;
        }
        
        answer.setId(id);
        answer.setTaskId(taskId);
        answer.setUserId(userId);
        answer.setSubmissionTime(submissionTime);
        answer.setGraded(isGraded);
        answer.setScore(score);
        answer.setFeedback(feedback);
        
        return answer;
    }
}