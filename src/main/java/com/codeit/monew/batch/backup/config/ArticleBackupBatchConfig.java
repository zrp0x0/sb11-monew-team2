package com.codeit.monew.batch.backup.config;

import com.amazonaws.services.s3.AmazonS3;
import com.codeit.monew.batch.backup.dto.ArticleBackupDto;
import com.codeit.monew.domain.article.entity.Article;
import com.codeit.monew.domain.article.repository.ArticleInterestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManagerFactory;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ArticleBackupBatchConfig {

  private final EntityManagerFactory entityManagerFactory; // JPA로 DB를 읽기 위함

  private final AmazonS3 amazonS3;

  private final ArticleInterestRepository articleInterestRepository;

  @Value("${batch.chunk-size:500}")
  private int chunkSize; // 한 번에 DB에서 읽고 처리할 데이터의 양

  @Value("${aws.s3.bucket}")
  private String bucketName;

  @Bean
  public Job articleBackupJob(JobRepository jobRepository,
      Step backupToLocalStep,
      Step uploadToS3Step) {
    return new JobBuilder("articleBackupJob", jobRepository)
        .start(backupToLocalStep)
        .next(uploadToS3Step)
        .build();
  }

  // DB -> 로컬 임시 파일 (Chunk 처리 - 1000건씩 끊어서 메서드 실행)
  @Bean
  public Step backupToLocalStep(JobRepository jobRepository, PlatformTransactionManager ptm) {
    return new StepBuilder("backupToLocalStep", jobRepository)
        .<Article, ArticleBackupDto>chunk(chunkSize, ptm)
        .reader(articleJpaReader(null))
        .processor(articleProcessor())
        .writer(articleLocalTempWriter(null))
        .build();
  }

  @Bean
  @StepScope
  public JpaPagingItemReader<Article> articleJpaReader(
      @Value("#{jobParameters['targetDate']}") String targetDate) {
    return new JpaPagingItemReaderBuilder<Article>()
        .name("articleJpaReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString(
            "SELECT a FROM Article a WHERE FUNCTION('DATE', a.publishedAt) = CAST(:targetDate AS date) ORDER BY a.id ASC")
        .parameterValues(Map.of("targetDate", targetDate))
        .pageSize(chunkSize)
        .build();
  }

  @Bean
  public ItemProcessor<Article, ArticleBackupDto> articleProcessor() {
    return article ->  {
      List<UUID> interestIds = articleInterestRepository.findInterestIdsByArticleId(article.getId());

      return new ArticleBackupDto(
          article.getId(),
          article.getSource(),
          article.getTitle(),
          article.getSummary(),
          article.getSourceUrl(),
          article.getPublishedAt(),
          interestIds
      );
    };
  }

  @Bean
  @StepScope
  public JsonFileItemWriter<ArticleBackupDto> articleLocalTempWriter(
      @Value("#{jobParameters['targetDate']}") String targetDate
  ) {
    String tempPath = getTempFilePath(targetDate);

    // LocalDateTime 필드를 JSON 문자열로 바꾸기 위한 날짜 변환 모듈
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    JacksonJsonObjectMarshaller<ArticleBackupDto> marshaller = new JacksonJsonObjectMarshaller<>();
    marshaller.setObjectMapper(objectMapper);

    return new JsonFileItemWriterBuilder<ArticleBackupDto>()
        .jsonObjectMarshaller(marshaller)
        .resource(new FileSystemResource(tempPath))
        .name("articleLocalTempWriter")
        .build();
  }

  // 로컬 임시 파일 -> AWS S3 업로드 (Tasklet 단일 처리)
  @Bean
  public Step uploadToS3Step(JobRepository jobRepository, PlatformTransactionManager ptm) {
    return new StepBuilder("uploadToS3Step", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          String targetDate = chunkContext.getStepContext().getStepExecution().getJobParameters().getString("targetDate");
          String tempPath = getTempFilePath(targetDate);
          File localFile = new File(tempPath);

          if (localFile.exists()) {
            String s3Key = "article/" + targetDate + ".json";
            amazonS3.putObject(bucketName, s3Key, localFile);
          }
          return RepeatStatus.FINISHED;
        }, ptm)
        .listener(new StepExecutionListener() {
          @Override
          public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
            String targetDate = stepExecution.getJobParameters().getString("targetDate");
            File localTempFile = new File(getTempFilePath(targetDate));
            if (localTempFile.exists()) {
              localTempFile.delete();
            }
            return StepExecutionListener.super.afterStep(stepExecution);
          }
        })
        .build();
  }

  private String getTempFilePath(String targetDate) {
    String tmpDir = System.getProperty("java.io.tmpdir");
    return tmpDir + File.separator + "monew_backup_" + targetDate + ".json";
  }
}
