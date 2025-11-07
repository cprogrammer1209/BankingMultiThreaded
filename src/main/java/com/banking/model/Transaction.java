package com.banking.model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Transaction {
    private final String transactionId;
    private final String fromAccount;
    private final String toAccount;
    private final double amount;
    private final TransactionType type;
    private final AtomicReference<TransactionStatus> status;
    private final LocalDateTime timestamp;
    
    public Transaction(String fromAccount, String toAccount, double amount, TransactionType type) {
        this.transactionId = UUID.randomUUID().toString();
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.type = type;
        this.status = new AtomicReference<>(TransactionStatus.PENDING);
        this.timestamp = LocalDateTime.now();
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getFromAccount() {
        return fromAccount;
    }
    
    public String getToAccount() {
        return toAccount;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public TransactionStatus getStatus() {
        return status.get();
    }
    
    public void setStatus(TransactionStatus newStatus) {
        status.set(newStatus);
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("Transaction{id='%s', type=%s, from='%s', to='%s', amount=%.2f, status=%s}", 
                transactionId, type, fromAccount, toAccount, amount, status.get());
    }
}