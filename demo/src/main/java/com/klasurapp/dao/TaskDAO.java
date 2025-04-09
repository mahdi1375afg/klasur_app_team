package com.klasurapp.dao;

import com.klasurapp.model.BloomLevel;
import com.klasurapp.model.ClosedTask;
import com.klasurapp.model.ClosedTaskType;
import com.klasurapp.model.Module;
import com.klasurapp.model.OpenTask;
import com.klasurapp.model.Task;
import com.klasurapp.model.TaskFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Data Access Object für Task-Operationen.
 */
public class TaskDAO {
    private static final Logger logger = LoggerFactory.getLogger(TaskDAO.class);
    private final ModuleDAO moduleDAO;

    public TaskDAO() {
        this.moduleDAO = new ModuleDAO();
    }

    /**
     * Erstellt die Datenbanktabellen für Aufgaben.
     */
    public void initializeTable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Aufgaben-Tabelle erstellen
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS tasks (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "text TEXT NOT NULL, " +
                "estimated_time_minutes INTEGER NOT NULL, " +
                "bloom_level VARCHAR(20) NOT NULL, " +
                "task_format VARCHAR(20) NOT NULL, " +
                "module_id INTEGER NOT NULL" +
                ")"
            );
            
            // Tabelle für offene Aufgaben
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS open_tasks (" +
                "task_id INTEGER PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE, " +
                "sample_solution TEXT NOT NULL" +
                ")"
            );
            
            // Tabelle für geschlossene Aufgaben
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS closed_tasks (" +
                "task_id INTEGER PRIMARY KEY REFERENCES tasks(id) ON DELETE CASCADE, " +
                "closed_task_type VARCHAR(20) NOT NULL, " +
                "correct_answer TEXT NOT NULL" +
                ")"
            );
            
            // Tabelle für Antwortoptionen bei geschlossenen Aufgaben
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS closed_task_options (" +
                "id SERIAL PRIMARY KEY, " +
                "task_id INTEGER NOT NULL REFERENCES closed_tasks(task_id) ON DELETE CASCADE, " +
                "option_text TEXT NOT NULL, " +
                "option_order INTEGER NOT NULL" +
                ")"
            );
            
            logger.info("Task-Tabellen initialisiert");
        } catch (SQLException e) {
            logger.error("Fehler beim Initialisieren der Task-Tabellen", e);
        }
    }

    /**
     * Erstellt eine neue Aufgabe in der Datenbank.
     * 
     * @param task Die zu erstellende Aufgabe (OpenTask oder ClosedTask)
     * @return Die erstellte Aufgabe mit generierter ID
     */
    public Task create(Task task) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Basis-Aufgabe in die tasks-Tabelle einfügen
                Long taskId = insertBaseTask(conn, task);
                task.setId(taskId);
                
                // Je nach Aufgabentyp in die entsprechende Tabelle einfügen
                if (task instanceof OpenTask) {
                    insertOpenTask(conn, (OpenTask) task);
                } else if (task instanceof ClosedTask) {
                    insertClosedTask(conn, (ClosedTask) task);
                }
                
                conn.commit();
                logger.info("Aufgabe erstellt: {}", task.getName());
                return task;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Fehler beim Erstellen der Aufgabe", e);
                throw new RuntimeException("Fehler beim Erstellen der Aufgabe", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Datenbankverbindungsfehler", e);
            throw new RuntimeException("Datenbankverbindungsfehler", e);
        }
    }

    /**
     * Findet eine Aufgabe anhand ihrer ID.
     * 
     * @param id Die Aufgaben-ID
     * @return Optional mit der Aufgabe oder leer wenn nicht gefunden
     */
    public Optional<Task> findById(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT t.id, t.name, t.text, t.estimated_time_minutes, " +
                "t.bloom_level, t.task_format, t.module_id, " +
                "ot.sample_solution, ct.closed_task_type, ct.correct_answer " +
                "FROM tasks t " +
                "LEFT JOIN open_tasks ot ON t.id = ot.task_id " +
                "LEFT JOIN closed_tasks ct ON t.id = ct.task_id " +
                "WHERE t.id = ?")) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Task task = mapResultSetToTask(conn, rs);
                    return Optional.of(task);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen der Aufgabe nach ID", e);
            return Optional.empty();
        }
    }

    /**
     * Findet alle Aufgaben eines Moduls.
     * 
     * @param moduleId Die Modul-ID
     * @return Liste von Aufgaben des Moduls
     */
    public List<Task> findByModule(Long moduleId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByModule(conn, moduleId);
        } catch (SQLException e) {
            logger.error("Error finding tasks by module", e);
            return Collections.emptyList();
        }
    }

    public List<Task> findByModule(Connection conn, long moduleId) throws SQLException {
        List<Task> tasks = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT t.id, t.name, t.text, t.estimated_time_minutes, t.bloom_level, t.task_format, t.module_id " +
            "FROM tasks t WHERE t.module_id = ?")) {

            stmt.setLong(1, moduleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = mapResultSetToTask(conn, rs);
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    /**
     * Aktualisiert eine bestehende Aufgabe.
     */
    public Task update(Task task) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Basis-Aufgabendaten aktualisieren
                updateBaseTask(conn, task);
                
                // Spezifische Aufgabendaten aktualisieren
                if (task instanceof OpenTask) {
                    updateOpenTask(conn, (OpenTask) task);
                } else if (task instanceof ClosedTask) {
                    updateClosedTask(conn, (ClosedTask) task);
                }
                
                conn.commit();
                logger.info("Aufgabe aktualisiert: {}", task.getName());
                return task;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Fehler beim Aktualisieren der Aufgabe", e);
                throw new RuntimeException("Fehler beim Aktualisieren der Aufgabe", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Datenbankverbindungsfehler", e);
            throw new RuntimeException("Datenbankverbindungsfehler", e);
        }
    }
    
    /**
     * Löscht eine Aufgabe.
     */
    public boolean delete(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            
            logger.info("Aufgabe mit ID {} gelöscht", id);
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Löschen der Aufgabe", e);
            return false;
        }
    }

    // Hilfsmethoden
    
    private Long insertBaseTask(Connection conn, Task task) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO tasks (name, text, estimated_time_minutes, bloom_level, task_format, module_id) " +
            "VALUES (?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, task.getName());
            stmt.setString(2, task.getText());
            stmt.setInt(3, task.getEstimatedTimeMinutes());
            stmt.setString(4, task.getBloomLevel().name());
            stmt.setString(5, task.getFormat().name());
            stmt.setLong(6, task.getModule().getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Erstellen der Aufgabe fehlgeschlagen, keine Zeilen betroffen.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Erstellen der Aufgabe fehlgeschlagen, keine ID erhalten.");
                }
            }
        }
    }
    
    private void insertOpenTask(Connection conn, OpenTask task) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO open_tasks (task_id, sample_solution) VALUES (?, ?)")) {
            
            stmt.setLong(1, task.getId());
            stmt.setString(2, task.getSampleSolution());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Erstellen der offenen Aufgabe fehlgeschlagen, keine Zeilen betroffen.");
            }
        }
    }
    
    private void insertClosedTask(Connection conn, ClosedTask task) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO closed_tasks (task_id, closed_task_type, correct_answer) VALUES (?, ?, ?)")) {
            
            stmt.setLong(1, task.getId());
            stmt.setString(2, task.getClosedTaskType().name());
            stmt.setString(3, task.getCorrectAnswer());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Erstellen der geschlossenen Aufgabe fehlgeschlagen, keine Zeilen betroffen.");
            }
        }
        
        // Antwortoptionen für geschlossene Aufgabe einfügen
        insertClosedTaskOptions(conn, task);
    }
    
    private void insertClosedTaskOptions(Connection conn, ClosedTask task) throws SQLException {
        if (task.getOptions() == null || task.getOptions().isEmpty()) {
            return;
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO closed_task_options (task_id, option_text, option_order) VALUES (?, ?, ?)")) {
            
            int order = 0;
            for (String option : task.getOptions()) {
                stmt.setLong(1, task.getId());
                stmt.setString(2, option);
                stmt.setInt(3, order++);
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        }
    }
    
    private void updateBaseTask(Connection conn, Task task) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE tasks SET name = ?, text = ?, estimated_time_minutes = ?, " +
            "bloom_level = ?, task_format = ?, module_id = ? WHERE id = ?")) {
            
            stmt.setString(1, task.getName());
            stmt.setString(2, task.getText());
            stmt.setInt(3, task.getEstimatedTimeMinutes());
            stmt.setString(4, task.getBloomLevel().name());
            stmt.setString(5, task.getFormat().name());
            stmt.setLong(6, task.getModule().getId());
            stmt.setLong(7, task.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Aktualisieren der Aufgabe fehlgeschlagen, keine Zeilen betroffen.");
            }
        }
    }
    
    private void updateOpenTask(Connection conn, OpenTask task) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE open_tasks SET sample_solution = ? WHERE task_id = ?")) {
            
            stmt.setString(1, task.getSampleSolution());
            stmt.setLong(2, task.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                // Die offene Aufgabe existiert möglicherweise noch nicht, daher einfügen
                insertOpenTask(conn, task);
            }
        }
    }
    
    private void updateClosedTask(Connection conn, ClosedTask task) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE closed_tasks SET closed_task_type = ?, correct_answer = ? WHERE task_id = ?")) {
            
            stmt.setString(1, task.getClosedTaskType().name());
            stmt.setString(2, task.getCorrectAnswer());
            stmt.setLong(3, task.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                // Die geschlossene Aufgabe existiert möglicherweise noch nicht, daher einfügen
                insertClosedTask(conn, task);
            } else {
                // Optionen aktualisieren
                deleteClosedTaskOptions(conn, task.getId());
                insertClosedTaskOptions(conn, task);
            }
        }
    }
    
    private void deleteClosedTaskOptions(Connection conn, Long taskId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "DELETE FROM closed_task_options WHERE task_id = ?")) {
            
            stmt.setLong(1, taskId);
            stmt.executeUpdate();
        }
    }
    
    private Task mapResultSetToTask(Connection conn, ResultSet rs) throws SQLException {
        // First set the ID to avoid null ID issues
        long taskId = rs.getLong("id");
        
        // Create the appropriate task type
        Task task;
        String format = rs.getString("task_format");
        if ("OPEN".equalsIgnoreCase(format)) {
            task = new OpenTask();
        } else {
            task = new ClosedTask();
        }
        
        // Set common task properties
        task.setId(taskId);
        task.setName(rs.getString("name"));
        task.setText(rs.getString("text"));
        task.setEstimatedTimeMinutes(rs.getInt("estimated_time_minutes"));
        task.setBloomLevel(BloomLevel.valueOf(rs.getString("bloom_level")));
        
        // Get the module by its ID and set it on the task
        long moduleId = rs.getLong("module_id");
        moduleDAO.findById(moduleId).ifPresent(task::setModule);
        
        // If it's a closed task, load the options using the same connection
        if (task instanceof ClosedTask) {
            loadClosedTaskOptions(conn, (ClosedTask) task);
        }
        
        return task;
    }
    
    private void loadClosedTaskOptions(Connection conn, ClosedTask task) throws SQLException {
        // Make sure we have a valid task ID
        if (task.getId() == null) {
            logger.warn("Cannot load options for task with null ID");
            return;
        }
        
        // Get a fresh connection if the current one is closed
        Connection optionsConn = conn;
        boolean needNewConnection = false;
        
        try {
            if (optionsConn.isClosed()) {
                needNewConnection = true;
            }
        } catch (SQLException e) {
            needNewConnection = true;
        }
        
        try {
            // If the connection is closed, get a new one
            if (needNewConnection) {
                optionsConn = DatabaseConnection.getConnection();
            }
            
            try (PreparedStatement stmt = optionsConn.prepareStatement(
                "SELECT option_text FROM closed_task_options WHERE task_id = ? ORDER BY option_order")) {
                
                stmt.setLong(1, task.getId());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        task.addOption(rs.getString("option_text"));
                    }
                }
            }
        } finally {
            // Close the new connection if we created one
            if (needNewConnection && optionsConn != null && !optionsConn.isClosed()) {
                try {
                    optionsConn.close();
                } catch (SQLException e) {
                    // Ignore close errors
                }
            }
        }
    }
        
}
