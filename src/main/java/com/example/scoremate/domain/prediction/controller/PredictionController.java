package com.example.scoremate.domain.prediction.controller;

import com.example.scoremate.domain.prediction.dto.PredictionDtos.*;
import com.example.scoremate.domain.prediction.service.PredictionResultService;
import com.example.scoremate.domain.prediction.service.ScorePredictionService;
import com.example.scoremate.global.response.ApiResponse;
import com.example.scoremate.global.swagger.SwaggerErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Predictions")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prediction-rooms/{roomId}")
public class PredictionController {
    private final ScorePredictionService scorePredictionService;
    private final PredictionResultService predictionResultService;

    @Operation(summary = "내 예측 조회", description = "ROOM_NOT_FOUND, NOT_ROOM_PARTICIPANT")
    @GetMapping("/predictions/me")
    public ApiResponse<MyPredictionsResponse> myPredictions(@PathVariable Long roomId) {
        return ApiResponse.success(scorePredictionService.myPredictions(roomId));
    }

    @Operation(summary = "예측 제출", description = "INVALID_SCORE, MATCH_NOT_IN_ROOM, PREDICTION_CLOSED, NOT_ROOM_PARTICIPANT")
    @PostMapping("/predictions")
    public ApiResponse<SubmitPredictionsResponse> postPredictions(@PathVariable Long roomId, @Valid @RequestBody SubmitPredictionsRequest request) {
        return ApiResponse.success(scorePredictionService.submit(roomId, request));
    }

    @Operation(summary = "예측 수정", description = "INVALID_SCORE, MATCH_NOT_IN_ROOM, PREDICTION_CLOSED, NOT_ROOM_PARTICIPANT")
    @PutMapping("/predictions")
    public ApiResponse<SubmitPredictionsResponse> putPredictions(@PathVariable Long roomId, @Valid @RequestBody SubmitPredictionsRequest request) {
        return ApiResponse.success(scorePredictionService.submit(roomId, request));
    }

    @Operation(summary = "예측방 결과", description = SwaggerErrorExamples.AUTH_AND_DOMAIN)
    @GetMapping("/results")
    public ApiResponse<PredictionRoomResultResponse> result(@PathVariable Long roomId) {
        return ApiResponse.success(predictionResultService.get(roomId));
    }
}
