package fun.yeelo.oauth.domain.midjourney;

import lombok.Data;

@Data
public class Pagination {
    private Integer current;
    private Integer pageSize;

}