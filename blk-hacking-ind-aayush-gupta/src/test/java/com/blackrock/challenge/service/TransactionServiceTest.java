package com.blackrock.challenge.service;

import com.blackrock.challenge.model.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise-grade JUnit 5 test suite for TransactionService.
 * 
 * Covers the exact "Wise Remanent" logic as specified by BlackRock judges:
 * 1. Rounding: 375.00 -> ceiling 400.00, remanent 25.00
 * 2. Q Rule Override: If Q rule = 10.00, remanent becomes 10.00
 * 3. P Rule Addition: If P rule = 5.00, adds to remanent
 * 4. Combined Logic: 375.00 -> (Q: 10.00) -> (P: +5.00) = 15.00 final remanent
 * 5. Date Edge Cases: Boundary conditions (exact start/end dates)
 * 6. Scale Test: 1000+ transactions without errors
 * 7. Validation: Negative amounts, duplicate timestamps, null values
 */
@SpringBootTest
@DisplayName("TransactionService Enterprise-Grade Test Suite")
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    private LocalDateTime baseDate;

    @BeforeEach
    void setup() {
        baseDate = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        assertNotNull(transactionService, "TransactionService should be injected");
    }

    @Nested
    @DisplayName("Test Suite 1: Rounding Logic")
    class RoundingLogicTests {

        @Test
        @DisplayName("Should round 375.00 to ceiling 400.00 with remanent 25.00")
        void testRound375To400() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            List<Expense> expenses = List.of(expense);

            // Act
            Object result = transactionService.applyRules(expenses, null, null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            assertEquals(1, processedExpenses.size(), "Should process exactly 1 expense");
            ExpenseResult expenseResult = processedExpenses.get(0);
            
            assertTrue(expenseResult.getCeiling().compareTo(new BigDecimal("400.00")) == 0,
                    "Ceiling should be 400.00, got " + expenseResult.getCeiling());
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal("25.00")) == 0,
                    "Remanent should be 25.00, got " + expenseResult.getRemanent());
        }

        @ParameterizedTest
        @CsvSource({
                "0.01,100.00,99.99",
                "1.00,100.00,99.00",
                "99.99,100.00,0.01",
                "100.00,100.00,0.00",
                "100.01,200.00,99.99",
                "999.99,1000.00,0.01",
                "1000.00,1000.00,0.00"
        })
        @DisplayName("Should round ceiling UP to nearest 100 for various amounts")
        void testCeilingRoundingEdgeCases(String amountStr, String expectedCeiling, String expectedRemanent) {
            // Arrange
            Expense expense = new Expense(new BigDecimal(amountStr), baseDate);
            List<Expense> expenses = List.of(expense);

            // Act
            Object result = transactionService.applyRules(expenses, null, null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");
            ExpenseResult expenseResult = processedExpenses.get(0);

            // Assert
            assertTrue(expenseResult.getCeiling().compareTo(new BigDecimal(expectedCeiling)) == 0,
                    String.format("Amount %s should ceiling to %s, got %s", 
                        amountStr, expectedCeiling, expenseResult.getCeiling()));
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal(expectedRemanent)) == 0,
                    String.format("Amount %s should have remanent %s, got %s",
                        amountStr, expectedRemanent, expenseResult.getRemanent()));
        }

        @Test
        @DisplayName("Should maintain precision with BigDecimal (no floating-point errors)")
        void testBigDecimalPrecision() {
            // Arrange: Use amounts that would cause floating-point errors
            Expense expense1 = new Expense(new BigDecimal("0.10"), baseDate);
            Expense expense2 = new Expense(new BigDecimal("0.20"), baseDate.plusSeconds(1));
            List<Expense> expenses = List.of(expense1, expense2);

            // Act
            Object result = transactionService.applyRules(expenses, null, null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            for (ExpenseResult exp : processedExpenses) {
                // Verify no floating-point artifacts (should be exact BigDecimal values)
                assertNotNull(exp.getCeiling());
                assertNotNull(exp.getRemanent());
                // Verify ceiling is multiple of 100
                assertTrue(exp.getCeiling().remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) == 0);
                // Verify remanent = ceiling - amount
                BigDecimal expectedRemanent = exp.getCeiling().subtract(exp.getAmount());
                assertTrue(exp.getRemanent().compareTo(expectedRemanent) == 0);
            }
        }
    }

    @Nested
    @DisplayName("Test Suite 2: Q Rule Override")
    class QRuleOverrideTests {

        @Test
        @DisplayName("Should override remanent 25.00 with Q rule 10.00")
        void testQRuleOverride() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            QRule qRule = new QRule(
                    new BigDecimal("10.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), List.of(qRule), null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");
            ExpenseResult expenseResult = processedExpenses.get(0);

            // Assert
            assertTrue(expenseResult.getCeiling().compareTo(new BigDecimal("400.00")) == 0,
                    "Ceiling should still be 400.00");
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal("10.00")) == 0,
                    "Remanent should be overridden to 10.00, got " + expenseResult.getRemanent());
        }

        @Test
        @DisplayName("Should apply first matching Q rule when multiple Q rules exist")
        void testMultipleQRulesFirstMatchWins() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            QRule qRule1 = new QRule(
                    new BigDecimal("10.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );
            QRule qRule2 = new QRule(
                    new BigDecimal("20.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), List.of(qRule1, qRule2), null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");
            ExpenseResult expenseResult = processedExpenses.get(0);

            // Assert: Should apply first Q rule (10.00), not second (20.00)
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal("10.00")) == 0,
                    "Should apply first matching Q rule (10.00)");
        }

        @Test
        @DisplayName("Should NOT apply Q rule if transaction is outside date range")
        void testQRuleNotAppliedOutsideDateRange() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            QRule qRule = new QRule(
                    new BigDecimal("10.00"),
                    baseDate.plusDays(1),  // Rule starts tomorrow
                    baseDate.plusDays(2)
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), List.of(qRule), null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");
            ExpenseResult expenseResult = processedExpenses.get(0);

            // Assert: Should keep original remanent 25.00 (not 10.00)
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal("25.00")) == 0,
                    "Remanent should remain 25.00 when Q rule is outside date range");
        }
    }

    @Nested
    @DisplayName("Test Suite 3: P Rule Addition")
    class PRuleAdditionTests {

        @Test
        @DisplayName("Should add P rule 5.00 to base remanent 25.00")
        void testPRuleAddition() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            PRule pRule = new PRule(
                    new BigDecimal("5.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), null, List.of(pRule), null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");
            ExpenseResult expenseResult = processedExpenses.get(0);

            // Assert
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal("30.00")) == 0,
                    "Remanent should be 25.00 + 5.00 = 30.00, got " + expenseResult.getRemanent());
        }

        @Test
        @DisplayName("Should accumulate multiple P rules (5.00 + 3.00 = 8.00)")
        void testMultiplePRulesAccumulate() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            PRule pRule1 = new PRule(
                    new BigDecimal("5.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );
            PRule pRule2 = new PRule(
                    new BigDecimal("3.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );

            // Act
            Object result = transactionService.applyRules(
                    List.of(expense), null, List.of(pRule1, pRule2), null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");
            ExpenseResult expenseResult = processedExpenses.get(0);

            // Assert
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal("33.00")) == 0,
                    "Remanent should be 25.00 + 5.00 + 3.00 = 33.00, got " + expenseResult.getRemanent());
        }

        @Test
        @DisplayName("Should NOT apply P rule if transaction is outside date range")
        void testPRuleNotAppliedOutsideDateRange() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            PRule pRule = new PRule(
                    new BigDecimal("5.00"),
                    baseDate.plusDays(1),  // Rule starts tomorrow
                    baseDate.plusDays(2)
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), null, List.of(pRule), null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");
            ExpenseResult expenseResult = processedExpenses.get(0);

            // Assert: Should keep original remanent 25.00 (not 30.00)
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal("25.00")) == 0,
                    "Remanent should remain 25.00 when P rule is outside date range");
        }
    }

    @Nested
    @DisplayName("Test Suite 4: Combined Q Rule + P Rule (Golden Test)")
    class CombinedQAndPRulesTests {

        @Test
        @DisplayName("Golden Test: 375 -> Q(10) -> P(+5) = 15 final remanent")
        void testGoldenWiseRemanentLogic() {
            // Arrange: The exact scenario from BlackRock specification
            // Input: 375.00
            // Step 1: Ceiling = 400.00
            // Step 2: Base Remanent = 400.00 - 375.00 = 25.00
            // Step 3: Apply Q Rule = 10.00 (override remanent to 10.00)
            // Step 4: Apply P Rule = +5.00 (add 5.00 to remanent)
            // Final: 10.00 + 5.00 = 15.00
            
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            QRule qRule = new QRule(
                    new BigDecimal("10.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );
            PRule pRule = new PRule(
                    new BigDecimal("5.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );

            // Act
            Object result = transactionService.applyRules(
                    List.of(expense), List.of(qRule), List.of(pRule), null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");
            ExpenseResult expenseResult = processedExpenses.get(0);

            // Assert
            assertTrue(expenseResult.getCeiling().compareTo(new BigDecimal("400.00")) == 0,
                    "Step 1: Ceiling should be 400.00");
            assertTrue(expenseResult.getRemanent().compareTo(new BigDecimal("15.00")) == 0,
                    "Final: Q Rule (10.00) + P Rule (+5.00) should = 15.00, got " + expenseResult.getRemanent());
        }

        @Test
        @DisplayName("Should apply Q rule BEFORE P rule (Q overrides, then P adds to override)")
        void testQRuleAppliesToOriginalRemanentThenPRuleAdds() {
            // Arrange: Verify the precedence order
            // Without Q rule: Remanent = 25.00, then P adds 5.00 = 30.00
            // With Q rule: Remanent = 10.00 (override), then P adds 5.00 = 15.00
            
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            QRule qRule = new QRule(
                    new BigDecimal("10.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );
            PRule pRule = new PRule(
                    new BigDecimal("5.00"),
                    baseDate.minusHours(1),
                    baseDate.plusHours(1)
            );

            // Act: Apply Q + P
            Object resultWithBoth = transactionService.applyRules(
                    List.of(expense), List.of(qRule), List.of(pRule), null);
            Map<String, Object> resultMapBoth = (Map<String, Object>) resultWithBoth;
            List<ExpenseResult> ExpensesBoth = (List<ExpenseResult>) resultMapBoth.get("processedExpenses");

            // Act: Apply P only (without Q)
            Object resultWithPOnly = transactionService.applyRules(
                    List.of(expense), null, List.of(pRule), null);
            Map<String, Object> resultMapPOnly = (Map<String, Object>) resultWithPOnly;
            List<ExpenseResult> ExpensesPOnly = (List<ExpenseResult>) resultMapPOnly.get("processedExpenses");

            // Assert
            assertTrue(ExpensesBoth.get(0).getRemanent().compareTo(new BigDecimal("15.00")) == 0,
                    "With Q rule: Should be 10.00 + 5.00 = 15.00");
            assertTrue(ExpensesPOnly.get(0).getRemanent().compareTo(new BigDecimal("30.00")) == 0,
                    "Without Q rule: Should be 25.00 + 5.00 = 30.00");
        }

        @Test
        @DisplayName("Should handle complex scenario with multiple Q/P rules")
        void testMultipleQAndPRules() {
            // Arrange: Multiple rules with different date ranges
            LocalDateTime time1 = baseDate;
            LocalDateTime time2 = baseDate.plusHours(2);

            Expense exp1 = new Expense(new BigDecimal("350.00"), time1);
            Expense exp2 = new Expense(new BigDecimal("450.00"), time2);

            QRule qRule = new QRule(
                    new BigDecimal("20.00"),
                    time1.minusHours(1),
                    time1.plusHours(1)
            );
            PRule pRule1 = new PRule(
                    new BigDecimal("5.00"),
                    time1.minusHours(1),
                    time1.plusHours(1)
            );
            PRule pRule2 = new PRule(
                    new BigDecimal("10.00"),
                    time2.minusHours(1),
                    time2.plusHours(1)
            );

            // Act
            Object result = transactionService.applyRules(
                    List.of(exp1, exp2),
                    List.of(qRule),
                    List.of(pRule1, pRule2),
                    null
            );
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            // exp1 (350.00, time1): ceiling=400, base remanent=50, Q rule=20, P rule +5 = 25
            // exp2 (450.00, time2): ceiling=500, base remanent=50, no Q rule, P rule +10 = 60
            assertTrue(processedExpenses.get(0).getRemanent().compareTo(new BigDecimal("25.00")) == 0);
            assertTrue(processedExpenses.get(1).getRemanent().compareTo(new BigDecimal("60.00")) == 0);
        }
    }

    @Nested
    @DisplayName("Test Suite 5: Date Edge Cases & Boundary Conditions")
    class DateEdgeCasesTests {

        @Test
        @DisplayName("Should apply Q rule when transaction is at exact START date of range")
        void testQRuleAppliedAtStartDate() {
            // Arrange
            LocalDateTime ruleStart = baseDate;
            Expense expense = new Expense(new BigDecimal("375.00"), ruleStart);
            QRule qRule = new QRule(
                    new BigDecimal("10.00"),
                    ruleStart,
                    ruleStart.plusHours(1)
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), List.of(qRule), null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            assertTrue(processedExpenses.get(0).getRemanent().compareTo(new BigDecimal("10.00")) == 0,
                    "Q rule should apply at exact start date");
        }

        @Test
        @DisplayName("Should apply Q rule when transaction is at exact END date of range")
        void testQRuleAppliedAtEndDate() {
            // Arrange
            LocalDateTime ruleEnd = baseDate;
            Expense expense = new Expense(new BigDecimal("375.00"), ruleEnd);
            QRule qRule = new QRule(
                    new BigDecimal("10.00"),
                    ruleEnd.minusHours(1),
                    ruleEnd
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), List.of(qRule), null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            assertTrue(processedExpenses.get(0).getRemanent().compareTo(new BigDecimal("10.00")) == 0,
                    "Q rule should apply at exact end date");
        }

        @Test
        @DisplayName("Should NOT apply Q rule 1 nanosecond BEFORE start date")
        void testQRuleNotAppliedBeforeStartDate() {
            // Arrange
            LocalDateTime ruleStart = baseDate;
            Expense expense = new Expense(new BigDecimal("375.00"), ruleStart.minusNanos(1));
            QRule qRule = new QRule(
                    new BigDecimal("10.00"),
                    ruleStart,
                    ruleStart.plusHours(1)
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), List.of(qRule), null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            assertTrue(processedExpenses.get(0).getRemanent().compareTo(new BigDecimal("25.00")) == 0,
                    "Q rule should NOT apply before start date");
        }

        @Test
        @DisplayName("Should NOT apply Q rule 1 nanosecond AFTER end date")
        void testQRuleNotAppliedAfterEndDate() {
            // Arrange
            LocalDateTime ruleEnd = baseDate;
            Expense expense = new Expense(new BigDecimal("375.00"), ruleEnd.plusNanos(1));
            QRule qRule = new QRule(
                    new BigDecimal("10.00"),
                    ruleEnd.minusHours(1),
                    ruleEnd
            );

            // Act
            Object result = transactionService.applyRules(List.of(expense), List.of(qRule), null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            assertTrue(processedExpenses.get(0).getRemanent().compareTo(new BigDecimal("25.00")) == 0,
                    "Q rule should NOT apply after end date");
        }

        @Test
        @DisplayName("Should handle overlapping P rules correctly")
        void testOverlappingPRules() {
            // Arrange: Two P rules that both overlap the transaction time
            LocalDateTime txTime = baseDate;
            Expense expense = new Expense(new BigDecimal("375.00"), txTime);
            
            PRule pRule1 = new PRule(
                    new BigDecimal("5.00"),
                    txTime.minusHours(2),
                    txTime.plusHours(1)
            );
            PRule pRule2 = new PRule(
                    new BigDecimal("3.00"),
                    txTime.minusHours(1),
                    txTime.plusHours(2)
            );

            // Act
            Object result = transactionService.applyRules(
                    List.of(expense), null, List.of(pRule1, pRule2), null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert: Both rules should apply and accumulate
            assertTrue(processedExpenses.get(0).getRemanent().compareTo(new BigDecimal("33.00")) == 0,
                    "Both overlapping P rules should apply: 25.00 + 5.00 + 3.00 = 33.00");
        }
    }

    @Nested
    @DisplayName("Test Suite 6: Scale Test (Performance & Correctness)")
    class ScaleTests {

        @Test
        @DisplayName("Should handle 1000 transactions without errors")
        void testScale1000Transactions() {
            // Arrange: Create 1000 expenses with sequential timestamps
            List<Expense> expenses = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                expenses.add(new Expense(
                        new BigDecimal(String.valueOf(100 + i % 900)),  // Amounts 100-999
                        baseDate.plusSeconds(i)
                ));
            }

            // Act
            long startTime = System.currentTimeMillis();
            Object result = transactionService.applyRules(expenses, null, null, null);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            assertEquals(1000, processedExpenses.size(), "Should process all 1000 transactions");
            for (ExpenseResult exp : processedExpenses) {
                // Verify each has valid ceiling and remanent
                assertNotNull(exp.getCeiling());
                assertNotNull(exp.getRemanent());
                assertTrue(exp.getCeiling().compareTo(exp.getAmount()) >= 0);
                assertTrue(exp.getRemanent().compareTo(BigDecimal.ZERO) >= 0);
            }

            // Verify performance: should complete in < 2 seconds with O(n log n) complexity
            assertTrue(duration < 2000, "Should complete 1000 transactions in < 2 seconds, took " + duration + "ms");
        }

        @Test
        @DisplayName("Should handle 100 Q rules + 100 P rules across 1000 transactions")
        void testScaleWithManyRules() {
            // Arrange: Create 1000 expenses, 100 Q rules, 100 P rules
            List<Expense> expenses = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                expenses.add(new Expense(
                        new BigDecimal(String.valueOf(100 + i % 900)),
                        baseDate.plusSeconds(i)
                ));
            }

            List<QRule> qRules = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                qRules.add(new QRule(
                        new BigDecimal(String.valueOf(5 + i % 20)),
                        baseDate.plusMinutes(i),
                        baseDate.plusMinutes(i + 10)
                ));
            }

            List<PRule> pRules = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                pRules.add(new PRule(
                        new BigDecimal(String.valueOf(1 + i % 10)),
                        baseDate.plusMinutes(i),
                        baseDate.plusMinutes(i + 10)
                ));
            }

            // Act
            long startTime = System.currentTimeMillis();
            Object result = transactionService.applyRules(expenses, qRules, pRules, null);
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            assertEquals(1000, processedExpenses.size());
            assertTrue(duration < 5000, "Should handle 1000 transactions + 200 rules in < 5 seconds, took " + duration + "ms");
        }

        @Test
        @DisplayName("Should correctly calculate K period range sums with 1000 transactions")
        void testScaleWithKPeriods() {
            // Arrange: Create 1000 expenses, 10 K periods
            List<Expense> expenses = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                expenses.add(new Expense(
                        new BigDecimal("100.00"),  // Simple amount for easy verification
                        baseDate.plusSeconds(i)
                ));
            }

            List<KPeriod> kPeriods = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                kPeriods.add(new KPeriod(
                        baseDate.plusSeconds(i * 100),
                        baseDate.plusSeconds((i + 1) * 100 - 1)
                ));
            }

            // Act
            Object result = transactionService.applyRules(expenses, null, null, kPeriods);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            Map<KPeriod, BigDecimal> kPeriodResults = (Map<KPeriod, BigDecimal>) resultMap.get("kPeriodResults");

            // Assert
            assertEquals(10, kPeriodResults.size(), "Should have 10 K period results");
            for (BigDecimal rangeSum : kPeriodResults.values()) {
                // Each period should have 100 expenses * 0 remanent = 0 (since amount=ceiling for 100)
                assertTrue(rangeSum.compareTo(BigDecimal.ZERO) == 0);
            }
        }
    }

    @Nested
    @DisplayName("Test Suite 7: Validation & Error Handling")
    class ValidationTests {

        @Test
        @DisplayName("Should reject negative amount with IllegalArgumentException")
        void testValidationRejectsNegativeAmount() {
            // Arrange
            Expense expenseNegative = new Expense(new BigDecimal("-10.00"), baseDate);
            List<Expense> expenses = List.of(expenseNegative);

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.applyRules(expenses, null, null, null),
                    "Should throw IllegalArgumentException for negative amount");
        }

        @Test
        @DisplayName("Should reject duplicate timestamps")
        void testValidationRejectsDuplicateTimestamps() {
            // Arrange
            Expense expense1 = new Expense(new BigDecimal("100.00"), baseDate);
            Expense expense2 = new Expense(new BigDecimal("200.00"), baseDate);  // Same timestamp
            List<Expense> expenses = List.of(expense1, expense2);

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.applyRules(expenses, null, null, null),
                    "Should throw IllegalArgumentException for duplicate timestamps");
        }

        @Test
        @DisplayName("Should handle empty expense list")
        void testValidationHandlesEmptyList() {
            // Arrange
            List<Expense> expenses = List.of();

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.applyRules(expenses, null, null, null),
                    "Should throw IllegalArgumentException for empty list");
        }

        @Test
        @DisplayName("Should handle null expense list")
        void testValidationHandlesNullList() {
            // Arrange
            List<Expense> expenses = null;

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> transactionService.applyRules(expenses, null, null, null),
                    "Should throw IllegalArgumentException for null list");
        }

        @Test
        @DisplayName("Should accept null Q rules and P rules list")
        void testValidationAcceptsNullRules() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            List<Expense> expenses = List.of(expense);

            // Act & Assert: Should NOT throw exception with null rules
            Object result = transactionService.applyRules(expenses, null, null, null);
            assertNotNull(result, "Should handle null Q/P rules gracefully");
        }

        @Test
        @DisplayName("Should handle transactions with zero amount")
        void testValidationHandlesZeroAmount() {
            // Arrange
            Expense expense = new Expense(BigDecimal.ZERO, baseDate);
            List<Expense> expenses = List.of(expense);

            // Act
            Object result = transactionService.applyRules(expenses, null, null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            assertEquals(1, processedExpenses.size());
            // Ceiling of 0 is 0, remanent is 0
            assertTrue(processedExpenses.get(0).getCeiling().compareTo(BigDecimal.ZERO) == 0);
            assertTrue(processedExpenses.get(0).getRemanent().compareTo(BigDecimal.ZERO) == 0);
        }

        @Test
        @DisplayName("Should validate that all processed expenses have valid ceiling >= amount")
        void testCeilingAlwaysGreaterThanAmount() {
            // Arrange: Test with various amounts
            List<Expense> expenses = new ArrayList<>();
            expenses.add(new Expense(new BigDecimal("1.50"), baseDate));
            expenses.add(new Expense(new BigDecimal("99.99"), baseDate.plusSeconds(1)));
            expenses.add(new Expense(new BigDecimal("1000.00"), baseDate.plusSeconds(2)));

            // Act
            Object result = transactionService.applyRules(expenses, null, null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            List<ExpenseResult> processedExpenses = (List<ExpenseResult>) resultMap.get("processedExpenses");

            // Assert
            for (ExpenseResult exp : processedExpenses) {
                assertTrue(exp.getCeiling().compareTo(exp.getAmount()) >= 0,
                        String.format("Ceiling %s should be >= amount %s",
                            exp.getCeiling(), exp.getAmount()));
                assertTrue(exp.getRemanent().compareTo(BigDecimal.ZERO) >= 0,
                        "Remanent should never be negative");
            }
        }
    }

    @Nested
    @DisplayName("Test Suite 8: Integration with K Periods (Binary Search)")
    class KPeriodIntegrationTests {

        @Test
        @DisplayName("Should correctly sum remanents within K period range")
        void testKPeriodRangeSumCalculation() {
            // Arrange: Create expenses with known remanents
            LocalDateTime time0 = baseDate;
            LocalDateTime time1 = baseDate.plusSeconds(1);
            LocalDateTime time2 = baseDate.plusSeconds(2);

            Expense exp1 = new Expense(new BigDecimal("375.00"), time0);  // remanent = 25.00
            Expense exp2 = new Expense(new BigDecimal("350.00"), time1);  // remanent = 50.00
            Expense exp3 = new Expense(new BigDecimal("400.00"), time2);  // remanent = 0.00

            KPeriod period = new KPeriod(time0, time2);  // Include all

            // Act
            Object result = transactionService.applyRules(
                    List.of(exp1, exp2, exp3), null, null, List.of(period));
            Map<String, Object> resultMap = (Map<String, Object>) result;
            Map<KPeriod, BigDecimal> kPeriodResults = (Map<KPeriod, BigDecimal>) resultMap.get("kPeriodResults");

            // Assert: Total = 25 + 50 + 0 = 75
            BigDecimal rangeSum = kPeriodResults.get(period);
            assertTrue(rangeSum.compareTo(new BigDecimal("75.00")) == 0,
                    "K period sum should be 25.00 + 50.00 + 0.00 = 75.00, got " + rangeSum);
        }

        @Test
        @DisplayName("Should return zero for K period with no transactions in range")
        void testKPeriodWithNoTransactionsInRange() {
            // Arrange
            Expense expense = new Expense(new BigDecimal("375.00"), baseDate);
            KPeriod periodAfter = new KPeriod(
                    baseDate.plusDays(1),
                    baseDate.plusDays(2)
            );

            // Act
            Object result = transactionService.applyRules(
                    List.of(expense), null, null, List.of(periodAfter));
            Map<String, Object> resultMap = (Map<String, Object>) result;
            Map<KPeriod, BigDecimal> kPeriodResults = (Map<KPeriod, BigDecimal>) resultMap.get("kPeriodResults");

            // Assert
            BigDecimal rangeSum = kPeriodResults.get(periodAfter);
            assertTrue(rangeSum.compareTo(BigDecimal.ZERO) == 0,
                    "K period with no transactions should sum to zero");
        }
    }

    @Nested
    @DisplayName("Test Suite 9: Total Remanent Calculation")
    class TotalRemanentTests {

        @Test
        @DisplayName("Should correctly calculate total remanent sum")
        void testTotalRemanentCalculation() {
            // Arrange
            Expense exp1 = new Expense(new BigDecimal("375.00"), baseDate);           // 25.00
            Expense exp2 = new Expense(new BigDecimal("360.00"), baseDate.plusSeconds(1));  // 40.00
            Expense exp3 = new Expense(new BigDecimal("410.00"), baseDate.plusSeconds(2));  // -10.00 ceiling

            // Act
            Object result = transactionService.applyRules(
                    List.of(exp1, exp2, exp3), null, null, null);
            Map<String, Object> resultMap = (Map<String, Object>) result;
            BigDecimal totalRemanent = (BigDecimal) resultMap.get("totalRemanent");

            // Assert: Total = 25 + 40 = 65 (ceiling of 410 is 500, so ceiling - 410 = 90)
            // Actually: 25 + 40 + 90 = 155
            assertTrue(totalRemanent.compareTo(new BigDecimal("155.00")) == 0,
                    "Total remanent should be 25 + 40 + 90 = 155, got " + totalRemanent);
        }
    }
}
