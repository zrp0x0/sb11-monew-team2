package com.codeit.monew.batch.restore.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.codeit.monew.batch.backup.dto.ArticleBackupDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleInterestRepository;
import com.codeit.monew.domain.article.repository.ArticleRepository;
import com.codeit.monew.domain.interest.entity.Interest;
import com.codeit.monew.domain.interest.repository.InterestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final ArticleInterestRepository articleInterestRepository;
  private final InterestRepository interestRepository;
  private final AmazonS3 amazonS3;

  @Value("${batch.chunk-size:500}")
  private int chunkSize;

  @Value("${aws.s3.bucket}")
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
          String targetDate = chunkContext.getStepContext().getJobParameters().get("targetDate")
              .toString();
          String s3Key = "article/" + targetDate + ".json";
          String tempPath = getTempFilePath(targetDate);

          // S3에 해당 날짜의 백업 파일이 없는 경우
          if (!amazonS3.doesObjectExist(bucketName, s3Key)) {
            log.warn("[{}] S3 백업 파일 미존재 (대상 Key: {}) - 다운로드를 스킵합니다.", targetDate, s3Key);

            // JsonItemReader의 NPE 방지를 위한 빈 JSON 배열([]) 파일 생성
            Files.writeString(new File(tempPath).toPath(), "[]");

            chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                .put(TEMP_FILE_PATH_KEY, tempPath);

            return RepeatStatus.FINISHED;
          }

          log.info("[{}] S3 백업 파일 다운로드 시작 (Key: {} -> Path: {})", targetDate, s3Key, tempPath);

          S3Object s3Object = amazonS3.getObject(bucketName, s3Key);
          File localTempFile = new File(tempPath);

          try (var inputStream = s3Object.getObjectContent()) {
            Files.copy(inputStream, localTempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
          }
          log.info("[{}] S3 백업 파일 로컬 저장 완료", targetDate);

          chunkContext.getStepContext().getStepExecution().getJobExecution()
              .getExecutionContext().put(TEMP_FILE_PATH_KEY, tempPath);

          return RepeatStatus.FINISHED;
        }, ptm)
        .build();
  }

  @Bean
  public Step restoreToDbStep(JobRepository jobRepository, PlatformTransactionManager ptm) {
    return new StepBuilder("restoreToDbStep", jobRepository)
        .<ArticleBackupDto, ArticleBackupDto>chunk(chunkSize, ptm)
        .reader(articleJsonReader(null))
        .processor(articleRestoreProcessor())
        .writer(articleDbWriter(null))
        .listener(new StepExecutionListener() {
          @Override
          public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
            String tempPath = stepExecution.getJobExecution().getExecutionContext()
                .getString(TEMP_FILE_PATH_KEY);
            String targetDate = stepExecution.getJobParameters().getString("targetDate");

            if (tempPath != null) {
              File localTempFile = new File(tempPath);
              if (localTempFile.exists()) {
                boolean deleted = localTempFile.delete();
                log.info("[{}] 로컬 임시 파일 삭제 {}: {}", targetDate, deleted ? "성공" : "실패", tempPath);}
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

    JacksonJsonObjectReader<ArticleBackupDto> jsonObjectReader = new JacksonJsonObjectReader<>(
        ArticleBackupDto.class);
    jsonObjectReader.setMapper(objectMapper);

    return new JsonItemReaderBuilder<ArticleBackupDto>()
        .jsonObjectReader(jsonObjectReader)
        .resource(new FileSystemResource(tempPath))
        .name("articleJsonReader")
        .strict(false) // 파일 내용이 비어있더라도 예외를 발생시키지 않고 스킵하도록 설정
        .build();
  }

  @Bean
  public ItemProcessor<ArticleBackupDto, ArticleBackupDto> articleRestoreProcessor() {
    return dto -> dto;
  }

  @Bean
  @StepScope
  public ItemWriter<ArticleBackupDto> articleDbWriter(
      @Value("#{stepExecution}") StepExecution stepExecution) {
    return dtos -> {
      var jobContext = stepExecution.getJobExecution().getExecutionContext();
      String targetDate = stepExecution.getJobParameters().getString("targetDate");

      List<String> restoredIds = Optional.ofNullable(
              (List<String>) jobContext.get("RESTORED_ARTICLE_IDS"))
          .orElse(new ArrayList<>());

      // 현재 청크에 존재하는 모든 관심사 ID 수집
      Set<UUID> allInterestIdsInChunk = dtos.getItems().stream()
          .filter(dto -> dto.interestIds() != null)
          .flatMap(dto -> dto.interestIds().stream())
          .collect(Collectors.toSet());

      // 수집된 ID 중 실제 DB에 살아있는 관심사 ID만 딱 1번의 쿼리로 미리 가져옴
      Set<UUID> aliveInterestIds = new HashSet<>();
      if (!allInterestIdsInChunk.isEmpty()) {
        aliveInterestIds = interestRepository.findAllById(allInterestIdsInChunk).stream()
            .map(Interest::getId)
            .collect(Collectors.toSet());
      }

      // 통계(로그) 용 변수 세팅
      int chunkTotalDtoCount = dtos.size(); // S3에서 읽어온 DTO 개수
      int chunkInsertedArticleCount = 0;    // 실제 DB에 복구된 기사 개수 (중복 제외)
      int chunkInsertedMappingCount = 0;    // 실제 DB에 복구된 관심사 매핑 개수
      int chunkSkippedArticleCount = 0;     // 이미 DB에 있어서 무시된 기사 개수

      for (ArticleBackupDto dto : dtos) {

        // createdAt이 null이라면, 차선책으로 publishDate를 생성일로 간주
        LocalDateTime safeCreatedAt = dto.createdAt() != null ? dto.createdAt() : dto.publishDate();

        // 기사 엔티티 생성
        Article article = Article.restore(
            dto.id(), dto.source(), dto.sourceUrl(), dto.title(), dto.summary(), dto.publishDate(), safeCreatedAt
        );

        // 기사 본체 복구 (articles 테이블)
        int updatedRows = articleRepository.upsertArticleSkipDuplicate(article);

        // 기사 본체 복구 완료 후 통계 처리
        if (updatedRows > 0) {
          restoredIds.add(article.getId().toString());
          chunkInsertedArticleCount++; // 기사 복구 성공 +1
        } else {
          chunkSkippedArticleCount++; // 이미 존재하는 기사 스킵 +1
        }

        // 관심사 매핑 복구 (article_interests 테이블)
        // 기사 본체의 신규/중복 여부와 상관없이 매핑 복구는 무조건 시도
        if (dto.interestIds() != null && !dto.interestIds().isEmpty()) {
          for (UUID interestId : dto.interestIds()) {

            // DB에 관심사가 없으면 쿼리를 날리지 않고 스킵
            if (!aliveInterestIds.contains(interestId)) {
              log.info("[{}] 삭제된 관심사 무시 (쿼리 패스) - 기사ID: {}, 무시된 관심사ID: {}", targetDate, article.getId(), interestId);
              continue;
            }

            // 새로운 매핑이 정상적으로 DB에 INSERT 된 경우 1, 매핑이 실패하여 무시된 경우 0을 반환
            int inserted = articleInterestRepository.insertIgnoreMapping(UUID.randomUUID(),
                article.getId(), interestId);

            if (inserted > 0) {
              chunkInsertedMappingCount++; // 매핑 복구 성공 +1
            } else {
              // 관심사가 살아있지만 이미 동일한 매핑이 존재할 경우
              log.debug("[{}] 중복된 매핑 스킵 - 기사ID: {}, 관심사ID: {}", targetDate, article.getId(), interestId);
            }
          }
        }
      }
      jobContext.put("RESTORED_ARTICLE_IDS", restoredIds);

      log.info("========== [{}] 기사 복구 청크 요약 ==========", targetDate);
      log.info("읽어온 데이터: 총 {} 건", chunkTotalDtoCount);
      log.info("기사 복구 성공: {} 건 (articles 테이블)", chunkInsertedArticleCount);
      log.info("매핑 복구 성공: {} 건 (article_interests 테이블)", chunkInsertedMappingCount);
      log.info("중복 스킵(무시): {} 건", chunkSkippedArticleCount);
      log.info("=========================================");
    };
  }

  private String getTempFilePath(String targetDate) {
    String tmpDir = System.getProperty("java.io.tmpdir");
    return tmpDir + File.separator + "monew_restore_" + targetDate + ".json";
  }
}
