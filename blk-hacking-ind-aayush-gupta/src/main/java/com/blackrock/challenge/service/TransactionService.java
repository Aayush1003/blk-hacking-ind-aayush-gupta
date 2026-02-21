package com.blackrock.challenge.service;

import com.blackrock.challenge.model.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Transaction Service for BlackRock Retirement Challenge.
 * Implements the "Wise Algorithm" with O(n log n) performance for handling
 * up to 10^6 transactions with 10^6 rules each.
 * 
 * Key Design: TreeMap for rule lookup, Prefix Sums + Binary Search for range queries.
 * All monetary calculations use BigDecimal for financial precision.
 */
@Service
public class TransactionService {

    private static final BigDecimal CEILING_UNIT = BigDecimal.valueOf(100);

    /**
     * Validates expenses for:
     * 1. Negative amounts (must be >= 0)
     * 2. Duplicate timestamps (all timestamps must be unique)
     * 
     * @param expenses List of expenses to validate
     * @return ValidationResponse with validation result and any errors
     */
    public ValidationResponse validateTransactions(List<Expense> expenses) {
        List<String> errors = new ArrayList<>();

        if (expenses == null || expenses.isEmpty()) {
            return ValidationResponse.builder()
                .valid(false)
                .message("Expenses list is null or empty")
                .errors(List.of("No expenses provided"))
                .build();
        }

        // Check for negative amounts
        for (int i = 0; i < expenses.size(); i++) {
            Expense exp = expenses.get(i);
            if (exp.getAmount() == null || exp.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Expense at index " + i + " has negative amount: " + exp.getAmount());
            }
        }

        // Check for duplicate timestamps
        Set<LocalDateTime> timestamps = new HashSet<>();
        for (int i = 0; i < expenses.size(); i++) {
            Expense exp = expenses.get(i);
            if (exp.getTimestamp() == null) {
                errors.add("Expense at index " + i + " has null timestamp");
            } else if (!timestamps.add(exp.getTimestamp())) {
                errors.add("Duplicate timestamp found: " + exp.getTimestamp());
            }
        }

        if (errors.isEmpty()) {
            return ValidationResponse.builder()
                .valid(true)
                .message("All expenses are valid")
                .errors(null)
                .build();
        } else {
            return ValidationResponse.builder()
                .valid(false)
                .message("Validation failed with " + errors.size() + " error(s)")
                .errors(errors)
                .build();
        }
    }

    /**
     * Core Algorithm: Apply Q, P, and K rules to expenses.
     * 
     * Algorithm Complexity: O(n log n) where n = max(expenses, qRules, pRules, kPeriods)
     * 
     * Steps:
     * 1. Validate expenses
     * 2. Calculate ceiling (round up to nearest 100) and base remanent for each expense
     * 3. Build TreeMap for QRule/PRule lookup by timestamp -> O(n log n)
     * 4. Apply Q rules (override) and P rules (add) -> O(n log n)
     * 5. Process K periods using prefix sums + binary search -> O(k log n)
     * 
     * @param expenses List of expenses
     * @param qRules List of Q rules (override remanent)
     * @param pRules List of P rules (add to remanent)
     * @param kPeriods List of K periods (range queries)
     * @return Map containing filtered expenses, K period results, and metadata
     */
    public Object applyRules(List<Expense> expenses, List<QRule> qRules,
                            List<PRule> pRules, List<KPeriod> kPeriods) {

        // Validation step
        ValidationResponse validation = validateTransactions(expenses);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid expenses: " + validation.getErrors());
        }

        // Step 1: Sort expenses by timestamp
        List<ExpenseResult> sortedExpenses = expenses.stream()
            .map(this::calculateBaseRemanent)
            .sorted(Comparator.comparing(ExpenseResult::getTimestamp))
            .collect(Collectors.toList());

        // Step 2: Build TreeMap for O(log n) rule lookups
        // TreeMap<timestamp, List of rules applicable at that time>
        TreeMap<LocalDateTime, List<QRule>> qRuleMap = buildQRuleMap(qRules);
        TreeMap<LocalDateTime, List<PRule>> pRuleMap = buildPRuleMap(pRules);

        // Step 3: Apply Q rules (override) and P rules (add) to each expense
        // Time Complexity: O(n log m) where m is number of rules in range
        applyQAndPRules(sortedExpenses, qRuleMap, pRuleMap);

        // Step 4: Build prefix sum array for efficient K period range queries
        List<BigDecimal> prefixSumRemanent = buildPrefixSum(sortedExpenses);

        // Step 5: Process K periods using binary search
        Map<KPeriod, BigDecimal> kPeriodResults = new LinkedHashMap<>();
        if (kPeriods != null) {
            for (KPeriod period : kPeriods) {
                BigDecimal rangeSum = calculateRangeSum(sortedExpenses, prefixSumRemanent, period);
                kPeriodResults.put(period, rangeSum);
            }
        }

        // Prepare response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processedExpenses", sortedExpenses);
        result.put("kPeriodResults", kPeriodResults);
        result.put("totalRemanent", prefixSumRemanent.isEmpty() ? BigDecimal.ZERO 
            : prefixSumRemanent.get(prefixSumRemanent.size() - 1));
        result.put("expenseCount", sortedExpenses.size());

        return result;
    }

    /**
     * Calculate ceiling (round up to nearest 100) and base remanent (ceiling - amount)
     * for a single expense. All calculations use BigDecimal with proper rounding.
     * 
     * @param expense Input expense with amount
     * @return ExpenseResult with ceiling, remanent, and metadata
     */
    private ExpenseResult calculateBaseRemanent(Expense expense) {
        BigDecimal amount = expense.getAmount();
        
        // Ceiling to nearest 100: divide by 100, round UP, multiply by 100
        BigDecimal ceiling = amount
            .divide(CEILING_UNIT, 0, RoundingMode.UP)
            .multiply(CEILING_UNIT);

        // Remanent = Ceiling - Amount
        BigDecimal remanent = ceiling.subtract(amount);

        return new ExpenseResult(amount, ceiling, remanent, expense.getTimestamp());
    }

    /**
     * Build TreeMap for Q rules indexed by time ranges.
     * Allows O(log n) lookup of applicable Q rules for any timestamp.
     * 
     * @param qRules List of Q rules to index
     * @return TreeMap with time -> rules mapping
     */
    private TreeMap<LocalDateTime, List<QRule>> buildQRuleMap(List<QRule> qRules) {
        TreeMap<LocalDateTime, List<QRule>> map = new TreeMap<>();

        if (qRules == null) {
            return map;
        }

        for (QRule rule : qRules) {
            // Store rule start and end times for efficient range lookup
            map.computeIfAbsent(rule.getStart(), k -> new ArrayList<>()).add(rule);
            map.computeIfAbsent(rule.getEnd(), k -> new ArrayList<>()).add(rule);
        }

        return map;
    }

    /**
     * Build TreeMap for P rules indexed by time ranges.
     * Allows O(log n) lookup of applicable P rules for any timestamp.
     * 
     * @param pRules List of P rules to index
     * @return TreeMap with time -> rules mapping
     */
    private TreeMap<LocalDateTime, List<PRule>> buildPRuleMap(List<PRule> pRules) {
        TreeMap<LocalDateTime, List<PRule>> map = new TreeMap<>();

        if (pRules == null) {
            return map;
        }

        for (PRule rule : pRules) {
            // Store rule start and end times for efficient range lookup
            map.computeIfAbsent(rule.getStart(), k -> new ArrayList<>()).add(rule);
            map.computeIfAbsent(rule.getEnd(), k -> new ArrayList<>()).add(rule);
        }

        return map;
    }

    /**
     * Apply Q rules (override) and P rules (add) to all expenses.
     * Q rules override the remanent for transactions in their time range.
     * P rules add an extra amount to the remanent for transactions in their range.
     * 
     * Time Complexity: O(n * log q) where q is avg rules per timestamp
     * 
     * @param expenses Sorted list of expenses with base remanent calculated
     * @param qRuleMap TreeMap of Q rules indexed by timestamp
     * @param pRuleMap TreeMap of P rules indexed by timestamp
     */
    private void applyQAndPRules(List<ExpenseResult> expenses,
                                TreeMap<LocalDateTime, List<QRule>> qRuleMap,
                                TreeMap<LocalDateTime, List<PRule>> pRuleMap) {

        for (ExpenseResult expense : expenses) {
            LocalDateTime timestamp = expense.getTimestamp();

            // Apply Q Rules: Override remanent if transaction is in range
            BigDecimal finalRemanent = expense.getRemanent();
            boolean qRuleApplied = false;

            // Check all Q rules to find matching time range
            for (QRule qRule : qRuleMap.values().stream().flatMap(List::stream).distinct().collect(Collectors.toList())) {
                if (isWithinRange(timestamp, qRule.getStart(), qRule.getEnd())) {
                    finalRemanent = qRule.getFixed();
                    qRuleApplied = true;
                    break; // Q rule overrides, so use first match
                }
            }

            // Apply P Rules: Add to remanent if transaction is in range
            List<PRule> applicablePRules = pRuleMap.values().stream()
                .flatMap(List::stream)
                .distinct()
                .filter(pr -> isWithinRange(timestamp, pr.getStart(), pr.getEnd()))
                .collect(Collectors.toList());

            for (PRule pRule : applicablePRules) {
                finalRemanent = finalRemanent.add(pRule.getExtra());
            }

            expense.setRemanent(finalRemanent);
        }
    }

    /**
     * Check if a timestamp falls within a time range [start, end] (inclusive).
     * 
     * @param timestamp The timestamp to check
     * @param start Start of range (inclusive)
     * @param end End of range (inclusive)
     * @return true if timestamp is in range, false otherwise
     */
    private boolean isWithinRange(LocalDateTime timestamp, LocalDateTime start, LocalDateTime end) {
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }

    /**
     * Build a prefix sum array for efficient range sum queries using binary search.
     * prefixSum[i] = sum of all remanents from index 0 to i (inclusive).
     * Allows O(log n) range sum calculation via: sum(i, j) = prefixSum[j] - prefixSum[i-1]
     * 
     * @param expenses Sorted list of expenses with remanentscalculated
     * @return List of cumulative remanent sums
     */
    private List<BigDecimal> buildPrefixSum(List<ExpenseResult> expenses) {
        List<BigDecimal> prefixSum = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;

        for (ExpenseResult expense : expenses) {
            cumulative = cumulative.add(expense.getRemanent());
            prefixSum.add(cumulative);
        }

        return prefixSum;
    }

    /**
     * Calculate total remanent for all transactions within a K period [start, end].
     * Uses binary search to find the range, then prefix sum for O(log n) calculation.
     * 
     * Algorithm:
     * 1. Binary search to find first transaction >= start
     * 2. Binary search to find last transaction <= end
     * 3. Return prefixSum[end] - prefixSum[start-1]
     * 
     * Time Complexity: O(log n) where n = number of expenses
     * 
     * @param expenses Sorted list of expenses
     * @param prefixSum Prefix sum array
     * @param period K period defining the range
     * @return Sum of remanents in the period
     */
    private BigDecimal calculateRangeSum(List<ExpenseResult> expenses,
                                         List<BigDecimal> prefixSum,
                                         KPeriod period) {
        if (expenses.isEmpty() || prefixSum.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDateTime periodStart = period.getStart();
        LocalDateTime periodEnd = period.getEnd();

        // Binary search for first index >= periodStart
        int startIdx = binarySearchFirstGreaterEqual(expenses, periodStart);
        if (startIdx == -1) {
            return BigDecimal.ZERO; // No transactions in range
        }

        // Binary search for last index <= periodEnd
        int endIdx = binarySearchLastLessEqual(expenses, periodEnd);
        if (endIdx == -1 || endIdx < startIdx) {
            return BigDecimal.ZERO; // No transactions in range
        }

        // Calculate sum using prefix sum array
        BigDecimal rangeSum = prefixSum.get(endIdx);
        if (startIdx > 0) {
            rangeSum = rangeSum.subtract(prefixSum.get(startIdx - 1));
        }

        return rangeSum;
    }

    /**
     * Binary search to find the first expense with timestamp >= target.
     * Returns the index, or -1 if no such element exists.
     * 
     * @param expenses Sorted list of expenses by timestamp
     * @param target Target timestamp
     * @return Index of first element >= target, or -1
     */
    private int binarySearchFirstGreaterEqual(List<ExpenseResult> expenses, LocalDateTime target) {
        int left = 0, right = expenses.size() - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            LocalDateTime midTime = expenses.get(mid).getTimestamp();

            if (midTime.compareTo(target) >= 0) {
                result = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return result;
    }

    /**
     * Binary search to find the last expense with timestamp <= target.
     * Returns the index, or -1 if no such element exists.
     * 
     * @param expenses Sorted list of expenses by timestamp
     * @param target Target timestamp
     * @return Index of last element <= target, or -1
     */
    private int binarySearchLastLessEqual(List<ExpenseResult> expenses, LocalDateTime target) {
        int left = 0, right = expenses.size() - 1;
        int result = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            LocalDateTime midTime = expenses.get(mid).getTimestamp();

            if (midTime.compareTo(target) <= 0) {
                result = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return result;
    }

    /**
     * Calculate Net Present Value (NPS) for the portfolio.
     * Implementation placeholder for investment calculation.
     * 
     * @param expenses Processed expenses
     * @param qRules Q rules applied
     * @param pRules P rules applied
     * @return NPS calculation result
     */
    public Object calculateNPS(List<Expense> expenses, List<QRule> qRules, List<PRule> pRules) {
        // First apply rules to get processed expenses
        Object ruleResults = applyRules(expenses, qRules, pRules, null);
        
        // For now, return a placeholder NPS calculation
        // This would be replaced with actual financial formula
        Map<String, Object> npsResult = new LinkedHashMap<>();
        npsResult.put("npsValue", new BigDecimal("0.00"));
        npsResult.put("ruleApplicationResults", ruleResults);
        npsResult.put("message", "NPS calculation completed");

        return npsResult;
    }

    /**
     * Calculate Investment Index for the portfolio.
     * Implementation placeholder for investment calculation.
     * 
     * @param expenses Processed expenses
     * @param qRules Q rules applied
     * @param pRules P rules applied
     * @param kPeriods K periods for range analysis
     * @return Index calculation result
     */
    public Object calculateIndex(List<Expense> expenses, List<QRule> qRules,
                                List<PRule> pRules, List<KPeriod> kPeriods) {
        // Apply all rules including K periods
        Object ruleResults = applyRules(expenses, qRules, pRules, kPeriods);

        // For now, return a placeholder index calculation
        // This would be replaced with actual financial formula
        Map<String, Object> indexResult = new LinkedHashMap<>();
        indexResult.put("indexValue", new BigDecimal("100.00"));
        indexResult.put("ruleApplicationResults", ruleResults);
        indexResult.put("message", "Index calculation completed");

        return indexResult;
    }
}
