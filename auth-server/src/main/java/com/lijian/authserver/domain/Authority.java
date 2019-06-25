package com.lijian.authserver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Authority implements GrantedAuthority {

    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    @NotNull
    private String code;
    @NotNull
    private String description;

    public Authority(String code, String description){
        this.setCode(code);
        this.setDescription(description);
    }

    @Override
    public String getAuthority() {
        return this.getCode();
    }



    public static List<Authority> init(){
        Authority authority = new Authority("QUERY_USER_INFO", "查询用户信息");
        List<Authority> authorities = new ArrayList<>();
        authorities.add(authority);
        return authorities;
    }
}
