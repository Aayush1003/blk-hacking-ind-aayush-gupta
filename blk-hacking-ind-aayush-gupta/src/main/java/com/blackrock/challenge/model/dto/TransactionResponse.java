package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response wrapper for transaction operations.
 * Includes status, message, and generic data payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private boolean success;
    private String message;
    private Object data;
}
