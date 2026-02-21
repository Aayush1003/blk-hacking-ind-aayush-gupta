package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * P Rule: Adds an extra amount to the remanent for transactions within the time range [start, end].
 * For expenses with timestamp in this range, the remanent is increased by the extra value.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PRule {
    private BigDecimal extra;
    private LocalDateTime start;
    private LocalDateTime end;
}
