CREATE TABLE job_run_history (
    id UUID PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    ended_at TIMESTAMP(6),
    duration_ms BIGINT,
    target_date DATE,
    total_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    failed_count BIGINT NOT NULL DEFAULT 0,
    skipped_count BIGINT NOT NULL DEFAULT 0,
    message VARCHAR(1000),
    created_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_job_run_history_job_started_at
    ON job_run_history (job_name, started_at DESC);

CREATE TABLE news_collect_run_summary (
    id UUID PRIMARY KEY,
    job_run_history_id UUID NOT NULL,
    interest_count BIGINT NOT NULL DEFAULT 0,
    api_call_count BIGINT NOT NULL DEFAULT 0,
    candidate_count BIGINT NOT NULL DEFAULT 0,
    unique_candidate_count BIGINT NOT NULL DEFAULT 0,
    duplicate_count BIGINT NOT NULL DEFAULT 0,
    saved_count BIGINT NOT NULL DEFAULT 0,
    failed_count BIGINT NOT NULL DEFAULT 0,
    duration_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_news_collect_run_summary_job_run_history
        FOREIGN KEY (job_run_history_id) REFERENCES job_run_history (id)
);

CREATE INDEX idx_news_collect_run_summary_job_run_history
    ON news_collect_run_summary (job_run_history_id);
