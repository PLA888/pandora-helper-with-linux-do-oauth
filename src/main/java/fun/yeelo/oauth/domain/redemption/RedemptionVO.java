package fun.yeelo.oauth.domain.redemption;

import lombok.Data;

@Data
public class RedemptionVO extends Redemption{
    private String email;

    private String accountType;

    private Integer count;
}
