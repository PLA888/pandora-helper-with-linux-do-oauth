package fun.yeelo.oauth.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.yeelo.oauth.domain.share.ShareGptConfig;
import org.springframework.stereotype.Repository;

@Repository
public interface GptConfigMapper extends BaseMapper<ShareGptConfig> {
}
