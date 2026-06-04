package com.codeit.monew.batch.restore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class ArticleRestoreBatchConfigTest {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private JobRepositoryTestUtils jobRepositoryTestUtils;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private Job articleRestoreJob;

  @MockitoBean
  private AmazonS3 amazonS3;

  @BeforeEach
  void setUpJob() {
    jobLauncherTestUtils.setJob(articleRestoreJob);
  }

  @AfterEach
  void tearDown() {
    jobRepositoryTestUtils.removeJobExecutions();
    articleRepository.deleteAll();
  }

  @Test
  @DisplayName("기사 복구 Job - S3에서 파일을 다운받아 DB에 넣되, 이미 존재하는 기사는 Skip 한다.")
  void articleRestoreJob_SkipExisting_Success() throws Exception {
    //given
    String targetDate = "2026-01-01";

    Article existingArticle = articleRepository.saveAndFlush(Article.create(
        ArticleSource.NAVER, "http://test.com/existing", "기존 기사", "요약",
        LocalDateTime.of(2026, 1, 1, 10, 0)
    ));

    // S3에서 내려받은 것처럼 꾸밀 가짜 JSON 문자열 생성 (기존 기사 1, 유실된 기사 2)
    String mockJsonData = """
        [
          {"id":"%s", "source":"NAVER", "title":"기존 기사", "summary":"요약", "sourceUrl":"http://test.com/existing", "publishDate":"2026-01-01T10:00:00"},
          {"id":"%s", "source":"NAVER", "title":"유실된 기사 1", "summary":"요약 1", "sourceUrl":"http://test.com/lost1", "publishDate":"2026-01-01T11:00:00"},
          {"id":"%s", "source":"NAVER", "title":"유실된 기사 2", "summary":"요약 2", "sourceUrl":"http://test.com/lost2", "publishDate":"2026-01-01T12:00:00"}
        ]
        """.formatted(existingArticle.getId().toString(), UUID.randomUUID().toString(),
        UUID.randomUUID().toString());

    // S3의 getObject() 호출 시, 가짜 JSON 문자열을 파일처럼 반환하도록 모킹
    S3Object mockS3Object = new S3Object();
    mockS3Object.setObjectContent(new S3ObjectInputStream(
        new ByteArrayInputStream(mockJsonData.getBytes(StandardCharsets.UTF_8)), null)
    );

    given(amazonS3.doesObjectExist(anyString(), anyString()))
        .willReturn(true);
    given(amazonS3.getObject(anyString(), anyString()))
        .willReturn(mockS3Object);

    JobParameters jobParameters = new JobParametersBuilder()
        .addString("targetDate", targetDate)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    String tmpDir = System.getProperty("java.io.tmpdir");
    File tempPathFile = new File(tmpDir + File.separator + "monew_restore_" + targetDate + ".json");

    //when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

    //then
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    assertThat(articleRepository.count()).isEqualTo(3);

    List<String> restoreIds = (List<String>) jobExecution.getExecutionContext().get("RESTORED_ARTICLE_IDS");
    assertThat(restoreIds).isNotNull();
    assertThat(restoreIds).hasSize(2);

    assertThat(tempPathFile.exists()).isFalse();
  }

  @Test
  @DisplayName("청크(Chunk) 테스트 - S3에서 8건의 데이터를 받아 청크 사이즈(3)에 맞춰 3번 이상의 쓰기 작업으로 처리된다.")
  void articleRestoreJob_ChunkTest() throws Exception {
    // given
    String targetDate = "2026-01-01";

    // S3에서 내려받은 것처럼 꾸밀 8개의 가짜 JSON 데이터 생성
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("[\n");
    for (int i = 1; i <= 8; i++) {
      jsonBuilder.append(String.format(
          "{\"id\":\"%s\", \"source\":\"NAVER\", \"title\":\"청크 복구 기사 %d\", \"summary\":\"요약 %d\", \"sourceUrl\":\"http://test.com/chunk-restore%d\", \"publishDate\":\"2026-01-01T10:00:00\"}",
          UUID.randomUUID().toString(), i, i, i
      ));
      if (i < 8) {
        jsonBuilder.append(",\n");
      } else {
        jsonBuilder.append("\n");
      }
    }
    jsonBuilder.append("]");
    String mockJsonData = jsonBuilder.toString();

    // S3 모킹
    S3Object mockS3Object = new S3Object();
    mockS3Object.setObjectContent(new S3ObjectInputStream(
        new ByteArrayInputStream(mockJsonData.getBytes(StandardCharsets.UTF_8)), null)
    );
    
    given(amazonS3.doesObjectExist(anyString(), anyString()))
        .willReturn(true);
    given(amazonS3.getObject(anyString(), anyString())).willReturn(mockS3Object);

    JobParameters jobParameters = new JobParametersBuilder()
        .addString("targetDate", targetDate)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

    // then
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    assertThat(articleRepository.count()).isEqualTo(8);

    // 복구 작업은 S3를 다운로드 하는 Step과 DB로 복구하는 스텝으로 나뉘어 있음 -> 청크 처리를 담당하는 DB 복구 스텝을 찾아 검증
    StepExecution restoreStepExecution = jobExecution.getStepExecutions().stream()
        .filter(step -> step.getStepName().equals("restoreToDbStep"))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("restoreToDbStep을 찾을 수 없습니다."));

    assertThat(restoreStepExecution.getReadCount()).isEqualTo(8);
    assertThat(restoreStepExecution.getWriteCount()).isEqualTo(8);

    assertThat(restoreStepExecution.getCommitCount()).isEqualTo(3);
  }
}
