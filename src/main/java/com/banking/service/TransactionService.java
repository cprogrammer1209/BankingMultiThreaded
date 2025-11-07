package com.banking.service;

import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.model.Transaction;
import com.banking.model.TransactionStatus;
import com.banking.model.TransactionType;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TransactionService {
    private final AccountService accountService;
    private final ThreadPoolExecutor transactionExecutor;
    private final ConcurrentHashMap<String, Transaction> transactions;
    private final AtomicLong processedTransactions;
    
    public TransactionService(AccountService accountService) {
        this.accountService = accountService;
        this.transactionExecutor = new ThreadPoolExecutor(
                5, // core pool size
                10, // maximum pool size
                60L, TimeUnit.SECONDS, // keep alive time
                new LinkedBlockingQueue<>(100), // work queue
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "TransactionWorker-" + (++counter));
                    }
                }
        );
        this.transactions = new ConcurrentHashMap<>();
        this.processedTransactions = new AtomicLong(0);
    }
    
    public Future<Transaction> processTransactionAsync(Transaction transaction) {
        transactions.put(transaction.getTransactionId(), transaction);
        
        return transactionExecutor.submit(() -> {
            try {
                logTransaction("Starting processing", transaction);
                
                // Validate transaction
                if (!validateTransaction(transaction)) {
                    transaction.setStatus(TransactionStatus.FAILED);
                    logTransaction("Validation failed", transaction);
                    return transaction;
                }
                
                transaction.setStatus(TransactionStatus.PROCESSING);
                logTransaction("Validation passed, executing", transaction);
                
                // Execute the transaction
                executeTransaction(transaction);
                
                transaction.setStatus(TransactionStatus.COMPLETED);
                processedTransactions.incrementAndGet();
                logTransaction("Completed successfully", transaction);
                
                return transaction;
                
            } catch (Exception e) {
                transaction.setStatus(TransactionStatus.FAILED);
                logTransaction("Failed with error: " + e.getMessage(), transaction);
                return transaction;
            }
        });
    }
    
    private boolean validateTransaction(Transaction transaction) {
        // Simple validation logic
        if (transaction.getAmount() <= 0) {
            return false;
        }
        
        if (transaction.getType() == TransactionType.TRANSFER && 
            transaction.getFromAccount().equals(transaction.getToAccount())) {
            return false;
        }
        
        // Simulate validation time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        
        return true;
    }
    
    private void executeTransaction(Transaction transaction) 
            throws AccountNotFoundException, InsufficientFundsException {
        
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
    }
    
    private void logTransaction(String message, Transaction transaction) {
        System.out.printf("[%s] %s - %s%n", 
                Thread.currentThread().getName(), message, transaction);
    }
    
    public Transaction getTransaction(String transactionId) {
        return transactions.get(transactionId);
    }
    
    public long getProcessedTransactionCount() {
        return processedTransactions.get();
    }
    
    public void printExecutorStats() {
        System.out.printf("ThreadPool Stats - Active: %d, Completed: %d, Queue Size: %d%n",
                transactionExecutor.getActiveCount(),
                transactionExecutor.getCompletedTaskCount(),
                transactionExecutor.getQueue().size());
    }
    
    public void shutdown() {
        transactionExecutor.shutdown();
        try {
            if (!transactionExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                transactionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            transactionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}