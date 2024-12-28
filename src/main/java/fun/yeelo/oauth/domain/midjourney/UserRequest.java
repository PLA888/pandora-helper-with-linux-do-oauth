package fun.yeelo.oauth.domain.midjourney;

import fun.yeelo.oauth.domain.midjourney.Search;
import lombok.Data;

@Data
public class UserRequest {
    private Pagination pagination;
    private Sort sort;
    private Search search;

}