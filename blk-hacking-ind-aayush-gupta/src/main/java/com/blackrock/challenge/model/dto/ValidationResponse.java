package com.blackrock.challenge.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Validation response DTO.
 * Contains validation status and list of any errors found.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResponse {
    private boolean valid;
    private String message;
    private List<String> errors;
}
