# Concurrent Banking System

A comprehensive Java application demonstrating all major multithreading concepts through a realistic banking scenario.

## Threading Concepts Demonstrated

### 1. **ReentrantLock** (Account Operations)
- Thread-safe deposit, withdrawal, and transfer operations
- Ordered locking to prevent deadlocks in transfers
- Located in: `AccountService.java`

### 2. **Atomic Variables**
- `AtomicLong` for transaction counters
- `AtomicReference` for transaction status and timestamps
- `AtomicInteger` for fraud detection counters
- Located in: `Account.java`, `Transaction.java`, `FraudDetectionService.java`

### 3. **Thread-Safe Collections**
- `ConcurrentHashMap` for account storage
- `BlockingQueue` for fraud alert processing
- Located in: `AccountService.java`, `FraudDetectionService.java`

### 4. **ThreadPoolExecutor**
- Custom thread pool for transaction processing
- Configurable core/max pool sizes and work queue
- Located in: `TransactionService.java`

### 5. **CompletableFuture Methods**
- `supplyAsync()` - Asynchronous validation
- `thenApply()` / `thenApplyAsync()` - Result transformation
- `thenCompose()` / `thenComposeAsync()` - Chaining dependent operations
- `thenCombine()` / `thenCombineAsync()` - Combining multiple futures
- `thenAccept()` / `thenAcceptAsync()` - Consuming results
- `allOf()` - Waiting for multiple operations
- Located in: `AsyncTransactionService.java`

### 6. **Semaphore**
- Limiting concurrent fraud detection operations
- Resource management for fraud checking
- Located in: `FraudDetectionService.java`

### 7. **BlockingQueue & Producer-Consumer**
- Inter-thread communication for fraud alerts
- Producer threads generate alerts
- Consumer threads process alerts
- Located in: `FraudDetectionService.java`

### 8. **ReadWriteLock**
- Concurrent report generation
- Multiple readers, exclusive writers
- Located in: `ReportService.java`

### 9. **StampedLock**
- Optimistic reading for statistics
- High-performance locking for read-heavy operations
- Located in: `ReportService.java`

## How to Run

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Compilation
```bash
mvn compile
```

### Running the Application
```bash
mvn exec:java -Dexec.mainClass="com.banking.BankingSystemCLI"
```

Or compile and run directly:
```bash
mvn compile exec:java -Dexec.mainClass="com.banking.BankingSystemCLI"
```

## Menu Options

1. **Basic Transactions** - Demonstrates ReentrantLock usage
2. **Async Transactions** - Shows CompletableFuture chains
3. **Fraud Detection** - Semaphore and BlockingQueue in action
4. **Generate Reports** - ReadWriteLock and StampedLock usage
5. **Concurrent Operations** - Multiple threading concepts together
6. **System Status** - View current system state and statistics

## Key Features

- **Thread Safety**: All operations are thread-safe using appropriate synchronization
- **Deadlock Prevention**: Ordered locking in transfer operations
- **Resource Management**: Proper executor shutdown and cleanup
- **Real-time Monitoring**: Thread activity and performance metrics
- **Error Handling**: Comprehensive exception handling throughout
- **Concurrent Collections**: Efficient thread-safe data structures

## Architecture

```
BankingSystemCLI
├── AccountService (ReentrantLock, ConcurrentHashMap)
├── TransactionService (ThreadPoolExecutor)
├── AsyncTransactionService (CompletableFuture chains)
├── FraudDetectionService (Semaphore, BlockingQueue)
└── ReportService (ReadWriteLock, StampedLock)
```

## Sample Output

The application provides detailed logging showing:
- Thread names for all operations
- Lock acquisition and release
- Queue sizes and semaphore permits
- Transaction processing stages
- Performance statistics

This makes it easy to observe the threading concepts in action!

## Testing

Run unit tests:
```bash
mvn test
```

The tests include concurrent operation scenarios to verify thread safety.# BankingMultiThreaded
