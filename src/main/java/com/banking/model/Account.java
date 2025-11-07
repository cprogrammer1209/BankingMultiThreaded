package com.banking.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    private final String accountId;
    private volatile double balance;
    private final ReentrantLock balanceLock;
    private final AtomicLong transactionCount;
    private final AtomicReference<LocalDateTime> lastActivity;
    
    public Account(String accountId, double initialBalance) {
        this.accountId = accountId;
        this.balance = initialBalance;
        this.balanceLock = new ReentrantLock();
        this.transactionCount = new AtomicLong(0);
        this.lastActivity = new AtomicReference<>(LocalDateTime.now());
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
        this.lastActivity.set(LocalDateTime.now());
    }
    
    public ReentrantLock getBalanceLock() {
        return balanceLock;
    }
    
    public long getTransactionCount() {
        return transactionCount.get();
    }
    
    public void incrementTransactionCount() {
        transactionCount.incrementAndGet();
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity.get();
    }
    
    @Override
    public String toString() {
        return String.format("Account{id='%s', balance=%.2f, transactions=%d}", 
                accountId, balance, transactionCount.get());
    }
}