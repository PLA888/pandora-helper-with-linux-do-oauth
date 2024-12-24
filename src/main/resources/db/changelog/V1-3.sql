-- 把share_gpt_config的expires_at 字段更新为share表的expires_at字段，需要满足条件：share_gpt_config.share_id = share.id
UPDATE share_gpt_config SET expires_at = share.expires_at FROM share WHERE share_gpt_config.share_id = share.id and share.expires_at is not null;
UPDATE share_claude_config SET expires_at = share.expires_at FROM share WHERE share_claude_config.share_id = share.id and share.expires_at is not null;
UPDATE share_api_config SET expires_at = share.expires_at FROM share WHERE share_api_config.share_id = share.id and share.expires_at is not null;
UPDATE share set expires_at = null;