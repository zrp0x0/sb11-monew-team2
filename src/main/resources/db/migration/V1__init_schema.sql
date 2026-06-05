CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    nickname VARCHAR(20) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE interests (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    subscriber_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_interests_name UNIQUE (name)
);

CREATE TABLE articles (
    id UUID PRIMARY KEY,
    source VARCHAR(30) NOT NULL,
    source_url VARCHAR(2048) NOT NULL,
    title VARCHAR(500) NOT NULL,
    summary VARCHAR(2000) NOT NULL,
    published_at TIMESTAMP(6) NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    CONSTRAINT uk_articles_source_url UNIQUE (source_url)
);

CREATE TABLE interest_keyword (
    interest_id UUID NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    CONSTRAINT fk_interest_keyword_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id)
);

CREATE TABLE article_interests (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL,
    interest_id UUID NOT NULL,
    CONSTRAINT uk_article_interest UNIQUE (article_id, interest_id),
    CONSTRAINT fk_article_interests_article
        FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_article_interests_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id)
);

CREATE TABLE comments (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL,
    user_id UUID NOT NULL,
    content VARCHAR(500) NOT NULL,
    like_counts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    CONSTRAINT fk_comments_article
        FOREIGN KEY (article_id) REFERENCES articles (id),
    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_comments_deleted_at
    ON comments (deleted_at);

CREATE TABLE comment_likes (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT idx_comment_like_comment_user UNIQUE (comment_id, user_id),
    CONSTRAINT fk_comment_likes_comment
        FOREIGN KEY (comment_id) REFERENCES comments (id),
    CONSTRAINT fk_comment_likes_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    interest_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT idx_subscription_interest_user UNIQUE (interest_id, user_id),
    CONSTRAINT fk_subscriptions_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id),
    CONSTRAINT fk_subscriptions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE article_views (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    article_id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_article_views_user_article UNIQUE (user_id, article_id),
    CONSTRAINT fk_article_views_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_article_views_article
        FOREIGN KEY (article_id) REFERENCES articles (id)
);

CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    user_id UUID NOT NULL,
    content VARCHAR(255) NOT NULL,
    resource_type VARCHAR(255) NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
