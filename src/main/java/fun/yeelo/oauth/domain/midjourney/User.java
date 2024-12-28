package fun.yeelo.oauth.domain.midjourney;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class User {
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private String role;
    private String status;
    private String token;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private String lastLoginTimeFormat;
    private String registerIp;
    private LocalDateTime registerTime;
    private String registerTimeFormat;
    private Integer dayDrawLimit;
    private Boolean isWhite;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String id;
    private Map<String, Object> properties;
}