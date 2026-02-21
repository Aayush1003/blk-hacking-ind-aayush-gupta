package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceMetrics {
    private String executionTime;
    private String memoryUsedMB;
    private Integer activeThreads;
    private LocalDateTime timestamp;
}