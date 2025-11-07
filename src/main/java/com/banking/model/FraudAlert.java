package com.banking.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class FraudAlert {
    private final String alertId;
    private final String accountId;
    private final String reason;
    private final double suspiciousAmount;
    private final LocalDateTime timestamp;
    
    public FraudAlert(String accountId, String reason, double suspiciousAmount) {
        this.alertId = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.reason = reason;
        this.suspiciousAmount = suspiciousAmount;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getAlertId() {
        return alertId;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public double getSuspiciousAmount() {
        return suspiciousAmount;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("FraudAlert{id='%s', account='%s', reason='%s', amount=%.2f}", 
                alertId, accountId, reason, suspiciousAmount);
    }
}