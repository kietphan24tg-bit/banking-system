package com.kgdev.frauddetectionservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FraudCheckResult {
    boolean fraud;
    String reason;
}
