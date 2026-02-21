package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Result DTO representing an expense after rule application.
 * Contains the original amount, ceiling (rounded), remanent (after Q/P rules),
 * and timestamp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResult {
    private BigDecimal amount;
    private BigDecimal ceiling;
    private BigDecimal remanent;
    private LocalDateTime timestamp;
}
