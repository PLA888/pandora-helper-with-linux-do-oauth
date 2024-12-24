package fun.yeelo.oauth.timer;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareApiConfig;
import fun.yeelo.oauth.domain.share.ShareClaudeConfig;
import fun.yeelo.oauth.domain.share.ShareGptConfig;
import fun.yeelo.oauth.service.*;
import fun.yeelo.oauth.utils.EncryptDecryptUtil;
import fun.yeelo.oauth.utils.OpenAIUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UpdateTimer {
    @Autowired
    private OpenAIUtil openAIUtil;

    @Value("${linux-do.oauth2.client.registration.redirect-uri}")
    private String apiUrl;

    @Value("${spring.mail.enable}")
    private Boolean mailEnable;

    @Autowired
    private AccountService accountService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private GptConfigService gptConfigService;

    @Autowired
    private ClaudeConfigService claudeConfigService;

    @Autowired
    private EmailService emailService;

    @Value("${spring.mail.username}")
    private String adminEmail;

    @Autowired
    private ApiConfigService apiConfigService;

    @PostConstruct
    @ConditionalOnProperty(name = "smtp.enable", havingValue = "true")
    public void init() {
        log.info("启动预检");
        CompletableFuture.runAsync(() -> {
            sendAccountExpiringEmail();
            sendShareExpiringEmail();
        });
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void updateExpire() {
        List<Share> shares = shareService.list().stream().filter(e -> StringUtils.hasText(e.getExpiresAt()) && !e.getExpiresAt().equals("-")).collect(Collectors.toList());
        List<ShareGptConfig> gptConfigs = gptConfigService.list().stream().filter(e -> Objects.nonNull(e.getExpiresAt())).collect(Collectors.toList());
        List<ShareClaudeConfig> claudeConfigs = claudeConfigService.list().stream().filter(e -> Objects.nonNull(e.getExpiresAt())).collect(Collectors.toList());
        List<ShareApiConfig> apiConfigs = apiConfigService.list().stream().filter(e -> Objects.nonNull(e.getExpiresAt())).collect(Collectors.toList());
        gptConfigs.forEach(share -> {
            try {
                LocalDate expireData = share.getExpiresAt().toLocalDate();
                // 大于等于过期时间的，删除share config
                if (expireData.isEqual(LocalDate.now()) || !expireData.isAfter(LocalDate.now())) {
                    gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, share.getId()));
                }
            } catch (Exception ex) {
                log.error("ChatGPT expire detect error,unique_name:{}", shareService.getById(share.getShareId()).getUniqueName(), ex);
            }
        });
        apiConfigs.forEach(share -> {
            try {
                LocalDate expireData = share.getExpiresAt().toLocalDate();
                // 大于等于过期时间的，删除share config
                if (expireData.isEqual(LocalDate.now()) || !expireData.isAfter(LocalDate.now())) {
                    apiConfigService.remove(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getShareId, share.getId()));
                }
            } catch (Exception ex) {
                log.error("API expire detect error,unique_name:{}", shareService.getById(share.getShareId()).getUniqueName(), ex);
            }
        });
        claudeConfigs.forEach(share -> {
            try {
                LocalDate expireData = share.getExpiresAt().toLocalDate();
                // 大于等于过期时间的，删除share config
                if (expireData.isEqual(LocalDate.now()) || !expireData.isAfter(LocalDate.now())) {
                    claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId, share.getId()));
                }
            } catch (Exception ex) {
                log.error("Claude expire detect error,unique_name:{}", shareService.getById(share.getShareId()).getUniqueName(), ex);
            }
        });
    }

    @Scheduled(cron = "${task.refresh:0 0 2 * * ?}")
    public void refreshAccessToken() {
        log.info("开始刷新access_token");
        List<Account> accounts = accountService.list().stream()
                                         .filter(e -> StringUtils.hasText(e.getRefreshToken()) && e.getAccountType().equals(1))
                                         .collect(Collectors.toList());
        accounts.forEach(account -> openAIUtil.refresh(account.getId(), account.getRefreshToken(), account.getEmail()));

        if (mailEnable) {
            accounts.forEach(account -> {
                LocalDateTime updateTime = account.getUpdateTime();
                if (updateTime != null && updateTime.toLocalDate().plusDays(9).isEqual(LocalDateTime.now().toLocalDate())) {
                    emailService.sendSimpleEmail(adminEmail, "ACCESS_TOKEN过期提醒", "ACCESS_TOKEN即将过期,账号（邮箱）:" + account.getEmail());
                }
            });
        }
        log.info("刷新access_token结束");
    }


    @Scheduled(cron = "0 0 8 * * ?")
    public void sendShareExpiringEmail() {
        if (!mailEnable) {
            return;
        }
        log.info("开始处理订阅过期通知");
        List<ShareGptConfig> gptConfigs = gptConfigService.list().stream().filter(e -> Objects.nonNull(e.getExpiresAt())).collect(Collectors.toList());
        List<ShareClaudeConfig> claudeConfigs = claudeConfigService.list().stream().filter(e -> Objects.nonNull(e.getExpiresAt())).collect(Collectors.toList());
        List<ShareApiConfig> apiConfigs = apiConfigService.list().stream().filter(e -> Objects.nonNull(e.getExpiresAt())).collect(Collectors.toList());
        for (ShareGptConfig share : gptConfigs) {
            Share user = shareService.getById(share.getShareId());
            processExpireUser(user, share.getExpiresAt());
        }
        for (ShareClaudeConfig share : claudeConfigs) {
            Share user = shareService.getById(share.getShareId());
            processExpireUser(user, share.getExpiresAt());
        }
        for (ShareApiConfig share : apiConfigs) {
            Share user = shareService.getById(share.getShareId());
            processExpireUser(user, share.getExpiresAt());
        }
        log.info("处理订阅过期通知结束");
    }

    public void processExpireUser(Share user, LocalDateTime expireData) {
        try {
            if (expireData.toLocalDate().isEqual(LocalDate.now().plusDays(1))) {
                String password = user.getPassword();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("username", user.getUniqueName());
                jsonObject.put("date", expireData.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                String encryptedCode = EncryptDecryptUtil.encrypt(jsonObject.toJSONString(), password.substring(0, 16));
                emailService.sendSimpleEmail(adminEmail, "用户订阅即将过期", "订阅即将过期,用户名:" + user.getUniqueName() + "。使用以下链接完成自动续费：" + apiUrl.replace("/loading", "") + "/share/autoRenewal?uniqueName=" + user.getUniqueName() + "&code=" + URLEncoder.encode(encryptedCode, "UTF-8"));
            }
        } catch (Exception ex) {
            log.error("send share expiring email error,unique_name:{}", user.getUniqueName(), ex);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendAccountExpiringEmail() {

        log.info("开始处理ChatGPT账号过期通知");
        List<Account> accounts = accountService.list().stream().filter(e -> e.getAccountType().equals(1) && StringUtils.hasText(e.getAccessToken())).collect(Collectors.toList());
        for (Account account : accounts) {
            try {
                LocalDateTime expireTime = openAIUtil.checkAccount(account.getAccessToken(), account.getEmail(), account.getId());
                if (expireTime != null) {
                    log.info("账号{}过期时间:{}", account.getEmail(), expireTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                if (mailEnable
                            && expireTime != null
                            && expireTime.isAfter(LocalDateTime.now())
                            && Duration.between(LocalDateTime.now(), expireTime).toDays() < 3) {
                    emailService.sendSimpleEmail(account.getEmail(), "ChatGPT订阅过期预警", "您的ChatGPT订阅即将到期，到期时间为：" + expireTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "，账号(邮箱):" + account.getEmail() + "，请注意及时续费。");
                }
                if (mailEnable
                            && expireTime != null
                            && expireTime.isBefore(LocalDateTime.now())
                            && Duration.between(expireTime, LocalDateTime.now()).toDays() < 3) {
                    emailService.sendSimpleEmail(account.getEmail(), "ChatGPT订阅过期提醒", "您的ChatGPT订阅已经到期，到期时间为：" + expireTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "，账号(邮箱):" + account.getEmail() + "，请注意及时续费。");
                }
            } catch (Exception ex) {
                log.error("获取chatgpt账号过期时间异常,账号:{}", account.getEmail(), ex);
            }
        }
        log.info("处理ChatGPT账号过期通知结束");
    }

}
