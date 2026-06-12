package com.codeit.monew.batch.backup.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.s3.AmazonS3;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.entity.ArticleSource;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.ExitStatus;
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
public class ArticleBackupBatchConfigTest {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private JobRepositoryTestUtils jobRepositoryTestUtils;

  @Autowired
  private ArticleRepository articleRepository;

  @Autowired
  private Job articleBackupJob;

  // 가짜 S3 객체 주입
  @MockitoBean
  private AmazonS3 amazonS3;

  @BeforeEach
  void setUpJob() {
    jobLauncherTestUtils.setJob(articleBackupJob);
  }

  @AfterEach
  void tearDown() {
    jobRepositoryTestUtils.removeJobExecutions();
    articleRepository.deleteAll(); // 테스트가 끝날 때마다 DB 초기화
  }

  @Test
  @DisplayName("기사 백업 Job - DB 데이터를 읽어 S3에 업로드(Mock)하고 로컬 파일을 삭제한다.")
  void articleBackUpJob_Success() throws Exception {
    //given
    String targetDate = "2026-01-01";
    LocalDateTime publishedAt = LocalDateTime.of(2026, 1, 1, 1, 0);

    for (int i = 1; i <= 5; i++) {
      articleRepository.save(Article.create(
          ArticleSource.NAVER, "http://test.com/" + i, "태스트 기사 " + i, "요약", publishedAt
      ));
    }

    JobParameters jobParameters = new JobParametersBuilder()
        .addString("targetDate", targetDate)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    String tmpDir = System.getProperty("java.io.tmpdir");
    File tempPathFile = new File(tmpDir + File.separator + "monew_backup_" + targetDate + ".json");

    //when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

    //then
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    verify(amazonS3).putObject(eq("dummy-bucket"), eq("article/" + targetDate+ ".json"), any(File.class));
    assertThat(tempPathFile.exists()).isFalse();
  }

  @Test
  @DisplayName("청크(Chunk) 테스트 - 8건의 데이터가 청크 사이즈(3)에 맞춰 3번의 쓰기 작업으로 나뉘어 처리된다.")
  void articleBackupJob_ChunkTest() throws Exception {
    //given
    String targetDate = LocalDate.now().toString();
    LocalDateTime publishDate = LocalDateTime.of(2026, 1, 1, 10, 0);

    for (int i = 0; i < 8; i++) {
      articleRepository.save(Article.create(
          ArticleSource.NAVER, "http://test.com/chunk"+ i, "청크 백업 기사 " + i, "요약", publishDate
      ));
    }

    JobParameters jobParameters = new JobParametersBuilder()
        .addString("targetDate", targetDate)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    //when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

    //then
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();

    assertThat(stepExecution.getReadCount()).isEqualTo(8);
    assertThat(stepExecution.getWriteCount()).isEqualTo(8);
    assertThat(stepExecution.getCommitCount()).isEqualTo(3); // 트랜잭션 커밋 횟수 검증
  }
}
