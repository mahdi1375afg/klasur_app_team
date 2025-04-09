package com.klasurapp.dao;

import com.klasurapp.model.Nutzer;
import com.klasurapp.model.NutzerKonto;
import com.klasurapp.model.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data Access Object for NutzerKonto operations.
 */
public class NutzerKontoDAO {
    private static final Logger logger = LoggerFactory.getLogger(NutzerKontoDAO.class);
    private final NutzerDAO nutzerDAO;
    private final TaskDAO taskDAO;

    public NutzerKontoDAO() {
        this.nutzerDAO = new NutzerDAO();
        this.taskDAO = new TaskDAO();
    }

    /**
     * Create database tables if they don't exist.
     */
    public void initializeTable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Nutzer table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS nutzer (" +
                "id SERIAL PRIMARY KEY, " +
                "vorname VARCHAR(100) NOT NULL, " +
                "nachname VARCHAR(100) NOT NULL, " +
                "email VARCHAR(255) UNIQUE NOT NULL, " +
                "rolle VARCHAR(50) NOT NULL" +
                ")"
            );
            
            // NutzerKonto table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS nutzer_konto (" +
                "id SERIAL PRIMARY KEY, " +
                "benutzername VARCHAR(100) UNIQUE NOT NULL, " +
                "passwort_hash VARCHAR(255) NOT NULL, " +
                "letzte_anmeldung TIMESTAMP, " +
                "aktiv BOOLEAN NOT NULL DEFAULT TRUE, " +
                "nutzer_id INTEGER NOT NULL REFERENCES nutzer(id)" +
                ")"
            );
            
            // Erstellte Aufgaben table (many-to-many relationship)
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS nutzer_aufgaben (" +
                "nutzer_id INTEGER NOT NULL REFERENCES nutzer_konto(id), " +
                "aufgabe_id INTEGER NOT NULL, " +
                "PRIMARY KEY (nutzer_id, aufgabe_id)" +
                ")"
            );
            
            // Aufgaben Antworten table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS nutzer_antworten (" +
                "nutzer_id INTEGER NOT NULL REFERENCES nutzer_konto(id), " +
                "aufgabe_id INTEGER NOT NULL, " +
                "antwort TEXT NOT NULL, " +
                "PRIMARY KEY (nutzer_id, aufgabe_id)" +
                ")"
            );
            
            logger.info("Database tables initialized");
        } catch (SQLException e) {
            logger.error("Error initializing database tables", e);
        }
    }

    /**
     * Create a new user account in the database.
     * 
     * @param konto the account to create
     * @return the created account with generated ID
     */
    public NutzerKonto create(NutzerKonto konto) {
        // First ensure the Nutzer exists
        Nutzer nutzer = konto.getNutzer();
        if (nutzer.getId() == null) {
            nutzer = nutzerDAO.create(nutzer);
            konto.setNutzer(nutzer);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO nutzer_konto (benutzername, passwort_hash, aktiv, nutzer_id) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, konto.getBenutzername());
            stmt.setString(2, konto.getPasswortHash());
            stmt.setBoolean(3, konto.isAktiv());
            stmt.setLong(4, konto.getNutzer().getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating user account failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    konto.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating user account failed, no ID obtained.");
                }
            }
            
            // Save associated tasks and answers
            saveUserTasks(conn, konto);
            saveUserAnswers(conn, konto);
            
            logger.info("Created user account: {}", konto.getBenutzername());
            return konto;
        } catch (SQLException e) {
            logger.error("Error creating user account", e);
            throw new RuntimeException("Error creating user account", e);
        }
    }

/**
 * Find a user account by username.
 * 
 * @param benutzername the username
 * @return an Optional containing the account, or empty if not found
 */
public Optional<NutzerKonto> findByBenutzername(String benutzername) {
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(
            "SELECT k.id, k.benutzername, k.passwort_hash, k.letzte_anmeldung, k.aktiv, k.nutzer_id " +
            "FROM nutzer_konto k " +
            "WHERE k.benutzername = ?")) {
        
        stmt.setString(1, benutzername);
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                // Create basic NutzerKonto without loading related data yet
                NutzerKonto konto = new NutzerKonto();
                konto.setId(rs.getLong("id"));
                konto.setBenutzername(rs.getString("benutzername"));
                konto.setPasswortHash(rs.getString("passwort_hash"));
                
                Timestamp lastLogin = rs.getTimestamp("letzte_anmeldung");
                if (lastLogin != null) {
                    konto.setLetzteAnmeldung(lastLogin.toLocalDateTime());
                }
                
                konto.setAktiv(rs.getBoolean("aktiv"));
                
                // Load associated Nutzer
                long nutzerId = rs.getLong("nutzer_id");
                nutzerDAO.findById(nutzerId).ifPresent(konto::setNutzer);
                
                // Load related data with separate connections
                loadUserTasks(konto);
                loadUserAnswers(konto);
                
                return Optional.of(konto);
            } else {
                return Optional.empty();
            }
        }
    } catch (SQLException e) {
        logger.error("Error finding user account by username", e);
        return Optional.empty();
    }
}

    /**
     * Find a user account by ID.
     * 
     * @param id the account ID
     * @return an Optional containing the account, or empty if not found
     */
    public Optional<NutzerKonto> findById(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT k.id, k.benutzername, k.passwort_hash, k.letzte_anmeldung, k.aktiv, k.nutzer_id " +
                "FROM nutzer_konto k " +
                "WHERE k.id = ?")) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    NutzerKonto konto = mapResultSetToNutzerKonto(conn, rs);
                    return Optional.of(konto);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user account by ID", e);
            return Optional.empty();
        }
    }


 /**
 * Update an existing user account.
 * 
 * @param konto the account to update
 * @return the updated account
 */
public NutzerKonto update(NutzerKonto konto) {
    try (Connection conn = DatabaseConnection.getConnection()) {
        conn.setAutoCommit(false); // Start transaction

        try {
            // Update nutzer_konto table
            try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE nutzer_konto SET benutzername = ?, passwort_hash = ?, letzte_anmeldung = ?, aktiv = ? " +
                "WHERE id = ?")) {

                stmt.setString(1, konto.getBenutzername());
                stmt.setString(2, konto.getPasswortHash());

                LocalDateTime letzteAnmeldung = konto.getLetzteAnmeldung();
                if (letzteAnmeldung != null) {
                    stmt.setTimestamp(3, Timestamp.valueOf(letzteAnmeldung));
                } else {
                    stmt.setNull(3, Types.TIMESTAMP);
                }

                stmt.setBoolean(4, konto.isAktiv());
                stmt.setLong(5, konto.getId());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Updating user account failed, no rows affected.");
                }
            }

            // Update Nutzer if needed
            if (konto.getNutzer() != null) {
                nutzerDAO.update(conn, konto.getNutzer()); // Pass the same connection
            }

            // Update associated tasks and answers
            deleteUserTasks(conn, konto.getId());
            deleteUserAnswers(conn, konto.getId());
            saveUserTasks(conn, konto);
            saveUserAnswers(conn, konto);

            conn.commit(); // Commit transaction
            logger.info("Updated user account: {}", konto.getBenutzername());
            return konto;
        } catch (SQLException e) {
            conn.rollback(); // Rollback transaction on error
            logger.error("Error updating user account", e);
            throw new RuntimeException("Error updating user account", e);
        }
    } catch (SQLException e) {
        logger.error("Database connection error", e);
        throw new RuntimeException("Database connection error", e);
    }
}

    /**
     * Delete a user account.
     * 
     * @param id the ID of the account to delete
     * @return true if the account was deleted
     */
    public boolean delete(Long id) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Delete associated data first
            deleteUserTasks(conn, id);
            deleteUserAnswers(conn, id);
            
            // Then delete the account
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM nutzer_konto WHERE id = ?")) {
                stmt.setLong(1, id);
                int affectedRows = stmt.executeUpdate();
                
                logger.info("Deleted user account with ID: {}", id);
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            logger.error("Error deleting user account", e);
            return false;
        }
    }

    /**
     * Retrieve all user accounts from the database.
     * 
     * @return a list of all user accounts
     */
    public List<NutzerKonto> findAll() {
        List<NutzerKonto> accounts = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT k.id, k.benutzername, k.passwort_hash, k.letzte_anmeldung, k.aktiv, k.nutzer_id " +
                "FROM nutzer_konto k")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    NutzerKonto konto = mapResultSetToNutzerKonto(conn, rs);
                    accounts.add(konto);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all accounts", e);
        }
        return accounts;
    }

    // Helper methods
    private NutzerKonto mapResultSetToNutzerKonto(Connection conn, ResultSet rs) throws SQLException {
        NutzerKonto konto = new NutzerKonto();
        konto.setId(rs.getLong("id"));
        konto.setBenutzername(rs.getString("benutzername"));
        konto.setPasswortHash(rs.getString("passwort_hash"));
        
        Timestamp lastLogin = rs.getTimestamp("letzte_anmeldung");
        if (lastLogin != null) {
            konto.setLetzteAnmeldung(lastLogin.toLocalDateTime());
        }
        
        konto.setAktiv(rs.getBoolean("aktiv"));
        
        // Load associated Nutzer
        long nutzerId = rs.getLong("nutzer_id");
        nutzerDAO.findById(nutzerId).ifPresent(konto::setNutzer);
        
        // Load associated tasks and answers
        loadUserTasks(konto);
        loadUserAnswers(konto);
        
        return konto;
    }

    private void loadUserTasks(NutzerKonto konto) throws SQLException {
        if (konto.getId() == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT aufgabe_id FROM nutzer_aufgaben WHERE nutzer_id = ?")) {
            
            stmt.setLong(1, konto.getId());
            
            List<Task> tasks = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long taskId = rs.getLong("aufgabe_id");
                    taskDAO.findById(taskId).ifPresent(tasks::add);
                }
            }
            
            konto.setErstellteAufgaben(tasks);
        }
    }

    private void loadUserAnswers(NutzerKonto konto) throws SQLException {
        if (konto.getId() == null) return;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT aufgabe_id, antwort FROM nutzer_antworten WHERE nutzer_id = ?")) {
            
            stmt.setLong(1, konto.getId());
            
            Map<Long, String> answers = new HashMap<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long taskId = rs.getLong("aufgabe_id");
                    String answer = rs.getString("antwort");
                    answers.put(taskId, answer);
                }
            }
            
            konto.setAufgabenAntworten(answers);
        }
    }

    private void saveUserTasks(Connection conn, NutzerKonto konto) throws SQLException {
        if (konto.getId() == null || konto.getErstellteAufgaben() == null) {
            return;
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO nutzer_aufgaben (nutzer_id, aufgabe_id) VALUES (?, ?)")) {
            
            for (Task task : konto.getErstellteAufgaben()) {
                if (task.getId() != null) {
                    stmt.setLong(1, konto.getId());
                    stmt.setLong(2, task.getId());
                    stmt.addBatch();
                }
            }
            
            stmt.executeBatch();
        }
    }

    private void saveUserAnswers(Connection conn, NutzerKonto konto) throws SQLException {
        if (konto.getId() == null || konto.getAufgabenAntworten() == null) {
            return;
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO nutzer_antworten (nutzer_id, aufgabe_id, antwort) VALUES (?, ?, ?)")) {
            
            for (Map.Entry<Long, String> entry : konto.getAufgabenAntworten().entrySet()) {
                stmt.setLong(1, konto.getId());
                stmt.setLong(2, entry.getKey());
                stmt.setString(3, entry.getValue());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        }
    }

    private void deleteUserTasks(Connection conn, Long nutzerId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "DELETE FROM nutzer_aufgaben WHERE nutzer_id = ?")) {
            stmt.setLong(1, nutzerId);
            stmt.executeUpdate();
        }
    }

    private void deleteUserAnswers(Connection conn, Long nutzerId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "DELETE FROM nutzer_antworten WHERE nutzer_id = ?")) {
            stmt.setLong(1, nutzerId);
            stmt.executeUpdate();
        }
    }
}