package fun.yeelo.oauth.domain.car;

import lombok.Data;

import java.util.List;

@Data
public class CarApplyVO extends CarApply{
    Integer allowApply;

    List<Integer> ids;
}
