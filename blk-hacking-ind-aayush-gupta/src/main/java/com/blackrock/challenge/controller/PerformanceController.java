package com.blackrock.challenge.controller;

import com.blackrock.challenge.model.dto.PerformanceMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class PerformanceController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @GetMapping("/blackrock/challenge/v1/performance")
    public ResponseEntity<PerformanceMetrics> getPerformanceMetrics() {
        String executionTime = LocalDateTime.now().format(TIME_FORMATTER);
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        double memoryUsedMB = usedMemory / (1024.0 * 1024.0);
        int activeThreads = Thread.activeCount();

        PerformanceMetrics metrics = PerformanceMetrics.builder()
            .executionTime(executionTime)
            .memoryUsedMB(String.format("%.2f", memoryUsedMB))
            .activeThreads(activeThreads)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.ok(metrics);
    }
}