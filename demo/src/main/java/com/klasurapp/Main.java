package com.klasurapp;

import com.klasurapp.dao.*;
import com.klasurapp.model.*;
import com.klasurapp.model.Module;
import com.klasurapp.service.AuthenticationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.Optional;

/**
 * Main entry point for the Klasur App application (Console Version).
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final Scanner scanner = new Scanner(System.in);
    
    private static AuthenticationService authService;
    private static ModuleDAO moduleDAO;
    private static TaskDAO taskDAO;
    private static ExamDAO examDAO;
    private static AnswerDAO answerDAO;

    public static void main(String[] args) {
        logger.info("Starting Klasur App (Console Version)");
        
        // Initialize database schema
        initializeDatabase();
        
        // Initialize services
        initializeServices();
        
        // Start the console menu
        showMainMenu();
        
        // Close scanner at the end
        scanner.close();
    }
    
    private static void initializeDatabase() {
        logger.info("Initializing database schema");
        
        try {
            // Initialize all necessary database tables
            NutzerKontoDAO nutzerKontoDAO = new NutzerKontoDAO();
            nutzerKontoDAO.initializeTable();
            
            moduleDAO = new ModuleDAO();
            moduleDAO.initializeTable();
            
            taskDAO = new TaskDAO();
            taskDAO.initializeTable();
            
            examDAO = new ExamDAO();
            examDAO.initializeTable();
            
            answerDAO = new AnswerDAO();
            answerDAO.initializeTable();
            
            System.out.println("Database initialization complete.");
        } catch (Exception e) {
            logger.error("Error initializing database", e);
            System.err.println("Error initializing database: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static void initializeServices() {
        logger.info("Initializing services");
        
        try {
            NutzerKontoDAO nutzerKontoDAO = new NutzerKontoDAO();
            authService = new AuthenticationService(nutzerKontoDAO);
            
            System.out.println("Services initialization complete.");
        } catch (Exception e) {
            logger.error("Error initializing services", e);
            System.err.println("Error initializing services: " + e.getMessage());
            System.exit(1);
        }
    }

    // Main menu navigation
    private static void showMainMenu() {
        boolean exit = false;
        
        while (!exit) {
            System.out.println("\n===== KLASUR APP =====");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            System.out.print("Select an option: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    handleLogin();
                    break;
                case "2":
                    handleRegistration();
                    break;
                case "0":
                    exit = true;
                    System.out.println("Exiting application. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void handleLogin() {
        System.out.println("\n===== LOGIN =====");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        
        if (authService.login(username, password)) {
            System.out.println("Login successful!");
            showUserMenu();
        } else {
            System.out.println("Login failed. Invalid username or password.");
        }
    }
    
    private static void handleRegistration() {
        System.out.println("\n===== REGISTRATION =====");
        
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        
        System.out.print("First Name: ");
        String firstName = scanner.nextLine().trim();
        
        System.out.print("Last Name: ");
        String lastName = scanner.nextLine().trim();
        
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        
        System.out.print("Role (ADMIN, DOZENT, STUDENT): ");
        String role = scanner.nextLine().trim();
        
        Nutzer nutzer = new Nutzer(firstName, lastName, email, role);
        NutzerKonto konto = new NutzerKonto();
        konto.setBenutzername(username);
        konto.setNutzer(nutzer);
        
        if (authService.register(konto, password)) {
            System.out.println("Registration successful! You can now login.");
        } else {
            System.out.println("Registration failed. Username may already exist.");
        }
    }
    
    private static void showUserMenu() {
        NutzerKonto currentUser = authService.getCurrentUser();
        String userRole = currentUser.getNutzer().getRolle();
        boolean logout = false;
        
        while (!logout) {
            System.out.println("\n===== USER MENU =====");
            System.out.println("Logged in as: " + currentUser.getNutzer().getVollerName() + 
                               " (" + userRole + ")");
            
            System.out.println("\n1. Module Management");
            System.out.println("2. Task Management");
            System.out.println("3. Exam Management");
            
            if ("ADMIN".equalsIgnoreCase(userRole)) {
                System.out.println("4. Manage Accounts"); // New option for admins
            }
            
            System.out.println("5. Delete Account");
            System.out.println("9. Logout");
            System.out.println("0. Exit");
            
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    showModuleMenu();
                    break;
                case "2":
                    showTaskMenu();
                    break;
                case "3":
                    showExamMenu();
                    break;
                case "4":
                    if ("ADMIN".equalsIgnoreCase(userRole)) {
                        manageAccounts(); // Call the new method
                    } else {
                        System.out.println("Invalid option. Please try again.");
                    }
                    break;
                case "5":
                    deleteCurrentAccount();
                    logout = true;
                    break;
                case "9":
                    authService.logout();
                    logout = true;
                    System.out.println("Logged out successfully.");
                    break;
                case "0":
                    System.out.println("Exiting application. Goodbye!");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void deleteCurrentAccount() {
        NutzerKonto currentUser = authService.getCurrentUser();
        System.out.print("Möchtest du dein Konto wirklich löschen? (j/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if ("j".equals(confirm) || "ja".equals(confirm)) {
            // Delete account using a new instance of NutzerKontoDAO
            NutzerKontoDAO kontoDAO = new NutzerKontoDAO();
            boolean success = kontoDAO.delete(currentUser.getId());
            if (success) {
                System.out.println("Dein Konto wurde erfolgreich gelöscht.");
                authService.logout();
            } else {
                System.out.println("Konto konnte nicht gelöscht werden.");
            }
        } else {
            System.out.println("Löschung abgebrochen.");
        }
    }

    private static void manageAccounts() {
        NutzerKontoDAO kontoDAO = new NutzerKontoDAO();
        List<NutzerKonto> accounts = kontoDAO.findAll(); // Add a method to fetch all accounts
        
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }
        
        System.out.println("\n===== MANAGE ACCOUNTS =====");
        System.out.println("ID\tUsername\tEmail\tRole");
        for (NutzerKonto account : accounts) {
            System.out.printf("%d\t%s\t%s\t%s\n", 
                account.getId(), 
                account.getBenutzername(), 
                account.getNutzer().getEmail(), 
                account.getNutzer().getRolle());
        }
        
        System.out.print("Enter the ID of the account to delete (or 0 to cancel): ");
        try {
            long accountId = Long.parseLong(scanner.nextLine().trim());
            if (accountId == 0) {
                System.out.println("Operation cancelled.");
                return;
            }
            
            System.out.print("Are you sure you want to delete this account? (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            if ("y".equals(confirm) || "yes".equals(confirm)) {
                boolean success = kontoDAO.delete(accountId);
                if (success) {
                    System.out.println("Account deleted successfully.");
                } else {
                    System.out.println("Account not found or could not be deleted.");
                }
            } else {
                System.out.println("Deletion cancelled.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        }
    }

    // Module management menu
    private static void showModuleMenu() {
        boolean back = false;
        
        while (!back) {
            System.out.println("\n===== MODULE MANAGEMENT =====");
            System.out.println("1. List all modules");
            System.out.println("2. Create new module");
            System.out.println("3. Update module");
            System.out.println("4. Delete module");
            System.out.println("9. Back to main menu");
            
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    listAllModules();
                    break;
                case "2":
                    createModule();
                    break;
                case "3":
                    updateModule();
                    break;
                case "4":
                    deleteModule();
                    break;
                case "9":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void listAllModules() {
        System.out.println("\n===== ALL MODULES =====");
        List<Module> modules = moduleDAO.findAll();
        
        if (modules.isEmpty()) {
            System.out.println("No modules found.");
            return;
        }
        
        System.out.println("ID\tCode\tName\tDescription");
        for (Module module : modules) {
            System.out.printf("%d\t%s\t%s\t%s\n", 
                module.getId(), 
                module.getCode(), 
                module.getName(),
                module.getDescription() != null ? module.getDescription() : "-");
        }
    }
    
    private static void createModule() {
        System.out.println("\n===== CREATE MODULE =====");
        
        System.out.print("Module name: ");
        String name = scanner.nextLine().trim();
        
        System.out.print("Module code: ");
        String code = scanner.nextLine().trim();
        
        System.out.print("Module description: ");
        String description = scanner.nextLine().trim();
        
        Module module = new Module();
        module.setName(name);
        module.setCode(code);
        module.setDescription(description);
        
        try {
            module = moduleDAO.create(module);
            System.out.println("Module created successfully with ID: " + module.getId());
        } catch (Exception e) {
            System.out.println("Error creating module: " + e.getMessage());
            logger.error("Error creating module", e);
        }
    }
    
    private static void updateModule() {
        System.out.println("\n===== UPDATE MODULE =====");
        listAllModules();
        
        System.out.print("Enter the ID of the module to update: ");
        try {
            long id = Long.parseLong(scanner.nextLine().trim());
            Optional<Module> moduleOpt = moduleDAO.findById(id);
            
            if (!moduleOpt.isPresent()) {
                System.out.println("Module not found.");
                return;
            }
            
            Module module = moduleOpt.get();
            
            System.out.print("Module name [" + module.getName() + "]: ");
            String name = scanner.nextLine().trim();
            if (!name.isEmpty()) {
                module.setName(name);
            }
            
            System.out.print("Module code [" + module.getCode() + "]: ");
            String code = scanner.nextLine().trim();
            if (!code.isEmpty()) {
                module.setCode(code);
            }
            
            System.out.print("Module description [" + module.getDescription() + "]: ");
            String description = scanner.nextLine().trim();
            if (!description.isEmpty()) {
                module.setDescription(description);
            }
            
            module = moduleDAO.update(module);
            System.out.println("Module updated successfully.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        } catch (Exception e) {
            System.out.println("Error updating module: " + e.getMessage());
            logger.error("Error updating module", e);
        }
    }
    
    private static void deleteModule() {
        System.out.println("\n===== DELETE MODULE =====");
        listAllModules();
        
        System.out.print("Enter the ID of the module to delete: ");
        try {
            long id = Long.parseLong(scanner.nextLine().trim());
            
            System.out.print("Are you sure you want to delete this module? This cannot be undone. (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if ("y".equals(confirm) || "yes".equals(confirm)) {
                boolean success = moduleDAO.delete(id);
                if (success) {
                    System.out.println("Module deleted successfully.");
                } else {
                    System.out.println("Module not found or could not be deleted.");
                }
            } else {
                System.out.println("Deletion cancelled.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        } catch (Exception e) {
            System.out.println("Error deleting module: " + e.getMessage());
            logger.error("Error deleting module", e);
        }
    }
    
    // Task management menu
    private static void showTaskMenu() {
        boolean back = false;
        
        while (!back) {
            System.out.println("\n===== TASK MANAGEMENT =====");
            System.out.println("1. List tasks by module");
            System.out.println("2. Create open task");
            System.out.println("3. Create closed task");
            System.out.println("4. Update task");
            System.out.println("5. Delete task");
            System.out.println("9. Back to main menu");
            
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    listTasksByModule();
                    break;
                case "2":
                    createOpenTask();
                    break;
                case "3":
                    createClosedTask();
                    break;
                case "4":
                    updateTask();
                    break;
                case "5":
                    deleteTask();
                    break;
                case "9":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void listTasksByModule() {
        System.out.println("\n===== TASKS BY MODULE =====");
        listAllModules();
        
        System.out.print("Enter the module ID to see its tasks: ");
        try {
            long moduleId = Long.parseLong(scanner.nextLine().trim());
            List<Task> tasks = taskDAO.findByModule(moduleId);
            
            if (tasks.isEmpty()) {
                System.out.println("No tasks found for this module.");
                return;
            }
            
            System.out.println("ID\tName\tType\tBloom Level\tEstimated Time");
            for (Task task : tasks) {
                String taskType = task instanceof OpenTask ? "Open" : "Closed";
                
                System.out.printf("%d\t%s\t%s\t%s\t%d min\n", 
                    task.getId(), 
                    task.getName(), 
                    taskType,
                    task.getBloomLevel().getName(),
                    task.getEstimatedTimeMinutes());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        } catch (Exception e) {
            System.out.println("Error retrieving tasks: " + e.getMessage());
            logger.error("Error retrieving tasks", e);
        }
    }
    
    private static void createOpenTask() {
        System.out.println("\n===== CREATE OPEN TASK =====");
        
        Module module = selectModule();
        if (module == null) return;
        
        System.out.print("Task name: ");
        String name = scanner.nextLine().trim();
        
        System.out.print("Task text/question: ");
        String text = scanner.nextLine().trim();
        
        System.out.print("Estimated time in minutes: ");
        int estimatedTime = Integer.parseInt(scanner.nextLine().trim());
        
        BloomLevel bloomLevel = selectBloomLevel();
        if (bloomLevel == null) return;
        
        System.out.print("Sample solution: ");
        String sampleSolution = scanner.nextLine().trim();
        
        OpenTask task = new OpenTask();
        task.setName(name);
        task.setText(text);
        task.setEstimatedTimeMinutes(estimatedTime);
        task.setBloomLevel(bloomLevel);
        task.setModule(module);
        task.setSampleSolution(sampleSolution);
        
        try {
            task = (OpenTask) taskDAO.create(task);
            System.out.println("Open task created successfully with ID: " + task.getId());
        } catch (Exception e) {
            System.out.println("Error creating task: " + e.getMessage());
            logger.error("Error creating task", e);
        }
    }
    
    private static void createClosedTask() {
        System.out.println("\n===== CREATE CLOSED TASK =====");
        
        Module module = selectModule();
        if (module == null) return;
        
        System.out.print("Task name: ");
        String name = scanner.nextLine().trim();
        
        System.out.print("Task text/question: ");
        String text = scanner.nextLine().trim();
        
        System.out.print("Estimated time in minutes: ");
        int estimatedTime = Integer.parseInt(scanner.nextLine().trim());
        
        BloomLevel bloomLevel = selectBloomLevel();
        if (bloomLevel == null) return;
        
        ClosedTaskType taskType = selectClosedTaskType();
        if (taskType == null) return;
        
        ClosedTask task = new ClosedTask();
        task.setName(name);
        task.setText(text);
        task.setEstimatedTimeMinutes(estimatedTime);
        task.setBloomLevel(bloomLevel);
        task.setModule(module);
        task.setClosedTaskType(taskType);
        
        System.out.println("Enter answer options (empty line to finish):");
        int optionNum = 1;
        while (true) {
            System.out.print("Option " + optionNum + ": ");
            String option = scanner.nextLine().trim();
            if (option.isEmpty()) break;
            
            task.addOption(option);
            optionNum++;
        }
        
        System.out.print("Correct answer (format depends on type, e.g., option number or indices): ");
        String correctAnswer = scanner.nextLine().trim();
        task.setCorrectAnswer(correctAnswer);
        
        try {
            task = (ClosedTask) taskDAO.create(task);
            System.out.println("Closed task created successfully with ID: " + task.getId());
        } catch (Exception e) {
            System.out.println("Error creating task: " + e.getMessage());
            logger.error("Error creating task", e);
        }
    }
    
    private static void updateTask() {
        System.out.println("\n===== UPDATE TASK =====");
        listAllModules();
        
        System.out.print("Enter the module ID to see its tasks: ");
        try {
            long moduleId = Long.parseLong(scanner.nextLine().trim());
            List<Task> tasks = taskDAO.findByModule(moduleId);
            
            if (tasks.isEmpty()) {
                System.out.println("No tasks found for this module.");
                return;
            }
            
            System.out.println("ID\tName\tType");
            for (Task task : tasks) {
                String taskType = task instanceof OpenTask ? "Open" : "Closed";
                System.out.printf("%d\t%s\t%s\n", task.getId(), task.getName(), taskType);
            }
            
            System.out.print("Enter the ID of the task to update: ");
            long taskId = Long.parseLong(scanner.nextLine().trim());
            Optional<Task> taskOpt = taskDAO.findById(taskId);
            
            if (!taskOpt.isPresent()) {
                System.out.println("Task not found.");
                return;
            }
            
            Task task = taskOpt.get();
            
            System.out.print("Task name [" + task.getName() + "]: ");
            String name = scanner.nextLine().trim();
            if (!name.isEmpty()) {
                task.setName(name);
            }
            
            System.out.print("Task text [" + task.getText() + "]: ");
            String text = scanner.nextLine().trim();
            if (!text.isEmpty()) {
                task.setText(text);
            }
            
            System.out.print("Estimated time in minutes [" + task.getEstimatedTimeMinutes() + "]: ");
            String timeStr = scanner.nextLine().trim();
            if (!timeStr.isEmpty()) {
                task.setEstimatedTimeMinutes(Integer.parseInt(timeStr));
            }
            
            if (task instanceof OpenTask) {
                OpenTask openTask = (OpenTask) task;
                System.out.print("Sample solution [" + openTask.getSampleSolution() + "]: ");
                String solution = scanner.nextLine().trim();
                if (!solution.isEmpty()) {
                    openTask.setSampleSolution(solution);
                }
                
                taskDAO.update(openTask);
            } else if (task instanceof ClosedTask) {
                ClosedTask closedTask = (ClosedTask) task;
                System.out.println("Current options:");
                for (int i = 0; i < closedTask.getOptions().size(); i++) {
                    System.out.println((i+1) + ". " + closedTask.getOptions().get(i));
                }
                
                System.out.println("Do you want to update options? (y/n)");
                String choice = scanner.nextLine().trim().toLowerCase();
                
                if ("y".equals(choice) || "yes".equals(choice)) {
                    List<String> options = closedTask.getOptions();
                    options.clear();
                    
                    System.out.println("Enter new options (empty line to finish):");
                    int optionNum = 1;
                    while (true) {
                        System.out.print("Option " + optionNum + ": ");
                        String option = scanner.nextLine().trim();
                        if (option.isEmpty()) break;
                        
                        options.add(option);
                        optionNum++;
                    }
                }
                
                System.out.print("Correct answer [" + closedTask.getCorrectAnswer() + "]: ");
                String correctAnswer = scanner.nextLine().trim();
                if (!correctAnswer.isEmpty()) {
                    closedTask.setCorrectAnswer(correctAnswer);
                }
                
                taskDAO.update(closedTask);
            }
            
            System.out.println("Task updated successfully.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (Exception e) {
            System.out.println("Error updating task: " + e.getMessage());
            logger.error("Error updating task", e);
        }
    }
    
    private static void deleteTask() {
        System.out.println("\n===== DELETE TASK =====");
        listAllModules();
        
        System.out.print("Enter the module ID to see its tasks: ");
        try {
            long moduleId = Long.parseLong(scanner.nextLine().trim());
            List<Task> tasks = taskDAO.findByModule(moduleId);
            
            if (tasks.isEmpty()) {
                System.out.println("No tasks found for this module.");
                return;
            }
            
            System.out.println("ID\tName\tType");
            for (Task task : tasks) {
                String taskType = task instanceof OpenTask ? "Open" : "Closed";
                System.out.printf("%d\t%s\t%s\n", task.getId(), task.getName(), taskType);
            }
            
            System.out.print("Enter the ID of the task to delete: ");
            long taskId = Long.parseLong(scanner.nextLine().trim());
            
            System.out.print("Are you sure you want to delete this task? This cannot be undone. (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if ("y".equals(confirm) || "yes".equals(confirm)) {
                boolean success = taskDAO.delete(taskId);
                if (success) {
                    System.out.println("Task deleted successfully.");
                } else {
                    System.out.println("Task not found or could not be deleted.");
                }
            } else {
                System.out.println("Deletion cancelled.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        } catch (Exception e) {
            System.out.println("Error deleting task: " + e.getMessage());
            logger.error("Error deleting task", e);
        }
    }
    
    // Exam management menu
    private static void showExamMenu() {
        boolean back = false;
        
        while (!back) {
            System.out.println("\n===== EXAM MANAGEMENT =====");
            System.out.println("1. List all exams");
            System.out.println("2. List exams by module");
            System.out.println("3. Create new exam");
            System.out.println("4. Update exam");
            System.out.println("5. Delete exam");
            System.out.println("9. Back to main menu");
            
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    listAllExams();
                    break;
                case "2":
                    listExamsByModule();
                    break;
                case "3":
                    createExam();
                    break;
                case "4":
                    updateExam();
                    break;
                case "5":
                    deleteExam();
                    break;
                case "9":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void listAllExams() {
        System.out.println("\n===== ALL EXAMS =====");
        List<Exam> exams = examDAO.findAll();
        
        if (exams.isEmpty()) {
            System.out.println("No exams found.");
            return;
        }
        
        System.out.println("ID\tTitle\tModule\tDate\tDuration\tTasks");
        for (Exam exam : exams) {
            System.out.printf("%d\t%s\t%s\t%s\t%d min\t%d tasks\n", 
                exam.getId(), 
                exam.getTitle(), 
                exam.getModule().getName(),
                exam.getExamDate() != null ? exam.getExamDate().toString() : "-",
                exam.getDurationMinutes(),
                exam.getTasks().size());
        }
    }
    
    private static void listExamsByModule() {
        System.out.println("\n===== EXAMS BY MODULE =====");
        listAllModules();
        
        System.out.print("Enter the module ID to see its exams: ");
        try {
            long moduleId = Long.parseLong(scanner.nextLine().trim());
            List<Exam> exams = examDAO.findByModule(moduleId);
            
            if (exams.isEmpty()) {
                System.out.println("No exams found for this module.");
                return;
            }
            
            System.out.println("ID\tTitle\tDate\tDuration\tTasks");
            for (Exam exam : exams) {
                System.out.printf("%d\t%s\t%s\t%d min\t%d tasks\n", 
                    exam.getId(), 
                    exam.getTitle(),
                    exam.getExamDate() != null ? exam.getExamDate().toString() : "-",
                    exam.getDurationMinutes(),
                    exam.getTasks().size());
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        } catch (Exception e) {
            System.out.println("Error retrieving exams: " + e.getMessage());
            logger.error("Error retrieving exams", e);
        }
    }
    
    private static void createExam() {
        System.out.println("\n===== CREATE EXAM =====");
        
        Module module = selectModule();
        if (module == null) return;
        
        System.out.print("Exam title: ");
        String title = scanner.nextLine().trim();
        
        System.out.print("Exam description: ");
        String description = scanner.nextLine().trim();
        
        System.out.print("Exam date (YYYY-MM-DD): ");
        String dateStr = scanner.nextLine().trim();
        LocalDate examDate = null;
        if (!dateStr.isEmpty()) {
            try {
                examDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Using no date.");
            }
        }
        
        System.out.print("Duration in minutes: ");
        int duration = Integer.parseInt(scanner.nextLine().trim());
        
        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setDescription(description);
        exam.setExamDate(examDate);
        exam.setDurationMinutes(duration);
        exam.setModule(module);
        
        // Add tasks to the exam
        List<Task> moduleTasks = taskDAO.findByModule(module.getId());
        if (moduleTasks.isEmpty()) {
            System.out.println("No tasks available for this module. Create tasks first.");
            return;
        }
        
        System.out.println("Available tasks:");
        System.out.println("ID\tName\tType\tBloom Level\tTime");
        for (Task task : moduleTasks) {
            String taskType = task instanceof OpenTask ? "Open" : "Closed";
            System.out.printf("%d\t%s\t%s\t%s\t%d min\n", 
                task.getId(), 
                task.getName(), 
                taskType,
                task.getBloomLevel().getName(),
                task.getEstimatedTimeMinutes());
        }
        
        System.out.println("Enter task IDs to add to this exam (comma-separated, e.g., 1,3,5): ");
        String taskIdsStr = scanner.nextLine().trim();
        if (!taskIdsStr.isEmpty()) {
            String[] taskIdArr = taskIdsStr.split(",");
            for (String taskIdStr : taskIdArr) {
                try {
                    long taskId = Long.parseLong(taskIdStr.trim());
                    Optional<Task> taskOpt = taskDAO.findById(taskId);
                    if (taskOpt.isPresent()) {
                        exam.addTask(taskOpt.get());
                    } else {
                        System.out.println("Task ID " + taskId + " not found, skipping.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid ID format: " + taskIdStr + ", skipping.");
                }
            }
        }
        
        try {
            exam = examDAO.create(exam);
            System.out.println("Exam created successfully with ID: " + exam.getId());
            System.out.println("Total estimated time: " + exam.getTotalEstimatedTime() + " minutes");
        } catch (Exception e) {
            System.out.println("Error creating exam: " + e.getMessage());
            logger.error("Error creating exam", e);
        }
    }
    
    private static void updateExam() {
        System.out.println("\n===== UPDATE EXAM =====");
        listAllExams();
        
        System.out.print("Enter the ID of the exam to update: ");
        try {
            long id = Long.parseLong(scanner.nextLine().trim());
            Optional<Exam> examOpt = examDAO.findById(id);
            
            if (!examOpt.isPresent()) {
                System.out.println("Exam not found.");
                return;
            }
            
            Exam exam = examOpt.get();
            
            System.out.print("Exam title [" + exam.getTitle() + "]: ");
            String title = scanner.nextLine().trim();
            if (!title.isEmpty()) {
                exam.setTitle(title);
            }
            
            System.out.print("Exam description [" + exam.getDescription() + "]: ");
            String description = scanner.nextLine().trim();
            if (!description.isEmpty()) {
                exam.setDescription(description);
            }
            
            System.out.print("Exam date (" + 
                (exam.getExamDate() != null ? exam.getExamDate().toString() : "none") + 
                ") [YYYY-MM-DD]: ");
            String dateStr = scanner.nextLine().trim();
            if (!dateStr.isEmpty()) {
                try {
                    exam.setExamDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format. Keeping existing date.");
                }
            }
            
            System.out.print("Duration in minutes [" + exam.getDurationMinutes() + "]: ");
            String durationStr = scanner.nextLine().trim();
            if (!durationStr.isEmpty()) {
                exam.setDurationMinutes(Integer.parseInt(durationStr));
            }
            
            System.out.println("Current tasks in this exam:");
            if (exam.getTasks().isEmpty()) {
                System.out.println("No tasks in this exam.");
            } else {
                for (Task task : exam.getTasks()) {
                    System.out.printf("%d: %s\n", task.getId(), task.getName());
                }
            }
            
            System.out.println("Do you want to update the task list? (y/n)");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if ("y".equals(choice) || "yes".equals(choice)) {
                // Show available tasks for the module
                List<Task> moduleTasks = taskDAO.findByModule(exam.getModule().getId());
                
                System.out.println("Available tasks for this module:");
                System.out.println("ID\tName\tType");
                for (Task task : moduleTasks) {
                    String taskType = task instanceof OpenTask ? "Open" : "Closed";
                    System.out.printf("%d\t%s\t%s\n", task.getId(), task.getName(), taskType);
                }
                
                System.out.println("Enter task IDs to add to this exam (comma-separated, e.g., 1,3,5): ");
                String taskIdsStr = scanner.nextLine().trim();
                
                // Clear existing tasks and add new ones
                exam.getTasks().clear();
                
                if (!taskIdsStr.isEmpty()) {
                    String[] taskIdArr = taskIdsStr.split(",");
                    for (String taskIdStr : taskIdArr) {
                        try {
                            long taskId = Long.parseLong(taskIdStr.trim());
                            Optional<Task> taskOpt = taskDAO.findById(taskId);
                            if (taskOpt.isPresent()) {
                                exam.addTask(taskOpt.get());
                            } else {
                                System.out.println("Task ID " + taskId + " not found, skipping.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid ID format: " + taskIdStr + ", skipping.");
                        }
                    }
                }
            }
            
            examDAO.update(exam);
            System.out.println("Exam updated successfully.");
            System.out.println("Total estimated time: " + exam.getTotalEstimatedTime() + " minutes");
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (Exception e) {
            System.out.println("Error updating exam: " + e.getMessage());
            logger.error("Error updating exam", e);
        }
    }
    
    private static void deleteExam() {
        System.out.println("\n===== DELETE EXAM =====");
        listAllExams();
        
        System.out.print("Enter the ID of the exam to delete: ");
        try {
            long id = Long.parseLong(scanner.nextLine().trim());
            
            System.out.print("Are you sure you want to delete this exam? This cannot be undone. (y/n): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if ("y".equals(confirm) || "yes".equals(confirm)) {
                boolean success = examDAO.delete(id);
                if (success) {
                    System.out.println("Exam deleted successfully.");
                } else {
                    System.out.println("Exam not found or could not be deleted.");
                }
            } else {
                System.out.println("Deletion cancelled.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
        } catch (Exception e) {
            System.out.println("Error deleting exam: " + e.getMessage());
            logger.error("Error deleting exam", e);
        }
    }
    
    // Answer management menu
    private static void showAnswerMenu() {
        boolean back = false;
        
        while (!back) {
            System.out.println("\n===== ANSWER MANAGEMENT =====");
            System.out.println("1. Show answers for a task");
            System.out.println("2. Grade an answer");
            System.out.println("3. Submit answer to a task");
            System.out.println("9. Back to main menu");
            
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    showAnswersForTask();
                    break;
                case "2":
                    gradeAnswer();
                    break;
                case "3":
                    submitAnswer();
                    break;
                case "9":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void showAnswersForTask() {
        System.out.println("\n===== ANSWERS FOR TASK =====");
        
        Task task = selectTask();
        if (task == null) return;
        
        List<Answer> answers = answerDAO.findByTaskId(task.getId());
        
        if (answers.isEmpty()) {
            System.out.println("No answers found for this task.");
            return;
        }
        
        System.out.println("ID\tUser ID\tSubmission Time\tGraded\tScore\tContent");
        for (Answer answer : answers) {
            System.out.printf("%d\t%d\t%s\t%s\t%s\t%s\n", 
                answer.getId(), 
                answer.getUserId(),
                answer.getSubmissionTime().toString(),
                answer.isGraded() ? "Yes" : "No",
                answer.getScore() != null ? answer.getScore().toString() : "-",
                answer.getAnswerContent().length() > 30 ? 
                    answer.getAnswerContent().substring(0, 30) + "..." : 
                    answer.getAnswerContent());
        }
    }
    
    private static void gradeAnswer() {
        System.out.println("\n===== GRADE ANSWER =====");
        
        System.out.print("Enter answer ID to grade: ");
        try {
            long answerId = Long.parseLong(scanner.nextLine().trim());
            Optional<Answer> answerOpt = answerDAO.findById(answerId);
            
            if (!answerOpt.isPresent()) {
                System.out.println("Answer not found.");
                return;
            }
            
            Answer answer = answerOpt.get();
            
            // Get the associated task
            Optional<Task> taskOpt = taskDAO.findById(answer.getTaskId());
            if (!taskOpt.isPresent()) {
                System.out.println("Associated task not found.");
                return;
            }
            
            Task task = taskOpt.get();
            
            System.out.println("Task: " + task.getName());
            System.out.println("Task text: " + task.getText());
            System.out.println("Task solution: " + task.getSolution());
            System.out.println("Answer content: " + answer.getAnswerContent());
            
            if (answer instanceof ClosedAnswer) {
                boolean isCorrect = answer.isCorrect(task);
                System.out.println("Auto-evaluation: " + (isCorrect ? "CORRECT" : "INCORRECT"));
            }
            
            System.out.print("Enter score (0.0-100.0): ");
            double score = Double.parseDouble(scanner.nextLine().trim());
            
            System.out.print("Enter feedback: ");
            String feedback = scanner.nextLine().trim();
            
            answer.setScore(score);
            answer.setFeedback(feedback);
            answer.setGraded(true);
            
            answerDAO.update(answer);
            System.out.println("Answer graded successfully.");
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (Exception e) {
            System.out.println("Error grading answer: " + e.getMessage());
            logger.error("Error grading answer", e);
        }
    }
    
    private static void submitAnswer() {
        System.out.println("\n===== SUBMIT ANSWER =====");
        
        Task task = selectTask();
        if (task == null) return;
        
        NutzerKonto currentUser = authService.getCurrentUser();
        
        if (task instanceof OpenTask) {
            System.out.print("Enter your answer text: ");
            String text = scanner.nextLine().trim();
            
            OpenAnswer answer = new OpenAnswer();
            answer.setTaskId(task.getId());
            answer.setUserId(currentUser.getId());
            answer.setText(text);
            
            try {
                answerDAO.create(answer);
                System.out.println("Answer submitted successfully.");
            } catch (Exception e) {
                System.out.println("Error submitting answer: " + e.getMessage());
                logger.error("Error submitting answer", e);
            }
        } else if (task instanceof ClosedTask) {
            ClosedTask closedTask = (ClosedTask) task;
            
            System.out.println("Options:");
            for (int i = 0; i < closedTask.getOptions().size(); i++) {
                System.out.println((i+1) + ". " + closedTask.getOptions().get(i));
            }
            
            System.out.print("Enter your answer (format depends on task type): ");
            String selectedOption = scanner.nextLine().trim();
            
            ClosedAnswer answer = new ClosedAnswer();
            answer.setTaskId(task.getId());
            answer.setUserId(currentUser.getId());
            answer.setSelectedOption(selectedOption);
            
            try {
                answerDAO.create(answer);
                System.out.println("Answer submitted successfully.");
                
                // Check if the answer is correct
                if (answer.isCorrect(task)) {
                    System.out.println("Your answer is correct!");
                } else {
                    System.out.println("Your answer is incorrect.");
                }
            } catch (Exception e) {
                System.out.println("Error submitting answer: " + e.getMessage());
                logger.error("Error submitting answer", e);
            }
        }
    }
    
    // Helper methods for selection
    private static Module selectModule() {
        listAllModules();
        
        System.out.print("Enter module ID: ");
        try {
            long moduleId = Long.parseLong(scanner.nextLine().trim());
            Optional<Module> moduleOpt = moduleDAO.findById(moduleId);
            
            if (moduleOpt.isPresent()) {
                return moduleOpt.get();
            } else {
                System.out.println("Module not found.");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
            return null;
        }
    }
    
    private static BloomLevel selectBloomLevel() {
        System.out.println("Bloom Taxonomy Levels:");
        for (BloomLevel level : BloomLevel.values()) {
            System.out.printf("%d. %s - %s\n", 
                level.getLevel(), 
                level.getName(), 
                level.getDescription());
        }
        
        System.out.print("Enter level number (1-6): ");
        try {
            int level = Integer.parseInt(scanner.nextLine().trim());
            for (BloomLevel bloomLevel : BloomLevel.values()) {
                if (bloomLevel.getLevel() == level) {
                    return bloomLevel;
                }
            }
            System.out.println("Invalid level. Using REMEMBER as default.");
            return BloomLevel.REMEMBER;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Using REMEMBER as default.");
            return BloomLevel.REMEMBER;
        }
    }
    
    private static ClosedTaskType selectClosedTaskType() {
        System.out.println("Closed Task Types:");
        ClosedTaskType[] types = ClosedTaskType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("%d. %s - %s\n", 
                i+1, 
                types[i].getName(), 
                types[i].getDescription());
        }
        
        System.out.print("Enter type number (1-" + types.length + "): ");
        try {
            int typeNum = Integer.parseInt(scanner.nextLine().trim());
            if (typeNum >= 1 && typeNum <= types.length) {
                return types[typeNum-1];
            } else {
                System.out.println("Invalid type. Using SINGLE_CHOICE as default.");
                return ClosedTaskType.SINGLE_CHOICE;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Using SINGLE_CHOICE as default.");
            return ClosedTaskType.SINGLE_CHOICE;
        }
    }
    
    private static Task selectTask() {
        Module module = selectModule();
        if (module == null) return null;
        
        List<Task> tasks = taskDAO.findByModule(module.getId());
        
        if (tasks.isEmpty()) {
            System.out.println("No tasks found for this module.");
            return null;
        }
        
        System.out.println("ID\tName\tType");
        for (Task task : tasks) {
            String taskType = task instanceof OpenTask ? "Open" : "Closed";
            System.out.printf("%d\t%s\t%s\n", task.getId(), task.getName(), taskType);
        }
        
        System.out.print("Enter task ID: ");
        try {
            long taskId = Long.parseLong(scanner.nextLine().trim());
            Optional<Task> taskOpt = taskDAO.findById(taskId);
            
            if (taskOpt.isPresent()) {
                return taskOpt.get();
            } else {
                System.out.println("Task not found.");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format.");
            return null;
        }
    }
}