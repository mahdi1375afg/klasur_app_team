package com.klasurapp.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.klasurapp.dao.NutzerKontoDAO;
import com.klasurapp.model.NutzerKonto;

/**
 * Service for user authentication.
 */
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final int SALT_LENGTH = 16;
    
    private NutzerKontoDAO nutzerKontoDAO;
    private NutzerKonto currentUser;

    public AuthenticationService(NutzerKontoDAO nutzerKontoDAO) {
        this.nutzerKontoDAO = nutzerKontoDAO;
    }

    /**
     * Authenticate a user with username and password.
     * 
     * @param benutzername the username
     * @param passwort the password (plain text)
     * @return true if authentication was successful
     */
    public boolean login(String benutzername, String passwort) {
        try {
            Optional<NutzerKonto> kontoOpt = nutzerKontoDAO.findByBenutzername(benutzername);
            
            if (!kontoOpt.isPresent() || !kontoOpt.get().isAktiv()) {
                logger.warn("Login failed for user: {}", benutzername);
                return false;
            }
            
            NutzerKonto konto = kontoOpt.get();
            String storedHash = konto.getPasswortHash();
            
            // Extract salt from stored hash
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                logger.error("Invalid hash format for user: {}", benutzername);
                return false;
            }
            
            String salt = parts[0];
            String expectedHash = parts[1];
            
            // Hash the provided password with the same salt
            String actualHash = hashPassword(passwort, Base64.getDecoder().decode(salt));
            
            if (expectedHash.equals(actualHash)) {
                // Update last login time
                konto.setLetzteAnmeldung(LocalDateTime.now());
                nutzerKontoDAO.update(konto);
                
                // Set current user
                currentUser = konto;
                logger.info("User logged in successfully: {}", benutzername);
                return true;
            } else {
                logger.warn("Invalid password for user: {}", benutzername);
                return false;
            }
        } catch (Exception e) {
            logger.error("Authentication error", e);
            return false;
        }
    }
    
    /**
     * Log out the current user.
     */
    public void logout() {
        currentUser = null;
        logger.info("User logged out");
    }
    
    /**
     * Check if a user is currently logged in.
     * 
     * @return true if a user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Get the currently logged-in user.
     * 
     * @return the current user or null if no user is logged in
     */
    public NutzerKonto getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Register a new user account.
     * 
     * @param konto the account to register
     * @param passwort the plain text password
     * @return true if registration was successful
     */
    public boolean register(NutzerKonto konto, String passwort) {
        try {
            // Check if username already exists
            if (nutzerKontoDAO.findByBenutzername(konto.getBenutzername()).isPresent()) {
                logger.warn("Username already exists: {}", konto.getBenutzername());
                return false;
            }
            
            // Generate salt and hash password
            byte[] salt = generateSalt();
            String hash = hashPassword(passwort, salt);
            String saltAndHash = Base64.getEncoder().encodeToString(salt) + ":" + hash;
            
            // Set password hash and save account
            konto.setPasswortHash(saltAndHash);
            konto.setAktiv(true);
            nutzerKontoDAO.create(konto);
            
            logger.info("User registered successfully: {}", konto.getBenutzername());
            return true;
        } catch (Exception e) {
            logger.error("Registration error", e);
            return false;
        }
    }
    
    /**
     * Generate a random salt.
     * 
     * @return random salt bytes
     */
    private byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Hash a password with a salt using SHA-256.
     * 
     * @param password the password to hash
     * @param salt the salt to use
     * @return the hashed password
     */
    private String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}