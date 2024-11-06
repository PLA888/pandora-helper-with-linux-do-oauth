package fun.yeelo.oauth.timer;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.config.CommonConst;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UpdateTimer {
    @Autowired
    private AccountService accountService;

    @Autowired
    private ShareService shareService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GptConfigService gptConfigService;

    @Autowired
    private ClaudeConfigService claudeConfigService;

    @Autowired
    private EmailService emailService;

    @Value("${smtp.admin-email}")
    private String adminEmail;

    private static final String REFRESH_URL = "https://token.oaifree.com/api/auth/refresh";


    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    @ConditionalOnProperty(name = "smtp.enable", havingValue = "true")
    public void init() {
        log.info("启动预检");
        sendAccountExpiringEmail();
        sendShareExpiringEmail();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void updateExpire() {
        List<Share> shares = shareService.list().stream().filter(e -> StringUtils.hasText(e.getExpiresAt()) && !e.getExpiresAt().equals("-")).collect(Collectors.toList());
        shares.forEach(share -> {
            try {
                LocalDate expireData = LocalDate.parse(share.getExpiresAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                // 大于等于过期时间的，删除share config
                if (expireData.isEqual(LocalDate.now()) || !expireData.isAfter(LocalDate.now())) {
                    gptConfigService.remove(new LambdaQueryWrapper<ShareGptConfig>().eq(ShareGptConfig::getShareId, share.getId()));
                    claudeConfigService.remove(new LambdaQueryWrapper<ShareClaudeConfig>().eq(ShareClaudeConfig::getShareId, share.getId()));
                }
            } catch (Exception ex) {
                log.error("expire detect error,unique_name:{}", share.getUniqueName(), ex);
            }
        });
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void updateRefreshToken() {
        log.info("开始刷新access_token");
        List<Account> accounts = accountService.list().stream()
                                         .filter(e -> StringUtils.hasText(e.getRefreshToken()) && e.getAccountType().equals(1))
                                         .collect(Collectors.toList());
        accounts.forEach(account -> {
            try {
                Integer accountId = account.getId();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
                personJsonObject.add("refresh_token", account.getRefreshToken());
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(REFRESH_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
                Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                if (map.containsKey("access_token")) {
                    log.info("refresh success");
                    String newToken = map.get("access_token").toString();
                    Account updateDTO = new Account();
                    updateDTO.setId(accountId);
                    updateDTO.setAccessToken(newToken);
                    accountService.saveOrUpdate(updateDTO);
                }
            } catch (Exception e) {
                log.error("刷新access_token异常,异常账号:{}", account.getEmail(), e);
            }
        });
        log.info("刷新access_token结束");
    }


    @Scheduled(cron = "0 0 3 */2 * ?")
    public void updateShareToken() {
        log.info("开始刷新share_token");
        List<Share> shares = shareService.list();

        Map<Integer, Account> accountIdMap = accountService.list()
                                                     .stream()
                                                     .collect(Collectors.toMap(Account::getId, Function.identity()));

        Map<Integer, ShareGptConfig> gptConfigMap = gptConfigService.list().stream().collect(Collectors.toMap(ShareGptConfig::getShareId, Function.identity()));

        for (Share share : shares) {
            ShareGptConfig gptConfig = gptConfigMap.get(share.getId());
            if (gptConfig == null) {
                continue;
            }
            try {
                Share update = new Share();
                update.setId(share.getId());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, Object> personJsonObject = new LinkedMultiValueMap<>();
                personJsonObject.add("access_token", accountIdMap.get(gptConfig.getAccountId()).getAccessToken());
                personJsonObject.add("unique_name", share.getUniqueName());
                personJsonObject.add("expires_in", 0);
                personJsonObject.add("gpt35_limit", -1);
                personJsonObject.add("gpt4_limit", -1);
                personJsonObject.add("site_limit", "");
                personJsonObject.add("show_userinfo", false);
                personJsonObject.add("show_conversations", false);
                personJsonObject.add("reset_limit", true);
                personJsonObject.add("temporary_chat", false);
                ResponseEntity<String> stringResponseEntity = restTemplate.exchange(CommonConst.SHARE_TOKEN_URL, HttpMethod.POST, new HttpEntity<>(personJsonObject, headers), String.class);
                Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
                gptConfig.setShareToken(map.get("token_key").toString());
                gptConfigService.updateById(gptConfig);
            } catch (Exception e) {
                log.error("update share token error,unique_name:{}", share.getUniqueName(), e);
            }
        }
        log.info("刷新share_token结束");
    }

    @ConditionalOnProperty(name = "smtp.enable", havingValue = "true")
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendShareExpiringEmail() {
        log.info("开始发送订阅过期通知");
        List<Share> shares = shareService.list().stream().filter(e -> StringUtils.hasText(e.getExpiresAt()) && !e.getExpiresAt().equals("-")).collect(Collectors.toList());
        for (Share share : shares) {
            try {
                LocalDate expireData = LocalDate.parse(share.getExpiresAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                if (expireData.isEqual(LocalDate.now().plusDays(1))) {
                    emailService.sendSimpleEmail(adminEmail, "用户订阅即将过期", "订阅即将过期,用户名:" + share.getUniqueName());
                }
            } catch (Exception ex) {
                log.error("send share expiring email error,unique_name:{}", share.getUniqueName(), ex);
            }
        }
        log.info("发送订阅过期通知结束");
    }

    @ConditionalOnProperty(name = "smtp.enable", havingValue = "true")
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendAccountExpiringEmail() {
        log.info("开始发送ChatGPT账号过期通知");
        List<Account> accounts = accountService.list().stream().filter(e -> e.getAccountType().equals(1) && StringUtils.hasText(e.getAccessToken())).collect(Collectors.toList());
        for (Account account : accounts) {
            try {
                LocalDateTime localDateTime = checkAccount(account.getAccessToken());
                if (localDateTime.toLocalDate().isEqual(LocalDate.now().plusDays(3))) {
                    emailService.sendSimpleEmail(adminEmail, "ChatGPT账号过期预警", "ChatGPT即将在3天后到期,账号(邮箱):" + account.getEmail());
                }
            } catch (Exception ex) {
                log.error("获取chatgpt账号过期时间异常,账号:{}", account.getEmail(), ex);
            }
        }
        log.info("发送ChatGPT账号过期通知结束");
    }

    public LocalDateTime checkAccount(String accessToken) {
        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        // 设置基础请求头
        headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8");
        headers.set(HttpHeaders.CACHE_CONTROL, "max-age=0");
        headers.set("dnt", "1");
        headers.set("priority", "u=0, i");

        // 设置 UA 相关信息
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36");
        headers.set("sec-ch-ua", "\"Not)A;Brand\";v=\"99\", \"Google Chrome\";v=\"127\", \"Chromium\";v=\"127\"");
        headers.set("sec-ch-ua-arch", "\"arm\"");
        headers.set("sec-ch-ua-bitness", "\"64\"");
        headers.set("sec-ch-ua-full-version", "\"127.0.6533.120\"");
        headers.set("sec-ch-ua-full-version-list", "\"Not)A;Brand\";v=\"99.0.0.0\", \"Google Chrome\";v=\"127.0.6533.120\", \"Chromium\";v=\"127.0.6533.120\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-model", "\"\"");
        headers.set("sec-ch-ua-platform", "\"macOS\"");
        headers.set("sec-ch-ua-platform-version", "\"15.1.0\"");

        // 设置 Fetch 相关信息
        headers.set("sec-fetch-dest", "document");
        headers.set("sec-fetch-mode", "navigate");
        headers.set("sec-fetch-site", "none");
        headers.set("sec-fetch-user", "?1");
        headers.set("upgrade-insecure-requests", "1");

        // 设置 Cookie
        headers.set(HttpHeaders.COOKIE,
                "oai-hlib=true; " +
                        "oai-did=aa75acc8-54bb-4f83-bae7-c7a1b7eb66c7; " +
                        "intercom-device-id-dgkjq2bp=bab4e9e8-ba51-4570-9992-259e438a19a7; " +
                        "oai-nav-state=1; " +
                        // ... 其他 cookie 值
                        "_dd_s=rum=0&expire=1730870402848"
        );

        // 创建请求实体
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://new.oaifree.com/backend-api/accounts/check/v4-2023-04-27?timezone_offset_min=-480",
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            //if (response.getStatusCode().is2xxSuccessful()) {
            //    return response.getBody().toString();
            //} else {
            //    throw new RuntimeException("账户请求失败: " + response.getStatusCodeValue());
            //}
            return LocalDateTime.parse(JSON.parseObject(response.getBody()).getJSONObject("accounts").getJSONObject("default").getJSONObject("entitlement").getString("expires_at"),DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (RestClientException e) {
            throw new RuntimeException("账户请求失败: " + e.getMessage());
        }
    }

}
