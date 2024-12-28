package fun.yeelo.oauth.service;

import com.alibaba.fastjson.JSONObject;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.midjourney.*;
import fun.yeelo.oauth.domain.share.Share;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MidjourneyService {
    @Autowired
    private ShareService shareService;
    @Value("${midjourney.url}")
    private String mjUrl;

    public HttpResult<UserResponse> getUsers(String username) {
        Share admin = shareService.getById(1);
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json, text/plain, */*");
        headers.set("accept-language", "zh-CN");
        headers.set("content-type", "application/json");
        headers.set("mj-api-secret", admin.getId() + "+" + admin.getUniqueName() + "+" + admin.getPassword().substring(0, 10));
        headers.set("origin", "https://midjourney.yeelo.fun");
        headers.set("referer", "https://midjourney.yeelo.fun/");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0");

        // 创建请求体
        UserRequest requestBody = new UserRequest();

        // 设置分页信息
        Pagination pagination = new Pagination();
        pagination.setCurrent(1);
        pagination.setPageSize(999);
        requestBody.setPagination(pagination);

        // 设置排序信息
        Sort sort = new Sort();
        sort.setPredicate("");
        sort.setReverse(true);
        requestBody.setSort(sort);

        // 设置搜索信息
        Search search = new Search();
        if (StringUtils.hasText(username)){
            search.setName(username);
        }
        search.setCurrent(1);
        search.setPageSize(10);
        search.setPageNumber(0);
        requestBody.setSearch(search);

        // 创建HTTP实体，包含头部和请求体
        HttpEntity<UserRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // 发送请求并返回响应
        ResponseEntity<String> exchange = new RestTemplate().postForEntity(
                mjUrl + "/mj/admin/users",
                requestEntity,
                String.class
        );
        try {
            List<Share> allUser = shareService.list();
            UserResponse userResponse = JSONObject.parseObject(exchange.getBody(), UserResponse.class);
            Map<String, User> mjUserMap = userResponse.getList().stream().collect(Collectors.toMap(User::getName, Function.identity()));

            allUser.stream().filter(e->e.getMjUserId()==null && mjUserMap.containsKey(e.getUniqueName())).forEach(e->{
                Share share = new Share();
                share.setId(e.getId());
                share.setMjUserId(mjUserMap.get(e.getUniqueName()).getId());
                shareService.updateById(share);
            });
            return HttpResult.success(userResponse);
        }catch (Exception e){
            log.error("获取用户列表失败", e);
            return HttpResult.error("获取用户列表失败");
        }
    }
}
