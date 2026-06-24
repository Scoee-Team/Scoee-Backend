package com.example.scoremate.domain.ranking.controller;

import com.example.scoremate.domain.ranking.dto.RankingDtos.*;
import com.example.scoremate.domain.ranking.service.RankingService;
import com.example.scoremate.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Rankings")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rankings")
public class RankingController {
    private final RankingService rankingService;

    @Operation(summary = "방 랭킹", description = "ROOM_NOT_FOUND, NOT_ROOM_PARTICIPANT, RESULT_NOT_READY")
    @GetMapping("/rooms/{roomId}")
    public ApiResponse<RoomRankingResponse> roomRanking(@PathVariable Long roomId) {
        return ApiResponse.success(rankingService.roomRanking(roomId));
    }

    @Operation(summary = "내 랭킹/기록", description = "AUTH_REQUIRED")
    @GetMapping("/me")
    public ApiResponse<MyRankingResponse> me(@RequestParam(defaultValue = "ALL") RankingPeriod period) {
        return ApiResponse.success(rankingService.me(period));
    }
}
