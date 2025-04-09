package com.klasurapp.dao;

import com.klasurapp.model.Nutzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

/**
 * Data Access Object for Nutzer operations.
 */
public class NutzerDAO {
    private static final Logger logger = LoggerFactory.getLogger(NutzerDAO.class);

    /**
     * Create a new user in the database.
     * 
     * @param nutzer the user to create
     * @return the created user with generated ID
     */
    public Nutzer create(Nutzer nutzer) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO nutzer (vorname, nachname, email, rolle) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, nutzer.getVorname());
            stmt.setString(2, nutzer.getNachname());
            stmt.setString(3, nutzer.getEmail());
            stmt.setString(4, nutzer.getRolle());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    nutzer.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
            
            logger.info("Created user: {}", nutzer.getEmail());
            return nutzer;
        } catch (SQLException e) {
            logger.error("Error creating user", e);
            throw new RuntimeException("Error creating user", e);
        }
    }

    /**
     * Find a user by ID.
     * 
     * @param id the user ID
     * @return an Optional containing the user, or empty if not found
     */
    public Optional<Nutzer> findById(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, vorname, nachname, email, rolle FROM nutzer WHERE id = ?")) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Nutzer nutzer = new Nutzer();
                    nutzer.setId(rs.getLong("id"));
                    nutzer.setVorname(rs.getString("vorname"));
                    nutzer.setNachname(rs.getString("nachname"));
                    nutzer.setEmail(rs.getString("email"));
                    nutzer.setRolle(rs.getString("rolle"));
                    return Optional.of(nutzer);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by ID", e);
            return Optional.empty();
        }
    }

    /**
     * Find a user by email.
     * 
     * @param email the user's email
     * @return an Optional containing the user, or empty if not found
     */
    public Optional<Nutzer> findByEmail(String email) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, vorname, nachname, email, rolle FROM nutzer WHERE email = ?")) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Nutzer nutzer = new Nutzer();
                    nutzer.setId(rs.getLong("id"));
                    nutzer.setVorname(rs.getString("vorname"));
                    nutzer.setNachname(rs.getString("nachname"));
                    nutzer.setEmail(rs.getString("email"));
                    nutzer.setRolle(rs.getString("rolle"));
                    return Optional.of(nutzer);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by email", e);
            return Optional.empty();
        }
    }

    /**
     * Update an existing user.
     * 
     * @param nutzer the user to update
     * @return the updated user
     */
    public Nutzer update(Nutzer nutzer) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE nutzer SET vorname = ?, nachname = ?, email = ?, rolle = ? WHERE id = ?")) {
            
            stmt.setString(1, nutzer.getVorname());
            stmt.setString(2, nutzer.getNachname());
            stmt.setString(3, nutzer.getEmail());
            stmt.setString(4, nutzer.getRolle());
            stmt.setLong(5, nutzer.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            }
            
            logger.info("Updated user: {}", nutzer.getEmail());
            return nutzer;
        } catch (SQLException e) {
            logger.error("Error updating user", e);
            throw new RuntimeException("Error updating user", e);
        }
    }

    /**
     * Update an existing user with a provided connection.
     * 
     * @param conn the connection to use
     * @param nutzer the user to update
     * @return the updated user
     * @throws SQLException if a database access error occurs
     */
    public Nutzer update(Connection conn, Nutzer nutzer) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE nutzer SET vorname = ?, nachname = ?, email = ?, rolle = ? WHERE id = ?")) {

            stmt.setString(1, nutzer.getVorname());
            stmt.setString(2, nutzer.getNachname());
            stmt.setString(3, nutzer.getEmail());
            stmt.setString(4, nutzer.getRolle());
            stmt.setLong(5, nutzer.getId());


            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating Nutzer failed, no rows affected.");
            }

            logger.info("Updated Nutzer: {}", nutzer.getEmail());
            return nutzer;
        }
    }

    /**
     * Delete a user.
     * 
     * @param id the ID of the user to delete
     * @return true if the user was deleted
     */
    public boolean delete(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM nutzer WHERE id = ?")) {
            
            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            
            logger.info("Deleted user with ID: {}", id);
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting user", e);
            return false;
        }
    }
}