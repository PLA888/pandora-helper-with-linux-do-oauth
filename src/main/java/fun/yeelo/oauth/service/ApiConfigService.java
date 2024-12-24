package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.ApiConfigMapper;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.ShareApiConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ApiConfigService extends ServiceImpl<ApiConfigMapper, ShareApiConfig> implements IService<ShareApiConfig> {

    public HttpResult<Boolean> addShare(Account account, int shareId, String expireAt) {
        // 删除原有的
        this.baseMapper.delete(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getShareId, shareId));

        ShareApiConfig shareApiConfig = new ShareApiConfig();
        shareApiConfig.setShareId(shareId);
        shareApiConfig.setAccountId(account.getId());
        // 根据expireAt设置过期时间，格式是精确到日 的
        if (StringUtils.hasText(expireAt)) {
            shareApiConfig.setExpiresAt(LocalDateTime.parse(expireAt + " 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        save(shareApiConfig);
        return HttpResult.success();
    }
}
