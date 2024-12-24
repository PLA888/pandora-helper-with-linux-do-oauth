package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.ApiConfigMapper;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.ShareApiConfig;
import org.springframework.stereotype.Service;

@Service
public class ApiConfigService extends ServiceImpl<ApiConfigMapper, ShareApiConfig> implements IService<ShareApiConfig> {

    public HttpResult<Boolean> addShare(Account account, int shareId, Integer expire) {
        // 删除原有的
        this.baseMapper.delete(new LambdaQueryWrapper<ShareApiConfig>().eq(ShareApiConfig::getShareId, shareId));

        ShareApiConfig shareClaudeConfig = new ShareApiConfig();
        shareClaudeConfig.setShareId(shareId);
        shareClaudeConfig.setAccountId(account.getId());
        save(shareClaudeConfig);
        return HttpResult.success();
    }
}
