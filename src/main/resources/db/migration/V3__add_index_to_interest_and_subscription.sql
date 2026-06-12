CREATE INDEX IF NOT EXISTS idx_article_interests_interest_id
  ON article_interests (interest_id);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id
  ON subscriptions (user_id);

