package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Expense DTO representing a single transaction.
 * Amount is in currency units (will be rounded up to nearest 100).
 * Timestamp must be unique across all expenses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
