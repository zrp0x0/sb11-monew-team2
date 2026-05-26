package com.codeit.monew.domain.interest.controller;

import com.codeit.monew.domain.interest.dto.request.InterestUpdateRequest;
import com.codeit.monew.domain.interest.dto.response.InterestResponse;
import com.codeit.monew.domain.interest.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interests")
public class InterestController {

  private final InterestService interestService;

  @Operation(summary = "관심사 정보 수정", description = "관심사의 키워드를 수정합니다.", operationId = "update_1")
  @PatchMapping("/{interestId}")
  @ResponseStatus(HttpStatus.OK)
  public InterestResponse update(
      @PathVariable UUID interestId,
      @Valid @RequestBody InterestUpdateRequest request) {
    return interestService.update(interestId, request);
  }
}
