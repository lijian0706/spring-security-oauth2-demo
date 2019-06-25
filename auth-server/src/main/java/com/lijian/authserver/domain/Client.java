package com.lijian.authserver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.util.StringUtils;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author: Lijian
 * @Date: 2019-06-25 14:34
 */

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Client implements ClientDetails {

    @Id
    @GeneratedValue
    private Long id;
    private String clientId;
    private String secret;
    private String scopes; // 以逗号分隔
    private String grantTypes; // 以逗号分隔
    private String redirectUris; // 以逗号分隔

    @Override
    public Set<String> getResourceIds() {
        return null;
    }

    @Override
    public boolean isSecretRequired() {
        return true;
    }

    @Override
    public String getClientSecret() {
        return getSecret();
    }

    @Override
    public boolean isScoped() {
        return true;
    }

    @Override
    public Set<String> getScope() {
        return stringToSet(getScopes());
    }

    @Override
    public Set<String> getAuthorizedGrantTypes() {
        return stringToSet(getGrantTypes());
    }

    @Override
    public Set<String> getRegisteredRedirectUri() {
        return stringToSet(getRedirectUris());
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public Integer getAccessTokenValiditySeconds() {
        return null;
    }

    @Override
    public Integer getRefreshTokenValiditySeconds() {
        return null;
    }

    @Override
    public boolean isAutoApprove(String scope) {
        return false;
    }

    @Override
    public Map<String, Object> getAdditionalInformation() {
        return null;
    }

    private Set<String> stringToSet(String items){
        Set<String> set = new HashSet<>();
        if(!StringUtils.isEmpty(items)){
            for (String item : items.split(",")) {
                set.add(item);
            }
        }
        return set;
    }
}
