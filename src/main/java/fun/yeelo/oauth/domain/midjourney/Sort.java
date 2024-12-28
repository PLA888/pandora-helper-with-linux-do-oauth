package fun.yeelo.oauth.domain.midjourney;

import lombok.Data;

@Data
public class Sort {
    private String predicate;
    private Boolean reverse;

}