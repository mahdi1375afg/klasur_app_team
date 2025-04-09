package com.klasurapp;

import com.klasurapp.ui.MainFrame;
import com.klasurapp.dao.DatabaseConnection;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Klausur-Generator Application");
        
        // Datenbankinitialisierung
        try {
            initializeDatabase();
            logger.info("Database initialized successfully");
        } catch (Exception e) {
            logger.error("Database initialization failed", e);
            System.err.println("Fehler bei der Datenbankinitialisierung: " + e.getMessage());
            return;
        }
        
        // GUI starten
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            } catch (Exception e) {
                logger.error("Error starting GUI", e);
                System.err.println("Fehler beim Starten der Anwendung: " + e.getMessage());
            }
        });
    }
    
    /**
     * Initialisiert die Datenbank mit dem Schema aus der schema.sql-Datei
     */
    private static void initializeDatabase() throws Exception {
        logger.info("Initializing database from schema.sql");
        
        // SQL-Schema aus Ressourcen laden
        InputStream is = Main.class.getClassLoader().getResourceAsStream("schema.sql");
        if (is == null) {
            throw new Exception("Schema file not found in resources");
        }
        
        String schema = new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n"));
        
        // Verbindung zur Datenbank herstellen und Schema ausführen
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Aufteilen des Schemas in einzelne Befehle
            String[] commands = schema.split(";");
            
            for (String command : commands) {
                String trimmedCommand = command.trim();
                if (!trimmedCommand.isEmpty() && !trimmedCommand.startsWith("--")) {
                    stmt.execute(trimmedCommand);
                    logger.debug("Executed SQL command: {}", trimmedCommand);
                }
            }
            
            logger.info("Database schema initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing database schema", e);
            throw new Exception("Failed to initialize database: " + e.getMessage(), e);
        }
    }
}