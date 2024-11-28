package fun.yeelo.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.domain.Share;
import fun.yeelo.oauth.domain.ShareGptConfig;
import fun.yeelo.oauth.domain.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class MirrorConfig {
    @Value("${mirror.host}")
    private String mirrorHost;

    @Autowired
    private AccountService accountService;

    @Autowired
    private RestTemplate restTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    public HttpResult<ShareVO> getMirrorUrl(String username, Integer accountId) {
        ShareVO res = new ShareVO();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");
        ObjectNode personJsonObject = objectMapper.createObjectNode();
        personJsonObject.put("user_name", username.length() < 4 ? username+"####" : username);
        personJsonObject.put("isolated_session", true);
        personJsonObject.put("access_token", accountService.getById(accountId).getAccessToken());

        ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(mirrorHost + "/api/login", new HttpEntity<>(personJsonObject, headers), String.class);
        try {
            Map map = objectMapper.readValue(stringResponseEntity.getBody(), Map.class);
            if (map.containsKey("user-gateway-token")) {
                String gatewayToken = map.get("user-gateway-token").toString();
                res.setAddress(mirrorHost + "/api/not-login?user_gateway_token=" + gatewayToken);
                return HttpResult.success(res);
            } else {
                return HttpResult.error("获取Gateway Token 异常");
            }
        } catch (IOException e) {
            log.error("Check user error:", e);
            return HttpResult.error("系统内部异常");
        }
    }
}
