package com.blackrock.challenge.controller;

import com.blackrock.challenge.model.dto.*;
import com.blackrock.challenge.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Transaction Controller for BlackRock Retirement Challenge.
 * Handles validation, filtering, and investment calculations
 * for micro-investment transactions.
 */
@RestController
@RequestMapping("/blackrock/challenge/v1")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    /**
     * Endpoint: POST /transactions:validator
     * Validates expenses for negative amounts and duplicate timestamps.
     * 
     * @param request TransactionRequest containing expenses to validate
     * @return ResponseEntity with ValidationResponse (400 if invalid, 200 if valid)
     */
    @PostMapping("/transactions:validator")
    public ResponseEntity<ValidationResponse> validateTransactions(@RequestBody TransactionRequest request) {
        ValidationResponse response = transactionService.validateTransactions(request.getExpenses());
        
        if (!response.isValid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint: POST /transactions:filter
     * Applies Q, P, and K rules to the expenses.
     * Q rules override remanent, P rules add to remanent, K periods aggregate ranges.
     * 
     * @param request TransactionRequest containing expenses and all rules
     * @return ResponseEntity with filtered/processed expenses
     */
    @PostMapping("/transactions:filter")
    public ResponseEntity<TransactionResponse> filterTransactions(@RequestBody TransactionRequest request) {
        try {
            Object result = transactionService.applyRules(
                request.getExpenses(),
                request.getQRules(),
                request.getPRules(),
                request.getKPeriods()
            );
            
            TransactionResponse response = TransactionResponse.builder()
                .success(true)
                .message("Rules applied successfully")
                .data(result)
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TransactionResponse response = TransactionResponse.builder()
                .success(false)
                .message("Error applying rules: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Endpoint: POST /returns:nps
     * Calculates Net Present Value (NPS) for the investment portfolio
     * based on filtered transactions and rules.
     * 
     * @param request TransactionRequest containing expenses and rules
     * @return ResponseEntity with NPS calculation result
     */
    @PostMapping("/returns:nps")
    public ResponseEntity<TransactionResponse> calculateNPS(@RequestBody TransactionRequest request) {
        try {
            Object result = transactionService.calculateNPS(
                request.getExpenses(),
                request.getQRules(),
                request.getPRules()
            );
            
            TransactionResponse response = TransactionResponse.builder()
                .success(true)
                .message("NPS calculation completed")
                .data(result)
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TransactionResponse response = TransactionResponse.builder()
                .success(false)
                .message("Error calculating NPS: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Endpoint: POST /returns:index
     * Calculates investment index/portfolio metric based on the filtered
     * transactions, rules, and K periods.
     * 
     * @param request TransactionRequest containing expenses and rules
     * @return ResponseEntity with index calculation result
     */
    @PostMapping("/returns:index")
    public ResponseEntity<TransactionResponse> calculateIndex(@RequestBody TransactionRequest request) {
        try {
            Object result = transactionService.calculateIndex(
                request.getExpenses(),
                request.getQRules(),
                request.getPRules(),
                request.getKPeriods()
            );
            
            TransactionResponse response = TransactionResponse.builder()
                .success(true)
                .message("Index calculation completed")
                .data(result)
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            TransactionResponse response = TransactionResponse.builder()
                .success(false)
                .message("Error calculating index: " + e.getMessage())
                .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}