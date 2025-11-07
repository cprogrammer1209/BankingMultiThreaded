package com.banking.service;

import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class AccountServiceTest {
    private AccountService accountService;
    
    @Before
    public void setUp() {
        accountService = new AccountService();
        accountService.createAccount("ACC001", 1000.0);
        accountService.createAccount("ACC002", 500.0);
    }
    
    @Test
    public void testDeposit() throws AccountNotFoundException {
        accountService.deposit("ACC001", 200.0);
        assertEquals(1200.0, accountService.getBalance("ACC001"), 0.01);
    }
    
    @Test
    public void testWithdraw() throws AccountNotFoundException, InsufficientFundsException {
        accountService.withdraw("ACC001", 300.0);
        assertEquals(700.0, accountService.getBalance("ACC001"), 0.01);
    }
    
    @Test(expected = InsufficientFundsException.class)
    public void testWithdrawInsufficientFunds() throws AccountNotFoundException, InsufficientFundsException {
        accountService.withdraw("ACC001", 1500.0);
    }
    
    @Test
    public void testTransfer() throws AccountNotFoundException, InsufficientFundsException {
        accountService.transfer("ACC001", "ACC002", 200.0);
        assertEquals(800.0, accountService.getBalance("ACC001"), 0.01);
        assertEquals(700.0, accountService.getBalance("ACC002"), 0.01);
    }
    
    @Test
    public void testConcurrentOperations() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100);
        
        // Perform 100 concurrent deposits of $10 each
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    accountService.deposit("ACC001", 10.0);
                } catch (AccountNotFoundException e) {
                    fail("Account not found during concurrent test");
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Should have original 1000 + (100 * 10) = 2000
        try {
            assertEquals(2000.0, accountService.getBalance("ACC001"), 0.01);
        } catch (AccountNotFoundException e) {
            fail("Account not found after concurrent operations");
        }
    }
}