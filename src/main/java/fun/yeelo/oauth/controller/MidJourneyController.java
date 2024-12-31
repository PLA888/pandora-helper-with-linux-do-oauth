package fun.yeelo.oauth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.midjourney.*;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.service.MidjourneyService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/mj")
public class MidJourneyController {
    @Value("${midjourney.url}")
    private String mjUrl;

    @Value("${midjourney.key}")
    private String mjKey;

    @Autowired
    private ShareService shareService;


    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MidjourneyService midjourneyService;

    @Value("${midjourney.enable}")
    private Boolean mjEnable;

    @GetMapping("/users")
    public HttpResult<UserResponse> getUsers(HttpServletRequest request, @RequestParam(required = false) String username) {
        if (!mjEnable) {
            return HttpResult.error("未启用MJ");
        }
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String myName = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(myName);
        if (user == null){
            return HttpResult.error("无权访问数据");
        }
        HttpResult<UserResponse> users = midjourneyService.getUsers(username);
        UserResponse data = users.getData();
        data.setList(data.getList().stream().filter(e-> user.getId().equals(1) || e.getName().equals(user.getUniqueName())).collect(Collectors.toList()));
        return users;
    }

    @GetMapping("/tasks")
    public HttpResult<JSONObject> getTasks(HttpServletRequest request,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)){
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String myName = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(myName);
        if (user == null){
            return HttpResult.error("用户不存在");
        }
        Share admin = shareService.getById(1);
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json, text/plain, */*");
        headers.set("accept-language", "zh-CN");
        headers.set("content-type", "application/json");
        headers.set("mj-api-secret", StringUtils.hasText(mjKey) ? mjKey : admin.getId() + "+" + admin.getUniqueName() + "+" + admin.getPassword().substring(0, 10));
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0");

        // 创建请求体
        UserRequest requestBody = new UserRequest();

        // 设置分页信息
        Pagination pagination = new Pagination();
        pagination.setCurrent(page);
        pagination.setPageSize(size);
        requestBody.setPagination(pagination);

        // 设置排序信息
        Sort sort = new Sort();
        sort.setPredicate("");
        sort.setReverse(true);
        requestBody.setSort(sort);

        // 设置搜索信息
        Search search = new Search();
        search.setCurrent(page);
        search.setPageSize(size);
        search.setPageNumber(0);
        requestBody.setSearch(search);

        // 创建HTTP实体，包含头部和请求体
        HttpEntity<UserRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // 发送请求并返回响应
        ResponseEntity<String> exchange = new RestTemplate().postForEntity(
                mjUrl + "/mj/admin/tasks",
                requestEntity,
                String.class
        );
        try {
            JSONObject taskResponse = JSONObject.parseObject(exchange.getBody());
            JSONArray list = taskResponse.getJSONArray("list");
            String mjUserId = user.getMjUserId();
            list.forEach(node->{
                JSONObject nodeJson = (JSONObject)node;
                if (!StringUtils.hasText(mjUserId) || !nodeJson.getString("userId").equals(mjUserId)){ {
                    ((JSONObject) node).put("prompt","🔒");
                    ((JSONObject) node).put("promptEn","🔒");
                    ((JSONObject) node).put("promptFull","🔒");
                    ((JSONObject) node).put("thumbnailUrl","🔒");
                    ((JSONObject) node).put("imageUrl","🔒");
                    ((JSONObject) node).put("description","🔒");
                    ((JSONObject) node).put("nonce","🔒");
                    ((JSONObject) node).put("jobId","🔒");
                    ((JSONObject) node).put("instanceId","🔒");
                    ((JSONObject) node).put("clientIp","🔒");
                    ((JSONObject) node).put("userId","🔒");
                }}
            });
            return HttpResult.success(taskResponse);
        }catch (Exception e){
            log.error("获取用户列表失败", e);
            return HttpResult.error("获取用户列表失败");
        }
    }





}
