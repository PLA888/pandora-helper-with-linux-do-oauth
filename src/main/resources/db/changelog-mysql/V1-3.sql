-- 把share_gpt_config的expires_at 字段更新为share表的expires_at字段，需要满足条件：share_gpt_config.share_id = share.id
UPDATE share_gpt_config 
INNER JOIN share ON share_gpt_config.share_id = share.id 
SET share_gpt_config.expires_at = share.expires_at 
WHERE share.expires_at is not null;

UPDATE share_claude_config 
INNER JOIN share ON share_claude_config.share_id = share.id 
SET share_claude_config.expires_at = share.expires_at 
WHERE share.expires_at is not null;

UPDATE share_api_config 
INNER JOIN share ON share_api_config.share_id = share.id 
SET share_api_config.expires_at = share.expires_at 
WHERE share.expires_at is not null;

UPDATE share set expires_at = null;