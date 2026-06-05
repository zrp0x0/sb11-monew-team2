package com.codeit.monew.batch.restore.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.codeit.monew.batch.backup.dto.ArticleBackupDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArticleRestoreBatchConfig {

  private final ArticleRepository articleRepository;
  private final AmazonS3 amazonS3;

  @Value("${batch.chunk-size:1000}")
  private int chunkSize;

  // TODO: 환경변수에서 값을 가져오도록 세팅
  @Value("${aws.s3.bucket:dummy-bucket}")
  private String bucketName;

  private static final String TEMP_FILE_PATH_KEY = "TEMP_FILE_PATH";

  @Bean
  public Job articleRestoreJob(JobRepository jobRepository,
      Step downloadFromS3Step,
      Step restoreToDbStep) {
    return new JobBuilder("articleRestoreJob", jobRepository)
        .start(downloadFromS3Step)
        .next(restoreToDbStep)
        .build();
  }

  @Bean
  public Step downloadFromS3Step(JobRepository jobRepository, PlatformTransactionManager ptm) {
    return new StepBuilder("downloadFromS3Step", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          String targetDate = chunkContext.getStepContext().getJobParameters().get("targetDate").toString();
          String s3Key = "article/" + targetDate + ".json";
          String tempPath = getTempFilePath(targetDate);

          // TODO: S3에 해당 날짜의 백업 파일이 있는지 먼저 체크 후, 없으면 스킵(정상 종료)하도록 구현
          if (!amazonS3.doesObjectExist(bucketName, s3Key)) {
            log.warn("[{}] S3에 해당 날짜 백업 파일이 존재하지 않아 다운로드를 건너뜁니다. (s3Key: {})", targetDate, s3Key);
            return RepeatStatus.FINISHED;
          }

          log.info("S3 다운로드 시작: s3://{}/{} -> 로컬 경로: {}", bucketName, s3Key, tempPath);

          S3Object s3Object = amazonS3.getObject(bucketName, s3Key);

          File localTempFile = new File(tempPath);
          Files.copy(s3Object.getObjectContent(), localTempFile.toPath(),
              StandardCopyOption.REPLACE_EXISTING);

          chunkContext.getStepContext().getStepExecution().getJobExecution()
              .getExecutionContext().put(TEMP_FILE_PATH_KEY, tempPath);

          return RepeatStatus.FINISHED;
        }, ptm)
        .build();
  }

  @Bean
  public Step restoreToDbStep(JobRepository jobRepository, PlatformTransactionManager ptm) {
    return new StepBuilder("restoreToDbStep", jobRepository)
        .<ArticleBackupDto, Article>chunk(chunkSize, ptm)
        .reader(articleJsonReader(null))
        .processor(articleRestoreProcessor())
        .writer(articleDbWriter(null))
        .listener(new StepExecutionListener() {
          @Override
          public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
            String tempPath = stepExecution.getJobExecution().getExecutionContext().getString(TEMP_FILE_PATH_KEY);

            if (tempPath != null) {
              File localTempFile = new File(tempPath);
              if (localTempFile.exists()) {
                boolean deleted = localTempFile.delete();
                log.info("로컬 임시 파일 삭제 {}: {}", deleted ? "성공" : "실패", tempPath);
              }
            }

            return StepExecutionListener.super.afterStep(stepExecution);
          }
        })
        .build();
  }

  @Bean
  @StepScope
  public JsonItemReader<ArticleBackupDto> articleJsonReader(
      @Value("#{jobParameters['targetDate']}") String targetDate) {
    String tempPath = getTempFilePath(targetDate);

    // S3에서 내려준 JSON 파일의 LocalDateTime 필드를 위한 날짜 변환 모듈
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    JacksonJsonObjectReader<ArticleBackupDto> jsonObjectReader = new JacksonJsonObjectReader<>(ArticleBackupDto.class);
    jsonObjectReader.setMapper(objectMapper);

    return new JsonItemReaderBuilder<ArticleBackupDto>()
        .jsonObjectReader(jsonObjectReader)
        .resource(new FileSystemResource(tempPath))
        .name("articleJsonReader")
        // TODO: 파일이 없어도 에러 내지 말고 조용히 넘어가도록 구현
        .strict(false)
        .build();
  }

  @Bean
  public ItemProcessor<ArticleBackupDto, Article> articleRestoreProcessor() {
    return dto -> {
      return Article.create(
          dto.source(),
          dto.sourceUrl(),
          dto.title(),
          dto.summary(),
          dto.publishDate()
      );
    };
  }

  @Bean
  @StepScope
  public ItemWriter<Article> articleDbWriter(
      @Value("#{stepExecution}") StepExecution stepExecution
  ) {
    return articles -> {
      var jobContext = stepExecution.getJobExecution().getExecutionContext();

      List<String> restoredIds = Optional.ofNullable((List<String>) jobContext.get("RESTORED_ARTICLE_IDS"))
          .orElse(new ArrayList<>());

      List<Article> articlesToSave = new ArrayList<>();
      for (Article article : articles) {
        if (!articleRepository.existsBySourceUrl(article.getSourceUrl())) {
          articlesToSave.add(article);
        }
      }

      if (!articlesToSave.isEmpty()) {
        List<Article> savedArticles = articleRepository.saveAll(articlesToSave);
        savedArticles.forEach(a -> restoredIds.add(a.getId().toString()));
      }

      jobContext.put("RESTORED_ARTICLE_IDS", restoredIds);
    };
  }

  private String getTempFilePath(String targetDate) {
    String tmpDir = System.getProperty("java.io.tmpdir");
    return tmpDir + File.separator + "monew_restore_" + targetDate + ".json";
  }
}
