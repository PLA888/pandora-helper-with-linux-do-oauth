package fun.yeelo.oauth.domain.midjourney;

import lombok.Data;

@Data
public class Search {
    private Integer current;
    private Integer pageSize;
    private Integer pageNumber;
    private String name;

}