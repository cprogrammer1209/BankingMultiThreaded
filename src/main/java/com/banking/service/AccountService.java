package com.banking.service;

import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.model.Account;

import java.util.concurrent.ConcurrentHashMap;

public class AccountService {
    private final ConcurrentHashMap<String, Account> accounts;
    
    public AccountService() {
        this.accounts = new ConcurrentHashMap<>();
    }
    
    public void createAccount(String accountId, double initialBalance) {
        accounts.put(accountId, new Account(accountId, initialBalance));
        System.out.printf("[%s] Created account: %s with balance: %.2f%n", 
                Thread.currentThread().getName(), accountId, initialBalance);
    }
    
    public Account getAccount(String accountId) throws AccountNotFoundException {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("Account not found: " + accountId);
        }
        return account;
    }
    
    public void deposit(String accountId, double amount) throws AccountNotFoundException {
        Account account = getAccount(accountId);
        
        account.getBalanceLock().lock();
        try {
            double newBalance = account.getBalance() + amount;
            account.setBalance(newBalance);
            account.incrementTransactionCount();
            
            System.out.printf("[%s] Deposited %.2f to %s. New balance: %.2f%n", 
                    Thread.currentThread().getName(), amount, accountId, newBalance);
        } finally {
            account.getBalanceLock().unlock();
        }
    }
    
    public void withdraw(String accountId, double amount) throws AccountNotFoundException, InsufficientFundsException {
        Account account = getAccount(accountId);
        
        account.getBalanceLock().lock();
        try {
            if (account.getBalance() < amount) {
                throw new InsufficientFundsException(
                        String.format("Insufficient funds in account %s. Balance: %.2f, Requested: %.2f", 
                                accountId, account.getBalance(), amount));
            }
            
            double newBalance = account.getBalance() - amount;
            account.setBalance(newBalance);
            account.incrementTransactionCount();
            
            System.out.printf("[%s] Withdrew %.2f from %s. New balance: %.2f%n", 
                    Thread.currentThread().getName(), amount, accountId, newBalance);
        } finally {
            account.getBalanceLock().unlock();
        }
    }
    
    public void transfer(String fromId, String toId, double amount) 
            throws AccountNotFoundException, InsufficientFundsException {
        
        Account fromAccount = getAccount(fromId);
        Account toAccount = getAccount(toId);
        
        // Ordered locking to prevent deadlocks
        Account firstLock = fromId.compareTo(toId) < 0 ? fromAccount : toAccount;
        Account secondLock = fromId.compareTo(toId) < 0 ? toAccount : fromAccount;
        
        firstLock.getBalanceLock().lock();
        try {
            secondLock.getBalanceLock().lock();
            try {
                if (fromAccount.getBalance() < amount) {
                    throw new InsufficientFundsException(
                            String.format("Insufficient funds for transfer from %s. Balance: %.2f, Requested: %.2f", 
                                    fromId, fromAccount.getBalance(), amount));
                }
                
                double newFromBalance = fromAccount.getBalance() - amount;
                double newToBalance = toAccount.getBalance() + amount;
                
                fromAccount.setBalance(newFromBalance);
                toAccount.setBalance(newToBalance);
                
                fromAccount.incrementTransactionCount();
                toAccount.incrementTransactionCount();
                
                System.out.printf("[%s] Transferred %.2f from %s to %s. Balances: %s=%.2f, %s=%.2f%n", 
                        Thread.currentThread().getName(), amount, fromId, toId, 
                        fromId, newFromBalance, toId, newToBalance);
                
            } finally {
                secondLock.getBalanceLock().unlock();
            }
        } finally {
            firstLock.getBalanceLock().unlock();
        }
    }
    
    public double getBalance(String accountId) throws AccountNotFoundException {
        Account account = getAccount(accountId);
        return account.getBalance();
    }
    
    public void printAllAccounts() {
        System.out.println("\n=== Account Summary ===");
        accounts.values().forEach(System.out::println);
        System.out.println("=====================\n");
    }
}