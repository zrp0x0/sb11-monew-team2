package com.codeit.monew.domain.interest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.monew.domain.interest.dto.request.InterestRegisterRequest;
import com.codeit.monew.domain.interest.dto.response.InterestResponse;
import com.codeit.monew.domain.interest.exception.InterestErrorCode;
import com.codeit.monew.domain.interest.exception.InterestException;
import com.codeit.monew.domain.interest.service.InterestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InterestController.class)
@AutoConfigureMockMvc(addFilters = false)
public class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private InterestService interestService;

  @Test
  @DisplayName("관심사 생성 성공 - 유효한 요청 시 201 Created를 반환한다.")
  void createInterest_returns201() throws Exception {
    InterestRegisterRequest request = new InterestRegisterRequest("관심사", List.of("키워드"));

    UUID newId = UUID.randomUUID();
    InterestResponse mockResponse = new InterestResponse(
        newId, "관심사", List.of("키워드"), 0L, false
    );

    given(interestService.createInterest(any(InterestRegisterRequest.class)))
        .willReturn(mockResponse);

    mockMvc.perform(
            post("/api/interests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(newId.toString()))
        .andExpect(jsonPath("$.name").value("관심사"))
        .andExpect(jsonPath("$.keywords[0]").value("키워드"))
        .andExpect(jsonPath("$.subscriberCount").value(0L))
        .andExpect(jsonPath("$.subscribedByMe").value(false));
  }

  @Test
  @DisplayName("관심사 생성 실패 1 - 이름이 누락되거나 빈 값이면 400 Bad Request를 반환한다.")
  void createInterest_fail_emptyName() throws Exception {
    InterestRegisterRequest request = new InterestRegisterRequest("", List.of("키워드"));

    mockMvc.perform(
            post("/api/interests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("관심사 생성 실패 2 - 키워드 리스트가 비어있으면 400 Bad Request를 반환한다.")
  void createInterest_fail_emptyKeywords() throws Exception {
    InterestRegisterRequest request = new InterestRegisterRequest("관심사", List.of());

    mockMvc.perform(
            post("/api/interests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("관심사 생성 실패 3 - 이미 존재하는 관심사(INTEREST_ALREADY_EXISTS) 등록 시 409 Conflict를 반환한다.")
  void createInterest_fail_alreadyExists() throws Exception {
    InterestRegisterRequest request = new InterestRegisterRequest("중복관심사", List.of("키워드"));

    given(interestService.createInterest(any()))
        .willThrow(new InterestException(InterestErrorCode.INTEREST_ALREADY_EXISTS, Map.of()));

    mockMvc.perform(
            post("/api/interests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("관심사 삭제 성공 - 정상 삭제 완료 시 204 No Content를 반환한다.")
  void deleteInterest_returns204() throws Exception {
    UUID interestId = UUID.randomUUID();

    mockMvc.perform(
            delete("/api/interests/{interestId}", interestId)
        )
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("관심사 삭제 실패 - 존재하지 않는 관심사 삭제(INTEREST_NOT_FOUND) 시 404 Not Found를 반환한다.")
  void deleteInterest_interestNotFound() throws Exception {
    UUID interestId = UUID.randomUUID();

    doThrow(new InterestException(InterestErrorCode.INTEREST_NOT_FOUND, Map.of()))
        .when(interestService).deleteInterest(interestId);

    mockMvc.perform(
            delete("/api/interests/{interestId}", interestId)
        )
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("관심사 조회 실패 - 잘못된 커서 데이터 유입(INVALID_CURSOR_FORMAT) 시 400 Bad Request를 반환한다.")
  void searchInterest_fail_invalidCursorFormat() throws Exception {
    UUID userId = UUID.randomUUID();

    given(interestService.searchInterest(any(), any()))
            .willThrow(new InterestException(InterestErrorCode.INVALID_CURSOR_FORMAT, Map.of()));

    mockMvc.perform(
        get("/api/interests")
            .header("User-Id", userId.toString())
            .param("cursor", "잘못된데이터")
    )
        .andExpect(status().isBadRequest());
  }
}