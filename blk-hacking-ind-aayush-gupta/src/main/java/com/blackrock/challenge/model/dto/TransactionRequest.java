package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Request wrapper for transaction operations.
 * Contains expenses and the rules (Q, P, K) to be applied.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    private List<Expense> expenses;
    private List<QRule> qRules;
    private List<PRule> pRules;
    private List<KPeriod> kPeriods;
}
