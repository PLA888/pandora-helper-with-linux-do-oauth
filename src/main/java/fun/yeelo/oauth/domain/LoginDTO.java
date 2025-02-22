package fun.yeelo.oauth.domain;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;

    private String password;

    private String token;

    private String loginUrl;
}
