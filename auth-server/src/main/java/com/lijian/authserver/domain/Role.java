package com.lijian.authserver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    @NotNull
    private String code;
    @NotNull
    private String name;
    @ManyToMany(targetEntity = Authority.class,fetch = FetchType.EAGER,cascade = CascadeType.PERSIST)
    private List<Authority> authorities = new ArrayList<>();

    public Role(String code, String name, List<Authority> authorities){
        this.setCode(code);
        this.setName(name);
        this.setAuthorities(authorities);
    }

    public static List<Role> initAdminRole(){
        Role adminRole = new Role("ADMIN", "管理员", Authority.init());
        List<Role> roles = new ArrayList<>();
        roles.add(adminRole);
        return roles;
    }
}
