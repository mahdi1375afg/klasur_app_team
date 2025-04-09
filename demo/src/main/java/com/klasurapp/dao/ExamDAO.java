package com.klasurapp.dao;

import com.klasurapp.model.Exam;
import com.klasurapp.model.Module;
import com.klasurapp.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object für Exam-Operationen.
 */
public class ExamDAO {
    private static final Logger logger = LoggerFactory.getLogger(ExamDAO.class);
    private final ModuleDAO moduleDAO;
    private final TaskDAO taskDAO;

    public ExamDAO() {
        this.moduleDAO = new ModuleDAO();
        this.taskDAO = new TaskDAO();
    }

    /**
     * Erstellt die Datenbanktabellen für Klausuren.
     */
    public void initializeTable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Klausur-Tabelle erstellen
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS exams (" +
                "id SERIAL PRIMARY KEY, " +
                "title VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "exam_date DATE, " +
                "duration_minutes INTEGER, " +
                "module_id INTEGER NOT NULL" +
                ")"
            );
            
            // Verknüpfungstabelle für Klausuren und Aufgaben (Many-to-Many)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS exam_tasks (" +
                "exam_id INTEGER NOT NULL REFERENCES exams(id) ON DELETE CASCADE, " +
                "task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE, " +
                "task_order INTEGER NOT NULL, " +
                "PRIMARY KEY (exam_id, task_id)" +
                ")"
            );
            
            logger.info("Klausur-Tabellen initialisiert");
        } catch (SQLException e) {
            logger.error("Fehler beim Initialisieren der Klausur-Tabellen", e);
        }
    }

    /**
     * Erstellt eine neue Klausur in der Datenbank.
     * 
     * @param exam Die zu erstellende Klausur
     * @return Die erstellte Klausur mit generierter ID
     */
    public Exam create(Exam exam) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Basisinformationen der Klausur speichern
                Long examId = insertBaseExam(conn, exam);
                exam.setId(examId);
                
                // Zugehörige Aufgaben speichern
                saveExamTasks(conn, exam);
                
                conn.commit();
                logger.info("Klausur erstellt: {}", exam.getTitle());
                return exam;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Fehler beim Erstellen der Klausur", e);
                throw new RuntimeException("Fehler beim Erstellen der Klausur", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Datenbankverbindungsfehler", e);
            throw new RuntimeException("Datenbankverbindungsfehler", e);
        }
    }

    /**
     * Findet eine Klausur anhand ihrer ID.
     * 
     * @param id Die Klausur-ID
     * @return Optional mit der Klausur oder leer wenn nicht gefunden
     */
    public Optional<Exam> findById(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, title, description, exam_date, duration_minutes, module_id " +
                "FROM exams WHERE id = ?")) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Exam exam = mapResultSetToExam(conn, rs);
                    return Optional.of(exam);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen der Klausur nach ID", e);
            return Optional.empty();
        }
    }

    /**
     * Findet alle Klausuren eines Moduls.
     * 
     * @param moduleId Die Modul-ID
     * @return Liste von Klausuren des Moduls
     */
    public List<Exam> findByModule(Long moduleId) {
        List<Exam> exams = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, title, description, exam_date, duration_minutes, module_id " +
                "FROM exams WHERE module_id = ? ORDER BY exam_date DESC")) {
            
            stmt.setLong(1, moduleId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Exam exam = mapResultSetToExam(conn, rs);
                    exams.add(exam);
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen der Klausuren nach Modul", e);
        }
        
        return exams;
    }

    /**
     * Ruft alle Klausuren aus der Datenbank ab.
     * 
     * @return Liste aller Klausuren
     */
    public List<Exam> findAll() {
        List<Exam> exams = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT id, title, description, exam_date, duration_minutes, module_id " +
                "FROM exams ORDER BY exam_date DESC")) {
            
            while (rs.next()) {
                Exam exam = mapResultSetToExam(conn, rs);
                exams.add(exam);
            }
            
        } catch (SQLException e) {
            logger.error("Fehler beim Abrufen aller Klausuren", e);
        }
        
        return exams;
    }

    /**
     * Aktualisiert eine bestehende Klausur.
     * 
     * @param exam Die zu aktualisierende Klausur
     * @return Die aktualisierte Klausur
     */
    public Exam update(Exam exam) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Basis-Klausurdaten aktualisieren
                updateBaseExam(conn, exam);
                
                // Zugehörige Aufgaben aktualisieren
                deleteExamTasks(conn, exam.getId());
                saveExamTasks(conn, exam);
                
                conn.commit();
                logger.info("Klausur aktualisiert: {}", exam.getTitle());
                return exam;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Fehler beim Aktualisieren der Klausur", e);
                throw new RuntimeException("Fehler beim Aktualisieren der Klausur", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Datenbankverbindungsfehler", e);
            throw new RuntimeException("Datenbankverbindungsfehler", e);
        }
    }
    
    /**
     * Löscht eine Klausur.
     * 
     * @param id Die ID der zu löschenden Klausur
     * @return true, wenn die Klausur gelöscht wurde
     */
    public boolean delete(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM exams WHERE id = ?")) {
            
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            
            logger.info("Klausur mit ID {} gelöscht", id);
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Löschen der Klausur", e);
            return false;
        }
    }

    // Hilfsmethoden
    
    private Long insertBaseExam(Connection conn, Exam exam) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO exams (title, description, exam_date, duration_minutes, module_id) " +
            "VALUES (?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, exam.getTitle());
            stmt.setString(2, exam.getDescription());
            
            if (exam.getExamDate() != null) {
                stmt.setDate(3, Date.valueOf(exam.getExamDate()));
            } else {
                stmt.setNull(3, Types.DATE);
            }
            
            stmt.setInt(4, exam.getDurationMinutes());
            stmt.setLong(5, exam.getModule().getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Erstellen der Klausur fehlgeschlagen, keine Zeilen betroffen.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Erstellen der Klausur fehlgeschlagen, keine ID erhalten.");
                }
            }
        }
    }
    
    private void updateBaseExam(Connection conn, Exam exam) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE exams SET title = ?, description = ?, exam_date = ?, " +
            "duration_minutes = ?, module_id = ? WHERE id = ?")) {
            
            stmt.setString(1, exam.getTitle());
            stmt.setString(2, exam.getDescription());
            
            if (exam.getExamDate() != null) {
                stmt.setDate(3, Date.valueOf(exam.getExamDate()));
            } else {
                stmt.setNull(3, Types.DATE);
            }
            
            stmt.setInt(4, exam.getDurationMinutes());
            stmt.setLong(5, exam.getModule().getId());
            stmt.setLong(6, exam.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Aktualisieren der Klausur fehlgeschlagen, keine Zeilen betroffen.");
            }
        }
    }
    
    private void saveExamTasks(Connection conn, Exam exam) throws SQLException {
        if (exam.getId() == null || exam.getTasks() == null || exam.getTasks().isEmpty()) {
            return;
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO exam_tasks (exam_id, task_id, task_order) VALUES (?, ?, ?)")) {
            
            int order = 0;
            for (Task task : exam.getTasks()) {
                if (task.getId() != null) {
                    stmt.setLong(1, exam.getId());
                    stmt.setLong(2, task.getId());
                    stmt.setInt(3, order++);
                    stmt.addBatch();
                }
            }
            
            stmt.executeBatch();
        }
    }
    
    private void deleteExamTasks(Connection conn, Long examId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "DELETE FROM exam_tasks WHERE exam_id = ?")) {
            
            stmt.setLong(1, examId);
            stmt.executeUpdate();
        }
    }
    
    private Exam mapResultSetToExam(Connection conn, ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setId(rs.getLong("id"));
        exam.setTitle(rs.getString("title"));
        exam.setDescription(rs.getString("description"));
        
        Date examDate = rs.getDate("exam_date");
        if (examDate != null) {
            exam.setExamDate(examDate.toLocalDate());
        }
        
        exam.setDurationMinutes(rs.getInt("duration_minutes"));
        
        // Modul abrufen
        long moduleId = rs.getLong("module_id");
        Module module = moduleDAO.findById(moduleId)
                .orElseThrow(() -> new SQLException("Modul mit ID " + moduleId + " nicht gefunden"));
        exam.setModule(module);
        
        // Aufgaben der Klausur laden
        loadExamTasks(conn, exam);
        
        return exam;
    }
    
    private void loadExamTasks(Connection conn, Exam exam) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT task_id FROM exam_tasks " +
            "WHERE exam_id = ? ORDER BY task_order")) {
            
            stmt.setLong(1, exam.getId());
            
            List<Task> tasks = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long taskId = rs.getLong("task_id");
                    taskDAO.findById(taskId).ifPresent(tasks::add);
                }
            }
            
            exam.setTasks(tasks);
        }
    }
}