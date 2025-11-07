package com.banking.service;

import com.banking.model.Transaction;
import com.banking.model.TransactionStatus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AsyncTransactionService {
    private final AccountService accountService;
    private final Executor validationExecutor;
    private final Executor enrichmentExecutor;
    private final Executor fraudExecutor;
    private final Executor notificationExecutor;
    
    public AsyncTransactionService(AccountService accountService) {
        this.accountService = accountService;
        this.validationExecutor = Executors.newFixedThreadPool(3, createThreadFactory("Validation"));
        this.enrichmentExecutor = Executors.newFixedThreadPool(2, createThreadFactory("Enrichment"));
        this.fraudExecutor = Executors.newFixedThreadPool(2, createThreadFactory("Fraud"));
        this.notificationExecutor = Executors.newFixedThreadPool(2, createThreadFactory("Notification"));
    }
    
    private ThreadFactory createThreadFactory(String prefix) {
        return new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, prefix + "Worker-" + (++counter));
            }
        };
    }
    
    public CompletableFuture<Transaction> processTransactionAsync(Transaction transaction) {
        return CompletableFuture
                // Step 1: Validate transaction asynchronously
                .supplyAsync(() -> {
                    System.out.printf("[%s] Starting validation for %s%n", 
                            Thread.currentThread().getName(), transaction.getTransactionId());
                    return validateTransaction(transaction);
                }, validationExecutor)
                
                // Step 2: Transform validation result using thenApply
                .thenApply(isValid -> {
                    System.out.printf("[%s] Validation result: %s for %s%n", 
                            Thread.currentThread().getName(), isValid, transaction.getTransactionId());
                    if (!isValid) {
                        transaction.setStatus(TransactionStatus.FAILED);
                        return null;
                    }
                    return transaction;
                })
                
                // Step 3: Enrich transaction data asynchronously using thenApplyAsync
                .thenApplyAsync(trans -> {
                    if (trans == null) return null;
                    System.out.printf("[%s] Enriching transaction %s%n", 
                            Thread.currentThread().getName(), trans.getTransactionId());
                    return enrichTransactionData(trans);
                }, enrichmentExecutor)
                
                // Step 4: Chain fraud check using thenCompose
                .thenCompose(trans -> {
                    if (trans == null) return CompletableFuture.completedFuture(null);
                    return checkFraudAsync(trans);
                })
                
                // Step 5: Combine with account balance check using thenCombine
                .thenCombine(
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return accountService.getBalance(transaction.getFromAccount());
                        } catch (Exception e) {
                            return 0.0;
                        }
                    }),
                    (trans, balance) -> {
                        if (trans == null) return null;
                        System.out.printf("[%s] Processing transaction %s with balance %.2f%n", 
                                Thread.currentThread().getName(), trans.getTransactionId(), balance);
                        return processWithBalance(trans, balance);
                    }
                )
                
                // Step 6: Execute the actual transaction using thenCombineAsync
                .thenCombineAsync(
                    CompletableFuture.supplyAsync(() -> "SYSTEM_APPROVED"),
                    (trans, approval) -> {
                        if (trans == null) return null;
                        System.out.printf("[%s] Executing transaction %s with approval %s%n", 
                                Thread.currentThread().getName(), trans.getTransactionId(), approval);
                        return executeTransactionStep(trans);
                    },
                    fraudExecutor
                )
                
                // Step 7: Log result using thenAccept
                .thenAccept(trans -> {
                    if (trans != null) {
                        System.out.printf("[%s] Transaction %s completed with status %s%n", 
                                Thread.currentThread().getName(), trans.getTransactionId(), trans.getStatus());
                    }
                })
                
                // Step 8: Send notification using thenAcceptAsync
                .thenAcceptAsync(result -> {
                    System.out.printf("[%s] Sending notification for transaction %s%n", 
                            Thread.currentThread().getName(), transaction.getTransactionId());
                    // Simulate notification
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, notificationExecutor)
                
                // Return the original transaction
                .thenApply(v -> transaction)
                
                // Handle exceptions
                .exceptionally(throwable -> {
                    System.err.printf("[%s] Transaction %s failed: %s%n", 
                            Thread.currentThread().getName(), transaction.getTransactionId(), throwable.getMessage());
                    transaction.setStatus(TransactionStatus.FAILED);
                    return transaction;
                });
    }
    
    private boolean validateTransaction(Transaction transaction) {
        // Simulate validation work
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return transaction.getAmount() > 0;
    }
    
    private Transaction enrichTransactionData(Transaction transaction) {
        // Simulate enrichment work
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return transaction;
    }
    
    private CompletableFuture<Transaction> checkFraudAsync(Transaction transaction) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.printf("[%s] Checking fraud for %s%n", 
                    Thread.currentThread().getName(), transaction.getTransactionId());
            
            // Simulate fraud check
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                transaction.setStatus(TransactionStatus.FAILED);
                return transaction;
            }
            
            // Simple fraud detection - flag large amounts
            if (transaction.getAmount() > 10000) {
                transaction.setStatus(TransactionStatus.FRAUD_DETECTED);
                return transaction;
            }
            
            return transaction;
        }, fraudExecutor);
    }
    
    private Transaction processWithBalance(Transaction transaction, double balance) {
        if (transaction.getAmount() > balance) {
            transaction.setStatus(TransactionStatus.FAILED);
            return transaction;
        }
        transaction.setStatus(TransactionStatus.PROCESSING);
        return transaction;
    }
    
    private Transaction executeTransactionStep(Transaction transaction) {
        try {
            switch (transaction.getType()) {
                case DEPOSIT:
                    accountService.deposit(transaction.getToAccount(), transaction.getAmount());
                    break;
                case WITHDRAWAL:
                    accountService.withdraw(transaction.getFromAccount(), transaction.getAmount());
                    break;
                case TRANSFER:
                    accountService.transfer(transaction.getFromAccount(), 
                                          transaction.getToAccount(), 
                                          transaction.getAmount());
                    break;
            }
            transaction.setStatus(TransactionStatus.COMPLETED);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
        }
        return transaction;
    }
    
    public CompletableFuture<Void> processMultipleTransactions(Transaction... transactions) {
        CompletableFuture<Transaction>[] futures = new CompletableFuture[transactions.length];
        
        for (int i = 0; i < transactions.length; i++) {
            futures[i] = processTransactionAsync(transactions[i]);
        }
        
        // Demonstrate CompletableFuture.allOf
        return CompletableFuture.allOf(futures)
                .thenAccept(v -> {
                    System.out.printf("[%s] All %d transactions completed%n", 
                            Thread.currentThread().getName(), transactions.length);
                });
    }
}