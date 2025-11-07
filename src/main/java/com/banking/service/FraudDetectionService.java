package com.banking.service;

import com.banking.model.FraudAlert;
import com.banking.model.Transaction;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FraudDetectionService {
    private final Semaphore fraudCheckSemaphore;
    private final BlockingQueue<FraudAlert> fraudAlerts;
    private final AtomicInteger totalFraudChecks;
    private final AtomicInteger fraudAlertsGenerated;
    private final AtomicReference<String> lastFraudReason;
    private final ExecutorService alertProcessorExecutor;
    private volatile boolean running;
    
    public FraudDetectionService() {
        this.fraudCheckSemaphore = new Semaphore(3); // Limit to 3 concurrent fraud checks
        this.fraudAlerts = new LinkedBlockingQueue<>();
        this.totalFraudChecks = new AtomicInteger(0);
        this.fraudAlertsGenerated = new AtomicInteger(0);
        this.lastFraudReason = new AtomicReference<>("None");
        this.alertProcessorExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "FraudAlertProcessor-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        this.running = true;
        
        // Start alert processing threads
        startAlertProcessors();
    }
    
    public boolean checkTransaction(Transaction transaction) {
        totalFraudChecks.incrementAndGet();
        
        try {
            // Acquire semaphore to limit concurrent fraud checks
            fraudCheckSemaphore.acquire();
            
            System.out.printf("[%s] Starting fraud check for transaction %s (Available permits: %d)%n", 
                    Thread.currentThread().getName(), 
                    transaction.getTransactionId(),
                    fraudCheckSemaphore.availablePermits());
            
            try {
                // Simulate fraud detection processing time
                Thread.sleep(200);
                
                // Simple fraud detection rules
                boolean isFraudulent = false;
                String reason = null;
                
                if (transaction.getAmount() > 10000) {
                    isFraudulent = true;
                    reason = "Large amount transaction: " + transaction.getAmount();
                } else if (transaction.getAmount() > 5000 && Math.random() < 0.3) {
                    isFraudulent = true;
                    reason = "Suspicious pattern detected";
                }
                
                if (isFraudulent) {
                    FraudAlert alert = new FraudAlert(
                            transaction.getFromAccount() != null ? transaction.getFromAccount() : transaction.getToAccount(),
                            reason,
                            transaction.getAmount()
                    );
                    
                    reportSuspiciousActivity(alert);
                    lastFraudReason.set(reason);
                    
                    System.out.printf("[%s] FRAUD DETECTED for transaction %s: %s%n", 
                            Thread.currentThread().getName(), 
                            transaction.getTransactionId(), 
                            reason);
                    
                    return false;
                }
                
                System.out.printf("[%s] Transaction %s passed fraud check%n", 
                        Thread.currentThread().getName(), 
                        transaction.getTransactionId());
                
                return true;
                
            } finally {
                fraudCheckSemaphore.release();
                System.out.printf("[%s] Released fraud check permit (Available permits: %d)%n", 
                        Thread.currentThread().getName(), 
                        fraudCheckSemaphore.availablePermits());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("[%s] Fraud check interrupted for transaction %s%n", 
                    Thread.currentThread().getName(), 
                    transaction.getTransactionId());
            return false;
        }
    }
    
    // Producer method - adds alerts to the queue
    public void reportSuspiciousActivity(FraudAlert alert) {
        try {
            fraudAlerts.put(alert); // Blocking put
            fraudAlertsGenerated.incrementAndGet();
            
            System.out.printf("[%s] Fraud alert queued: %s (Queue size: %d)%n", 
                    Thread.currentThread().getName(), 
                    alert.getAlertId(), 
                    fraudAlerts.size());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("[%s] Failed to queue fraud alert%n", Thread.currentThread().getName());
        }
    }
    
    // Start consumer threads for processing fraud alerts
    private void startAlertProcessors() {
        for (int i = 0; i < 2; i++) {
            alertProcessorExecutor.submit(this::processFraudAlerts);
        }
    }
    
    // Consumer method - processes alerts from the queue
    private void processFraudAlerts() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Blocking take from queue
                FraudAlert alert = fraudAlerts.take();
                
                System.out.printf("[%s] Processing fraud alert: %s%n", 
                        Thread.currentThread().getName(), 
                        alert.getAlertId());
                
                // Simulate alert investigation
                investigateAlert(alert);
                
                System.out.printf("[%s] Completed processing fraud alert: %s%n", 
                        Thread.currentThread().getName(), 
                        alert.getAlertId());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.printf("[%s] Fraud alert processor interrupted%n", Thread.currentThread().getName());
                break;
            }
        }
    }
    
    private void investigateAlert(FraudAlert alert) {
        try {
            // Simulate investigation time
            Thread.sleep(300);
            
            System.out.printf("[%s] Investigated alert %s for account %s: %s%n", 
                    Thread.currentThread().getName(), 
                    alert.getAlertId(), 
                    alert.getAccountId(), 
                    alert.getReason());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void printStatistics() {
        System.out.println("\n=== Fraud Detection Statistics ===");
        System.out.printf("Total fraud checks: %d%n", totalFraudChecks.get());
        System.out.printf("Fraud alerts generated: %d%n", fraudAlertsGenerated.get());
        System.out.printf("Pending alerts in queue: %d%n", fraudAlerts.size());
        System.out.printf("Available fraud check permits: %d%n", fraudCheckSemaphore.availablePermits());
        System.out.printf("Last fraud reason: %s%n", lastFraudReason.get());
        System.out.println("=================================\n");
    }
    
    public void shutdown() {
        running = false;
        alertProcessorExecutor.shutdown();
        try {
            if (!alertProcessorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                alertProcessorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            alertProcessorExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Getters for monitoring
    public int getTotalFraudChecks() {
        return totalFraudChecks.get();
    }
    
    public int getFraudAlertsGenerated() {
        return fraudAlertsGenerated.get();
    }
    
    public int getPendingAlerts() {
        return fraudAlerts.size();
    }
}