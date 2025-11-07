package com.banking;

import com.banking.model.Transaction;
import com.banking.model.TransactionType;
import com.banking.service.*;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankingSystemCLI {
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AsyncTransactionService asyncTransactionService;
    private final FraudDetectionService fraudDetectionService;
    private final ReportService reportService;
    private final Scanner scanner;
    private final ExecutorService demoExecutor;
    
    public BankingSystemCLI() {
        this.accountService = new AccountService();
        this.transactionService = new TransactionService(accountService);
        this.asyncTransactionService = new AsyncTransactionService(accountService);
        this.fraudDetectionService = new FraudDetectionService();
        this.reportService = new ReportService(accountService);
        this.scanner = new Scanner(System.in);
        this.demoExecutor = Executors.newFixedThreadPool(5);
        
        // Initialize with some sample accounts
        initializeSampleAccounts();
    }
    
    private void initializeSampleAccounts() {
        accountService.createAccount("ACC001", 1000.0);
        accountService.createAccount("ACC002", 2500.0);
        accountService.createAccount("ACC003", 500.0);
        System.out.println("Sample accounts created!");
    }
    
    public void start() {
        System.out.println("=== Concurrent Banking System ===");
        System.out.println("Demonstrating Java Multithreading Concepts");
        System.out.println("=====================================\n");
        
        while (true) {
            printMenu();
            int choice = getChoice();
            
            try {
                switch (choice) {
                    case 1:
                        performBasicTransactions();
                        break;
                    case 2:
                        demonstrateAsyncTransactions();
                        break;
                    case 3:
                        demonstrateFraudDetection();
                        break;
                    case 4:
                        demonstrateReporting();
                        break;
                    case 5:
                        demonstrateScheduledOperations();
                        break;
                    case 6:
                        demonstrateConcurrentOperations();
                        break;
                    case 7:
                        showSystemStatus();
                        break;
                    case 0:
                        shutdown();
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private void printMenu() {
        System.out.println("\n=== Banking Operations Menu ===");
        System.out.println("1. Basic Transactions (ReentrantLock)");
        System.out.println("2. Async Transactions (CompletableFuture)");
        System.out.println("3. Fraud Detection (Semaphore + BlockingQueue)");
        System.out.println("4. Generate Reports (ReadWriteLock + StampedLock)");
        System.out.println("5. Scheduled Operations (ScheduledThreadPoolExecutor)");
        System.out.println("6. Concurrent Operations Demo");
        System.out.println("7. System Status");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }
    
    private int getChoice() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private void performBasicTransactions() {
        System.out.println("\n=== Basic Transactions Demo ===");
        
        try {
            // Demonstrate deposit with ReentrantLock
            accountService.deposit("ACC001", 200.0);
            
            // Demonstrate withdrawal with ReentrantLock
            accountService.withdraw("ACC002", 100.0);
            
            // Demonstrate transfer with ordered locking
            accountService.transfer("ACC001", "ACC003", 150.0);
            
            accountService.printAllAccounts();
            
        } catch (Exception e) {
            System.err.println("Transaction failed: " + e.getMessage());
        }
    }
    
    private void demonstrateAsyncTransactions() {
        System.out.println("\n=== Async Transactions Demo ===");
        
        Transaction t1 = new Transaction("ACC001", "ACC002", 100.0, TransactionType.TRANSFER);
        Transaction t2 = new Transaction(null, "ACC003", 50.0, TransactionType.DEPOSIT);
        Transaction t3 = new Transaction("ACC002", null, 75.0, TransactionType.WITHDRAWAL);
        
        System.out.println("Processing transactions asynchronously...");
        
        CompletableFuture<Transaction> future1 = asyncTransactionService.processTransactionAsync(t1);
        CompletableFuture<Transaction> future2 = asyncTransactionService.processTransactionAsync(t2);
        CompletableFuture<Transaction> future3 = asyncTransactionService.processTransactionAsync(t3);
        
        // Wait for all to complete
        CompletableFuture.allOf(future1, future2, future3)
                .thenRun(() -> {
                    System.out.println("All async transactions completed!");
                    accountService.printAllAccounts();
                })
                .join();
    }
    
    private void demonstrateFraudDetection() {
        System.out.println("\n=== Fraud Detection Demo ===");
        
        // Create transactions with different amounts to trigger fraud detection
        Transaction normalTx = new Transaction("ACC001", "ACC002", 500.0, TransactionType.TRANSFER);
        Transaction largeTx = new Transaction("ACC002", "ACC003", 15000.0, TransactionType.TRANSFER);
        Transaction mediumTx = new Transaction("ACC003", "ACC001", 7000.0, TransactionType.TRANSFER);
        
        System.out.println("Checking transactions for fraud...");
        
        // Submit fraud checks concurrently
        demoExecutor.submit(() -> {
            boolean result = fraudDetectionService.checkTransaction(normalTx);
            System.out.println("Normal transaction fraud check result: " + result);
        });
        
        demoExecutor.submit(() -> {
            boolean result = fraudDetectionService.checkTransaction(largeTx);
            System.out.println("Large transaction fraud check result: " + result);
        });
        
        demoExecutor.submit(() -> {
            boolean result = fraudDetectionService.checkTransaction(mediumTx);
            System.out.println("Medium transaction fraud check result: " + result);
        });
        
        // Wait a bit for processing
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        fraudDetectionService.printStatistics();
    }
    
    private void demonstrateReporting() {
        System.out.println("\n=== Reporting Demo ===");
        
        // Demonstrate concurrent report generation using ReadWriteLock
        System.out.println("Generating account reports concurrently...");
        
        for (int i = 0; i < 3; i++) {
            final String accountId = "ACC00" + (i + 1);
            demoExecutor.submit(() -> {
                String report = reportService.generateAccountReport(accountId);
                System.out.println(report);
            });
        }
        
        // Demonstrate statistics calculation using StampedLock
        demoExecutor.submit(() -> {
            String stats = reportService.calculateBankStatistics();
            System.out.println(stats);
        });
        
        // Wait for reports to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        reportService.printLockStatistics();
    }
    
    private void demonstrateScheduledOperations() {
        System.out.println("\n=== Scheduled Operations Demo ===");
        System.out.println("Demonstrating ScheduledThreadPoolExecutor functionality...");
        
        // Start periodic reporting every 2 seconds
        System.out.println("1. Starting periodic reporting (every 2 seconds)...");
        reportService.startPeriodicReporting(1, 2, java.util.concurrent.TimeUnit.SECONDS);
        
        // Start periodic maintenance every 3 seconds with 1 second delay between completions
        System.out.println("2. Starting periodic maintenance (every 3 seconds after completion)...");
        reportService.startPeriodicMaintenance(2, 3, java.util.concurrent.TimeUnit.SECONDS);
        
        // Schedule some delayed reports
        System.out.println("3. Scheduling delayed reports...");
        java.util.concurrent.ScheduledFuture<String> delayedReport1 = 
                reportService.scheduleDelayedReport("ACC001", 3, java.util.concurrent.TimeUnit.SECONDS);
        java.util.concurrent.ScheduledFuture<String> delayedReport2 = 
                reportService.scheduleDelayedReport("ACC002", 5, java.util.concurrent.TimeUnit.SECONDS);
        
        System.out.println("4. Letting scheduled operations run for 10 seconds...");
        System.out.println("   Watch the periodic reports and maintenance tasks execute!");
        
        try {
            // Let it run for 10 seconds to see multiple executions
            Thread.sleep(10000);
            
            // Try to get the delayed reports
            System.out.println("\n5. Retrieving delayed reports:");
            try {
                String report1 = delayedReport1.get(1, java.util.concurrent.TimeUnit.SECONDS);
                System.out.println("Delayed Report 1 completed:\n" + report1);
            } catch (java.util.concurrent.TimeoutException e) {
                System.out.println("Delayed Report 1 still pending...");
            }
            
            try {
                String report2 = delayedReport2.get(1, java.util.concurrent.TimeUnit.SECONDS);
                System.out.println("Delayed Report 2 completed:\n" + report2);
            } catch (java.util.concurrent.TimeoutException e) {
                System.out.println("Delayed Report 2 still pending...");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error retrieving delayed reports: " + e.getMessage());
        }
        
        // Stop periodic reporting for this demo
        System.out.println("\n6. Stopping periodic reporting...");
        reportService.stopPeriodicReporting();
        
        // Show scheduled operations statistics
        System.out.println("\n7. Scheduled Operations Statistics:");
        System.out.println(reportService.getScheduledOperationsStatistics());
        
        System.out.println("\nScheduled operations demo completed!");
        System.out.println("Note: Maintenance tasks will continue running until system shutdown.");
    }
    
    private void demonstrateConcurrentOperations() {
        System.out.println("\n=== Concurrent Operations Demo ===");
        System.out.println("Running multiple operations simultaneously...");
        
        // Submit various concurrent operations
        for (int i = 0; i < 5; i++) {
            final int iteration = i;
            
            // Concurrent deposits
            demoExecutor.submit(() -> {
                try {
                    accountService.deposit("ACC001", 10.0);
                } catch (Exception e) {
                    System.err.println("Deposit failed: " + e.getMessage());
                }
            });
            
            // Concurrent withdrawals
            demoExecutor.submit(() -> {
                try {
                    accountService.withdraw("ACC002", 5.0);
                } catch (Exception e) {
                    System.err.println("Withdrawal failed: " + e.getMessage());
                }
            });
            
            // Concurrent transfers
            demoExecutor.submit(() -> {
                try {
                    accountService.transfer("ACC003", "ACC001", 20.0);
                } catch (Exception e) {
                    System.err.println("Transfer failed: " + e.getMessage());
                }
            });
            
            // Concurrent report generation
            demoExecutor.submit(() -> {
                reportService.generateAccountReport("ACC00" + ((iteration % 3) + 1));
            });
        }
        
        // Wait for operations to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Concurrent operations completed!");
        accountService.printAllAccounts();
    }
    
    private void showSystemStatus() {
        System.out.println("\n=== System Status ===");
        
        accountService.printAllAccounts();
        transactionService.printExecutorStats();
        fraudDetectionService.printStatistics();
        reportService.printLockStatistics();
        
        System.out.printf("Demo executor active threads: %d%n", 
                ((java.util.concurrent.ThreadPoolExecutor) demoExecutor).getActiveCount());
    }
    
    private void shutdown() {
        System.out.println("\nShutting down banking system...");
        
        // Shutdown all services with proper executor lifecycle management
        System.out.println("Shutting down TransactionService...");
        transactionService.shutdown();
        
        System.out.println("Shutting down FraudDetectionService...");
        fraudDetectionService.shutdown();
        
        System.out.println("Shutting down ReportService (including ScheduledThreadPoolExecutor)...");
        reportService.shutdown();
        
        System.out.println("Shutting down demo executor...");
        demoExecutor.shutdown();
        
        try {
            if (!demoExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("Demo executor did not terminate gracefully, forcing shutdown...");
                demoExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            demoExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Show final statistics
        System.out.println("\n=== Final System Statistics ===");
        System.out.println(reportService.getScheduledOperationsStatistics());
        
        System.out.println("Banking system shutdown complete.");
        System.out.println("Final account status:");
        accountService.printAllAccounts();
    }
    
    public static void main(String[] args) {
        BankingSystemCLI cli = new BankingSystemCLI();
        cli.start();
    }
}