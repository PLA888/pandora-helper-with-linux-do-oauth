package fun.yeelo.oauth.domain.share;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@TableName("share_gpt_config")
@Data
public class ShareGptConfig {
    @TableId(type= IdType.AUTO)
    private Integer id;

    @TableField("share_id")
    private Integer shareId;

    @TableField("account_id")
    private Integer accountId;

    @TableField("share_token")
    private String shareToken;

    @TableField("expires_in")
    private Integer expiresIn;

    @TableField("expires_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    @TableField("site_limit")
    private String siteLimit;

    @TableField("gpt4_limit")
    private Integer gpt4Limit;

    @TableField("gpt35_limit")
    private Integer gpt35Limit;

    @TableField("show_userinfo")
    private Boolean showUserinfo;

    @TableField("show_conversations")
    private Boolean showConversations;

    @TableField("refresh_everyday")
    private Boolean refreshEveryday;

    @TableField("temporary_chat")
    private Boolean temporaryChat;

}
