package com.example.scoremate.domain.predictionroom.controller;

import com.example.scoremate.domain.predictionroom.dto.PredictionRoomDtos.*;
import com.example.scoremate.domain.predictionroom.service.PredictionRoomService;
import com.example.scoremate.global.response.ApiResponse;
import com.example.scoremate.global.response.PageResponse;
import com.example.scoremate.global.swagger.SwaggerErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Prediction Rooms")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/prediction-rooms")
public class PredictionRoomController {
    private final PredictionRoomService predictionRoomService;

    @Operation(summary = "내 예측방 목록", description = SwaggerErrorExamples.AUTH_AND_DOMAIN)
    @GetMapping("/me")
    public ApiResponse<PageResponse<PredictionRoomSummaryResponse>> myRooms(@RequestParam(defaultValue = "ALL") PredictionRoomFilter filter, Pageable pageable) {
        return ApiResponse.success(predictionRoomService.myRooms(filter, pageable));
    }

    @Operation(summary = "예측방 생성", description = "INVALID_ROOM_REQUEST, MATCH_NOT_FOUND, MATCH_LOCKED, PREDICTION_CLOSED")
    @PostMapping
    public ApiResponse<CreatePredictionRoomResponse> create(@Valid @RequestBody CreatePredictionRoomRequest request) {
        return ApiResponse.success(predictionRoomService.create(request));
    }

    @Operation(summary = "예측방 상세", description = "ROOM_NOT_FOUND, NOT_ROOM_PARTICIPANT")
    @GetMapping("/{roomId}")
    public ApiResponse<PredictionRoomDetailResponse> detail(@PathVariable Long roomId) {
        return ApiResponse.success(predictionRoomService.detail(roomId));
    }

    @Operation(summary = "초대 링크 생성/조회", description = "ROOM_NOT_FOUND, NOT_ROOM_PARTICIPANT")
    @PostMapping("/{roomId}/invite-link")
    public ApiResponse<InviteLinkResponse> inviteLink(@PathVariable Long roomId) {
        return ApiResponse.success(predictionRoomService.inviteLink(roomId));
    }

    @Operation(summary = "초대 코드 미리보기", description = "ROOM_NOT_FOUND")
    @GetMapping("/invite/{inviteCode}")
    public ApiResponse<PredictionRoomDetailResponse> invitePreview(@PathVariable String inviteCode) {
        return ApiResponse.success(predictionRoomService.invitePreview(inviteCode));
    }

    @Operation(summary = "초대 코드로 참여", description = "ROOM_NOT_FOUND, ALREADY_JOINED, ROOM_FULL, ROOM_CANCELLED")
    @PostMapping("/join")
    public ApiResponse<Void> join(@Valid @RequestBody JoinPredictionRoomRequest request) {
        predictionRoomService.join(request);
        return ApiResponse.success();
    }
}
