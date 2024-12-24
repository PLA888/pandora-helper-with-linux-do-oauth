package fun.yeelo.oauth.domain.share;

import lombok.Data;

@Data
public class ResetDTO {
    private String username;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
