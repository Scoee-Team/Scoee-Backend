package com.example.scoremate.domain.prediction.controller;

import com.example.scoremate.domain.prediction.dto.PredictionDtos.PredictionRoomResultResponse;
import com.example.scoremate.domain.prediction.service.PredictionResultService;
import com.example.scoremate.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminPredictionController {
    private final PredictionResultService predictionResultService;

    @Operation(summary = "예측방 결과 계산", description = "RESULT_NOT_READY, ROOM_NOT_FOUND. idempotent하게 기존 결과를 갱신합니다.")
    @PostMapping("/prediction-rooms/{roomId}/calculate-result")
    public ApiResponse<PredictionRoomResultResponse> calculate(@PathVariable Long roomId) {
        return ApiResponse.success(predictionResultService.calculateAsAdmin(roomId));
    }
}
