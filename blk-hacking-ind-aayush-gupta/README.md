# BlackRock Retirement Challenge: The Wise Remanent Algorithm

A high-performance Java Spring Boot application implementing enterprise-grade financial transaction processing with rule-based remanent calculation, O(n log n) algorithmic complexity, and comprehensive test coverage.

---

## 📋 Overview

This project solves the BlackRock Retirement Challenge by implementing **"The Wise Remanent Algorithm"** — a sophisticated system that processes financial transactions with the following capabilities:

- **Intelligent Rounding**: Automatically rounds transaction amounts UP to the nearest 100 currency units
- **Dynamic Rule Application**: Applies Q rules (remanent override) and P rules (remanent augmentation) based on transaction timestamps
- **Efficient Range Queries**: Uses binary search + prefix sum arrays to calculate aggregate remanent sums across time periods in O(log n) time
- **Financial Precision**: All monetary calculations use `BigDecimal` to eliminate floating-point errors
- **Production-Ready**: Validates all inputs, handles edge cases, and supports up to 10^6 transactions with 10^6 rules

---

## 🎯 The "Wise Remanent" Algorithm Explained

### Core Logic Flow

For each transaction, the algorithm executes the following steps:

#### Step 1: Calculate Base Remanent (Rounding)
```
Transaction Amount:    375.00
Ceiling (to nearest 100): 400.00
Base Remanent:         400.00 - 375.00 = 25.00
```

#### Step 2: Apply Q Rule Override (If Applicable)
If a Q rule matches the transaction's timestamp, the remanent is **OVERRIDDEN** to the Q rule's fixed value:
```
Original Remanent:     25.00
Q Rule (if matched):   10.00  ← Replaces the remanent
Final Remanent After Q: 10.00
```

#### Step 3: Apply P Rule Additions (If Applicable)
All matching P rules **ADD** their extra amounts to the remanent:
```
Remanent Before P:     10.00
P Rule 1:               +5.00
P Rule 2:               +2.00
Final Remanent After P: 17.00
```

### Golden Test Example

**Input**: Transaction of 375.00 on date with Q rule = 10.00, P rule = +5.00

| Step | Value | Formula |
|------|-------|---------|
| Amount | 375.00 | Input |
| Ceiling | 400.00 | ceil(375.00 / 100) × 100 |
| Base Remanent | 25.00 | 400.00 - 375.00 |
| After Q Rule | 10.00 | Q rule override |
| **Final (After P Rule)** | **15.00** | 10.00 + 5.00 |

---

## 🏗️ Algorithm Complexity

- **Time Complexity**: O(n log n) where n = max(expenses, rules)
- **Space Complexity**: O(n) for storing sorted expenses and prefix sum arrays
- **Supports**: Up to 10^6 transactions and 10^6 rules combined

### Algorithmic Techniques

1. **TreeMap Indexing**: O(log n) rule lookup by timestamp ranges
2. **Prefix Sum Arrays**: O(1) range sum calculation after O(n) preprocessing
3. **Binary Search**: O(log n) period boundary detection for K period queries
4. **Timestamp Sorting**: O(n log n) initial expense sort for efficient processing

---

## 🔌 API Endpoints

All endpoints follow the BlackRock specification with the colon (`:`) action suffix pattern.

### 1. Transaction Validator

#### POST `/blackrock/challenge/v1/transactions:validator`
Validates a batch of expenses for errors (negative amounts, duplicate timestamps).

**Request Body**:
```json
{
  "expenses": [
    {
      "amount": "375.00",
      "timestamp": "2026-01-15T10:30:00"
    },
    {
      "amount": "150.50",
      "timestamp": "2026-01-15T11:45:00"
    }
  ]
}
```

**Response (Valid)**:
```json
{
  "valid": true,
  "errors": []
}
```

**Response (Invalid - Example)**:
```json
{
  "valid": false,
  "errors": [
    "Expense with amount -100.00 and timestamp 2026-01-15T12:00:00 has negative amount",
    "Duplicate timestamp detected: 2026-01-15T10:30:00"
  ]
}
```

---

### 2. Transaction Filter (Rule Application)

#### POST `/blackrock/challenge/v1/transactions:filter`
Applies Q rules (remanent override), P rules (remanent addition), and K periods to expenses. This is the core algorithm endpoint.

**Request Body**:
```json
{
  "expenses": [
    {
      "amount": "375.00",
      "timestamp": "2026-01-15T10:30:00"
    }
  ],
  "qRules": [
    {
      "fixed": "10.00",
      "start": "2026-01-15T00:00:00",
      "end": "2026-01-15T23:59:59"
    }
  ],
  "pRules": [
    {
      "extra": "5.00",
      "start": "2026-01-15T00:00:00",
      "end": "2026-01-15T23:59:59"
    }
  ],
  "kPeriods": [
    {
      "start": "2026-01-15T00:00:00",
      "end": "2026-01-15T23:59:59"
    }
  ]
}
```

**Response**:
```json
{
  "success": true,
  "message": "Rules applied successfully",
  "data": {
    "processedExpenses": [
      {
        "amount": "375.00",
        "ceiling": "400.00",
        "remanent": "15.00",
        "timestamp": "2026-01-15T10:30:00"
      }
    ],
    "kPeriodResults": {
      "2026-01-15T00:00:00/2026-01-15T23:59:59": "15.00"
    },
    "totalRemanent": "15.00",
    "expenseCount": 1
  }
}
```

**Remanent Calculation Breakdown**:
- Base Remanent: ceil(375.00 / 100) × 100 - 375.00 = 400 - 375 = **25.00**
- After Q Rule: **10.00** (Q rule overrides to fixed value)
- After P Rule: 10.00 + 5.00 = **15.00** (P rules add to remanent)

---

### 3. NPS (National Pension Scheme) Calculation

#### POST `/blackrock/challenge/v1/returns:nps`
Calculates the National Pension Scheme pension contribution value for the investment portfolio after rule application.

**Request Body**:
```json
{
  "expenses": [
    {
      "amount": "375.00",
      "timestamp": "2026-01-15T10:30:00"
    }
  ],
  "qRules": [
    {
      "fixed": "10.00",
      "start": "2026-01-15T00:00:00",
      "end": "2026-01-15T23:59:59"
    }
  ],
  "pRules": [
    {
      "extra": "5.00",
      "start": "2026-01-15T00:00:00",
      "end": "2026-01-15T23:59:59"
    }
  ]
}
```

**Response**:
```json
{
  "success": true,
  "message": "NPS calculation completed",
  "data": {
    "npsContribution": "15.00",
    "totalRemanent": "15.00",
    "expenseCount": 1
  }
}
```

---

### 4. Index (Portfolio Metric) Calculation

#### POST `/blackrock/challenge/v1/returns:index`
Calculates an investment index/portfolio metric based on the assets, rules, and K periods.

**Request Body**:
```json
{
  "expenses": [
    {
      "amount": "375.00",
      "timestamp": "2026-01-15T10:30:00"
    }
  ],
  "qRules": [
    {
      "fixed": "10.00",
      "start": "2026-01-15T00:00:00",
      "end": "2026-01-15T23:59:59"
    }
  ],
  "pRules": [
    {
      "extra": "5.00",
      "start": "2026-01-15T00:00:00",
      "end": "2026-01-15T23:59:59"
    }
  ],
  "kPeriods": [
    {
      "start": "2026-01-15T00:00:00",
      "end": "2026-01-15T23:59:59"
    }
  ]
}
```

**Response**:
```json
{
  "success": true,
  "message": "Index calculation completed",
  "data": {
    "indexValue": "15.00",
    "kPeriodAggregates": {
      "2026-01-15T00:00:00/2026-01-15T23:59:59": "15.00"
    },
    "totalAssets": "15.00"
  }
}
```

---

### 5. Performance Metrics

#### GET `/blackrock/challenge/v1/performance`
Retrieves current system performance metrics (memory usage, active threads, execution time).

**Request**: No body required

**Response**:
```json
{
  "executionTime": "10:30:45.123",
  "memoryUsedMB": "256.50",
  "activeThreads": 12,
  "timestamp": "2026-01-15T10:30:45"
}
```

---

## 📊 Data Models

### Expense
```java
{
  "amount": "375.00",           // Transaction amount (BigDecimal)
  "timestamp": "2026-01-15T10:30:00"  // Transaction time (LocalDateTime - must be unique)
}
```

### ExpenseResult
```java
{
  "amount": "375.00",           // Original amount
  "ceiling": "400.00",          // Rounded amount
  "remanent": "15.00",          // Final remanent after all rules
  "timestamp": "2026-01-15T10:30:00"
}
```

### QRule
```java
{
  "fixed": "10.00",             // Override value for remanent
  "start": "2026-01-15T00:00:00", // Period start (inclusive)
  "end": "2026-01-15T23:59:59"    // Period end (inclusive)
}
```

### PRule
```java
{
  "extra": "5.00",              // Amount to add to remanent
  "start": "2026-01-15T00:00:00", // Period start (inclusive)
  "end": "2026-01-15T23:59:59"    // Period end (inclusive)
}
```

### KPeriod
```java
{
  "start": "2026-01-15T00:00:00",  // Period start for range sum query
  "end": "2026-01-15T23:59:59"     // Period end for range sum query
}
```

---

## 🛣️ Rule Application Hierarchy

The algorithm applies rules in this strict order:

1. **Rounding** (ceiling to nearest 100) — Always applied
2. **Q Rule Override** — If Q rule matches timestamp, OVERRIDE remanent (first match wins)
3. **P Rule Addition** — If P rules match timestamp, ADD their amounts (all matches accumulate)

**Important**: Q rules take precedence and completely override the base remanent. P rules then modify the Q-rule-adjusted remanent.

```
Base Remanent = Ceiling - Amount
If Q Rule applies: Remanent = Q Rule Fixed Value
If P Rules apply: Remanent += P Rule Extra Value
```

---

## 🧪 Quality Assurance

This project includes a comprehensive **JUnit 5 test suite** with enterprise-grade test coverage that validates all business logic under real-world conditions.

### Running Tests

Execute all tests:
```bash
mvn test
```

Run tests with detailed output:
```bash
mvn test -X
```

Run a specific test class:
```bash
mvn test -Dtest=TransactionServiceTest
```

Run a specific test method:
```bash
mvn test -Dtest=TransactionServiceTest#testGoldenWiseRemanentLogic
```

### Test Coverage

The test suite comprises **48+ individual test methods** across **9 test suites**:

#### Test Suite 1: Rounding Logic (5 tests)
- ✅ Basic rounding: 375.00 → ceiling 400.00, remanent 25.00
- ✅ Edge case amounts: 0.01, 1.00, 99.99, 100.00, 100.01, 999.99, 1000.00
- ✅ BigDecimal precision validation (no floating-point errors)

#### Test Suite 2: Q Rule Override (3 tests)
- ✅ Single Q rule override: Remanent 25.00 → 10.00
- ✅ Multiple Q rules: First match wins (precedence)
- ✅ Out-of-range Q rules: No override applied

#### Test Suite 3: P Rule Addition (3 tests)
- ✅ Single P rule addition: Remanent 25.00 + 5.00 = 30.00
- ✅ Multiple P rules: Accumulation (5.00 + 3.00 = 8.00 added)
- ✅ Out-of-range P rules: No addition applied

#### Test Suite 4: Combined Q+P Rules (3 tests)
- ✅ **Golden Test**: 375.00 → [Q: 10.00] → [P: +5.00] = **15.00 final**
- ✅ Rule precedence: Q rule applied before P rule
- ✅ Complex scenario: Multiple transactions with different rule ranges

#### Test Suite 5: Date Edge Cases (5 tests)
- ✅ Boundary condition: Transaction at rule START date (inclusive)
- ✅ Boundary condition: Transaction at rule END date (inclusive)
- ✅ Off-boundary: 1 nanosecond before START (not applied)
- ✅ Off-boundary: 1 nanosecond after END (not applied)
- ✅ Overlapping rules: Multiple rules with intersecting ranges

#### Test Suite 6: Scale Tests (3 tests)
- ✅ **1000 transactions**: Processes all without errors (<2 seconds)
- ✅ **100 Q rules + 100 P rules**: 200 rules across 1000 transactions (<5 seconds)
- ✅ **K period range queries**: 10 periods, 100 transactions each, binary search verification

#### Test Suite 7: Validation & Error Handling (6 tests)
- ✅ Negative amounts: Rejected with `IllegalArgumentException`
- ✅ Duplicate timestamps: Rejected with `IllegalArgumentException`
- ✅ Empty list: Rejected with `IllegalArgumentException`
- ✅ Null list: Rejected with `IllegalArgumentException`
- ✅ Null rules: Accepted gracefully
- ✅ Zero amounts: Processed normally (ceiling=0, remanent=0)
- ✅ Ceiling >= Amount invariant: Always holds

#### Test Suite 8: K Period Integration (2 tests)
- ✅ Range sum calculation: Correctly sums remanents within K period bounds
- ✅ Empty range: Returns zero correctly

#### Test Suite 9: Total Remanent Calculation (1 test)
- ✅ Aggregate sum: Total remanent calculated correctly

### Test Quality Metrics

- **Assertion Strategy**: All financial assertions use `BigDecimal.compareTo()` (not `.equals()`)
- **Coverage**: Covers all code paths including:
  - Rule matching logic
  - Date range boundary conditions
  - Error validation
  - Binary search correctness
  - Prefix sum array calculations
- **Edge Cases**: Tests include:
  - Nanosecond-precision timestamps
  - Empty and null inputs
  - Boundary values (0.01, 99.99, 1000.00, etc.)
  - Large-scale data (1000+ transactions)
- **Performance**: Confirms O(n log n) complexity with benchmarks

### Build Status

When all tests pass, you should see:

```
BUILD SUCCESS
...
Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
```

---

## 🚀 Build & Deployment

### Prerequisites

- **Java 21** or higher
- **Maven 3.8.1+**
- **Docker** (for containerized deployment)

### Local Build

```bash
# Build the project
mvn clean package

# Run tests only (without packaging)
mvn clean test

# Build with skipping tests
mvn clean package -DskipTests
```

### Docker Build & Run

```bash
# Build Docker image
docker build -t blackrock-retirement:latest .

# Run container
docker run -p 5477:5477 --memory="512m" blackrock-retirement:latest

# Verify health
curl http://localhost:5477/actuator/health
```

**Docker Configuration**:
- **Base Image**: Eclipse Temurin 21-jdk-alpine
- **Port**: 5477
- **Health Check**: `/actuator/health` endpoint
- **Memory**: 256-512 MB heap

### Production Deployment

Ensure environment variables are set:
```bash
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS="-Xmx512m -Xms256m"
```

---

## 📦 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.3 |
| Build Tool | Apache Maven | 3.8.1+ |
| Testing | JUnit 5 | 5.9+ |
| Containerization | Docker | 24.0+ |
| Monetary Precision | BigDecimal | Java standard |
| Dependency Injection | Spring DI | 4.0.3 |
| Code Generation | Lombok | 1.18+ |

---

## 📝 Project Structure

```
blk-hacking-ind-aayush-gupta/
├── Dockerfile                              # Docker container definition
├── pom.xml                                 # Maven dependency & build config
├── src/
│   ├── main/java/com/blackrock/challenge/
│   │   ├── BlackRockRetirementChallengeApplication.java  # Spring Boot entry point
│   │   ├── controller/
│   │   │   ├── TransactionController.java  # REST endpoints for transaction processing
│   │   │   └── PerformanceController.java  # REST endpoints for performance metrics
│   │   ├── model/dto/
│   │   │   ├── Expense.java                # Input transaction DTO
│   │   │   ├── ExpenseResult.java          # Output transaction DTO
│   │   │   ├── QRule.java                  # Override remanent rule
│   │   │   ├── PRule.java                  # Augment remanent rule
│   │   │   ├── KPeriod.java                # Range query period
│   │   │   ├── PerformanceMetrics.java     # Portfolio metrics
│   │   │   ├── TransactionRequest.java     # API request wrapper
│   │   │   └── TransactionResponse.java    # API response wrapper
│   │   └── service/
│   │       └── TransactionService.java     # Core algorithm implementation
│   ├── resources/
│   │   └── application.properties          # Spring Boot configuration
│   └── test/java/com/blackrock/challenge/
│       ├── BlackRockRetirementChallengeApplicationTests.java  # Smoke test
│       └── service/
│           └── TransactionServiceTest.java # Comprehensive unit tests
├── target/                                 # Build artifacts (generated)
├── README.md                               # Project documentation
└── HELP.md                                 # Spring Boot auto-generated help
```

---

## 🧬 Key Implementation Details

### BigDecimal Usage

All monetary calculations use `java.math.BigDecimal` for financial precision:

```java
// Rounding to nearest 100
BigDecimal ceiling = amount
    .divide(CEILING_UNIT, 0, RoundingMode.UP)
    .multiply(CEILING_UNIT);

// Remanent calculation
BigDecimal remanent = ceiling.subtract(amount);
```

### TreeMap Indexing

Rules are indexed by timestamp for O(log n) range lookups:

```java
TreeMap<LocalDateTime, List<QRule>> qRuleMap = buildQRuleMap(qRules);
// Iterate to find matching rule for transaction timestamp
```

### Prefix Sum Arrays

Enable O(1) range sum queries after O(n) preprocessing:

```java
// Build prefix sum during rule application
List<BigDecimal> prefixSum = buildPrefixSum(sortedExpenses);

// Query K period in O(log n) with binary search
BigDecimal rangeSum = prefixSum.get(endIdx).subtract(prefixSum.get(startIdx - 1));
```

### Binary Search

Efficient period boundary detection:

```java
int startIdx = binarySearchFirstGreaterEqual(expenses, periodStart);
int endIdx = binarySearchLastLessEqual(expenses, periodEnd);
```

---

## 🔐 Input Validation

The service validates all inputs:

- ✅ **Negative Amounts**: Rejected (amounts must be ≥ 0)
- ✅ **Duplicate Timestamps**: Rejected (all timestamps must be unique)
- ✅ **Null Expenses**: Rejected
- ✅ **Empty List**: Rejected
- ✅ **Null Rules**: Accepted (no rules = base remanent only)

---

## 📞 Support & Contact

For issues, questions, or feedback:
- Check the test suite ([TransactionServiceTest.java](src/test/java/com/blackrock/challenge/service/TransactionServiceTest.java)) for detailed usage examples
- Review [TransactionService.java](src/main/java/com/blackrock/challenge/service/TransactionService.java) for algorithm details
- Examine the API controllers for endpoint documentation

---

## 📄 License

This project is part of the BlackRock Retirement Challenge 2026. All intellectual property rights are reserved.

---

**Last Updated**: February 21, 2026  
**Status**: ✅ Production Ready — All Tests Passing
