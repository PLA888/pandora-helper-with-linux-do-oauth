package fun.yeelo.oauth.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@Slf4j
public class OpenAIUtil {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AccountService accountService;

    @Value("${spring.mail.enable}")
    private Boolean mailEnable;

    @Value("${spring.mail.username}")
    private String adminEmail;

    @Autowired
    private EmailService emailService;


    public LocalDateTime checkAccount(String accessToken, String email, Integer id) {
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

        // 创建请求实体
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);

        try {
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://token.yeelo.fun/backend-api/accounts/check/v4-2023-04-27?timezone_offset_min=-480",
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            try {
                if (!JSON.parseObject(response.getBody()).containsKey("accounts")) {
                    log.error("access_token已过期，账号：{}", email);
                    log.error(JSON.parseObject(response.getBody()).toJSONString());
                    return null;
                }
            } catch (Exception ex) {
                log.error("JSON解析失败，账号：{}, 返回内容{}", email, response.getBody());
                return null;
            }

            String planType = JSON.parseObject(response.getBody()).getJSONObject("accounts").getJSONObject("default").getJSONObject("account").getString("plan_type");
            Account account = new Account();
            account.setId(id);
            account.setPlanType(planType);
            accountService.updateById(account);
            return LocalDateTime.parse(JSON.parseObject(response.getBody()).getJSONObject("accounts").getJSONObject("default").getJSONObject("entitlement").getString("expires_at"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        } catch (Exception e) {
            log.error("获取账号信息异常，账号：{}", email);
            return null;
        }
    }

    public void refresh(Integer accountId, String refreshToken, String accountEmail) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.ACCEPT, "*/*");
            headers.set(HttpHeaders.USER_AGENT, "PostmanRuntime/7.43.0");
            headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
            headers.set(HttpHeaders.CONNECTION, "keep-alive");
            headers.set(HttpHeaders.HOST, "auth0.openai.com");
            JSONObject body = new JSONObject();
            body.put("refresh_token", refreshToken);
            body.put("redirect_uri", "com.openai.chat://auth0.openai.com/ios/com.openai.chat/callback");
            body.put("grant_type", "refresh_token");
            body.put("client_id", "pdlLIX2Y72MIl2rhLhTE9VV9bN905kBh");
            headers.set("Accept-Charset", "UTF-8"); // 声明接受的字符集
            headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.toJSONString().length()));
            ResponseEntity<String> stringResponseEntity = restTemplate.exchange("https://auth0.openai.com/oauth/token", HttpMethod.POST, new HttpEntity<>(body.toJSONString(), headers), String.class);
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("access_token")) {
                log.info("refresh success");
                String newToken = map.get("access_token").toString();
                Account updateDTO = new Account();
                updateDTO.setId(accountId);
                updateDTO.setAccessToken(newToken);
                updateDTO.setUpdateTime(LocalDateTime.now());
                accountService.saveOrUpdate(updateDTO);
                log.info("刷新账号{}成功", accountEmail);
            }
        } catch (Exception e) {
            log.error("刷新access_token异常,异常账号:{}", accountEmail, e);
            if (mailEnable) {
                emailService.sendSimpleEmail(adminEmail, "刷新access_token异常", "刷新access_token异常,异常账号:" + accountEmail + ", 请检查对应 refresh_token 是否有效");
            }
        }
    }
}
