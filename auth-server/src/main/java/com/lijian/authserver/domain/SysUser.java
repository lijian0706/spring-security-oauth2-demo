package com.lijian.authserver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SysUser implements UserDetails {

    public static final String ADMIN_PASSWORD = "admin";
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    @NotNull
    private String username;
    @NotNull
    private String password;
    @ManyToMany(targetEntity = Role.class,fetch = FetchType.EAGER,cascade = CascadeType.PERSIST)
    private List<Role> roles = new ArrayList<>();

    public SysUser(String username, String password, List<Role> roles){
        this.setUsername(username);
        this.setPassword(password);
        this.setRoles(roles);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        this.roles.forEach(role -> authorities.addAll(role.getAuthorities()));
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static SysUser initAdminUser(String adminPassword){
        return new SysUser("admin", adminPassword, Role.initAdminRole());
    }
}
