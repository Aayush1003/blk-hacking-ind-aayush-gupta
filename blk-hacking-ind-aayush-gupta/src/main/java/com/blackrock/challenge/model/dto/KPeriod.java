package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * K Period: Defines a time range for which total remanent is calculated.
 * Used to query the sum of remanents for all transactions within [start, end].
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KPeriod {
    private LocalDateTime start;
    private LocalDateTime end;
}
