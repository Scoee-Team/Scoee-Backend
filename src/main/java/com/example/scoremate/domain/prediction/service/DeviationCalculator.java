package com.example.scoremate.domain.prediction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeviationCalculator {
    private final int missingPredictionPenalty;

    public DeviationCalculator(@Value("${scoremate.prediction.missing-penalty:10}") int missingPredictionPenalty) {
        this.missingPredictionPenalty = missingPredictionPenalty;
    }

    public int calculate(int predictedHomeScore, int predictedAwayScore, int actualHomeScore, int actualAwayScore) {
        return Math.abs(predictedHomeScore - actualHomeScore) + Math.abs(predictedAwayScore - actualAwayScore);
    }

    public int missingPredictionPenalty() {
        return missingPredictionPenalty;
    }
}
