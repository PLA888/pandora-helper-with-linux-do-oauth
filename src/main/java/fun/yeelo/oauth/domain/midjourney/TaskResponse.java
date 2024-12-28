package fun.yeelo.oauth.domain.midjourney;

import lombok.Data;

import java.util.List;

@Data
public class TaskResponse {
    private List<User> list;
    private Pagination pagination;

}