package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class UsageVO {
    private Integer gpt_4o = 0;

    private Integer gpt_4 = 0;

    private Integer gpt_4o_mini = 0;

    private Integer o1 = 0;

    private Integer o1_mini = 0;
}
