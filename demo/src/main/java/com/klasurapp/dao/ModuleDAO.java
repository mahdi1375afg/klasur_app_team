package com.klasurapp.dao;

import com.klasurapp.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object für Module-Operationen.
 */
public class ModuleDAO {
    private static final Logger logger = LoggerFactory.getLogger(ModuleDAO.class);

    /**
     * Erstellt die Datenbanktabellen für Module.
     */
    public void initializeTable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS modules (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "code VARCHAR(50) NOT NULL UNIQUE, " +
                "description TEXT" +
                ")"
            );
            
            logger.info("Modul-Tabelle initialisiert");
        } catch (SQLException e) {
            logger.error("Fehler beim Initialisieren der Modul-Tabelle", e);
        }
    }

    /**
     * Erstellt ein neues Modul in der Datenbank.
     * 
     * @param module Das zu erstellende Modul
     * @return Das erstellte Modul mit generierter ID
     */
    public Module create(Module module) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO modules (name, code, description) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, module.getName());
            stmt.setString(2, module.getCode());
            stmt.setString(3, module.getDescription());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Erstellen des Moduls fehlgeschlagen, keine Zeilen betroffen.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    module.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Erstellen des Moduls fehlgeschlagen, keine ID erhalten.");
                }
            }
            
            logger.info("Modul erstellt: {}", module.getName());
            return module;
        } catch (SQLException e) {
            logger.error("Fehler beim Erstellen des Moduls", e);
            throw new RuntimeException("Fehler beim Erstellen des Moduls", e);
        }
    }

    /**
     * Sucht ein Modul anhand seiner ID.
     * 
     * @param id Die Modul-ID
     * @return Optional mit dem Modul oder leer, wenn nicht gefunden
     */
    public Optional<Module> findById(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, code, description FROM modules WHERE id = ?")) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Module module = mapResultSetToModule(rs);
                    return Optional.of(module);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen des Moduls nach ID", e);
            return Optional.empty();
        }
    }

    /**
     * Sucht ein Modul anhand seines Codes.
     * 
     * @param code Der Modul-Code
     * @return Optional mit dem Modul oder leer, wenn nicht gefunden
     */
    public Optional<Module> findByCode(String code) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, code, description FROM modules WHERE code = ?")) {
            
            stmt.setString(1, code);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Module module = mapResultSetToModule(rs);
                    return Optional.of(module);
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Suchen des Moduls nach Code", e);
            return Optional.empty();
        }
    }

    /**
     * Ruft alle Module aus der Datenbank ab.
     * 
     * @return Liste aller Module
     */
    public List<Module> findAll() {
        List<Module> modules = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, code, description FROM modules ORDER BY name")) {
            
            while (rs.next()) {
                Module module = mapResultSetToModule(rs);
                modules.add(module);
            }
            
        } catch (SQLException e) {
            logger.error("Fehler beim Abrufen aller Module", e);
        }
        
        return modules;
    }

    /**
     * Aktualisiert ein bestehendes Modul.
     * 
     * @param module Das zu aktualisierende Modul
     * @return Das aktualisierte Modul
     */
    public Module update(Module module) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "UPDATE modules SET name = ?, code = ?, description = ? WHERE id = ?")) {
            
            stmt.setString(1, module.getName());
            stmt.setString(2, module.getCode());
            stmt.setString(3, module.getDescription());
            stmt.setLong(4, module.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Aktualisieren des Moduls fehlgeschlagen, keine Zeilen betroffen.");
            }
            
            logger.info("Modul aktualisiert: {}", module.getName());
            return module;
        } catch (SQLException e) {
            logger.error("Fehler beim Aktualisieren des Moduls", e);
            throw new RuntimeException("Fehler beim Aktualisieren des Moduls", e);
        }
    }

    /**
     * Löscht ein Modul.
     * 
     * @param id Die ID des zu löschenden Moduls
     * @return true, wenn das Modul gelöscht wurde
     */
    public boolean delete(Long id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM modules WHERE id = ?")) {
            
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            logger.info("Modul mit ID {} gelöscht", id);
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Löschen des Moduls", e);
            return false;
        }
    }

    /**
     * Konvertiert einen ResultSet-Eintrag in ein Module-Objekt.
     * 
     * @param rs Das ResultSet
     * @return Das Module-Objekt
     * @throws SQLException Bei Datenbankfehlern
     */
    private Module mapResultSetToModule(ResultSet rs) throws SQLException {
        Module module = new Module();
        module.setId(rs.getLong("id"));
        module.setName(rs.getString("name"));
        module.setCode(rs.getString("code"));
        module.setDescription(rs.getString("description"));
        return module;
    }
}