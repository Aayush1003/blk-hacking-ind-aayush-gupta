package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Q Rule: Overrides the remanent for transactions within the time range [start, end].
 * For expenses with timestamp in this range, the remanent (ceiling - amount)
 * is replaced with the fixed value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QRule {
    private BigDecimal fixed;
    private LocalDateTime start;
    private LocalDateTime end;
}
