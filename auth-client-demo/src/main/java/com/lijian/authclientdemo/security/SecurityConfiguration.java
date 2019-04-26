package com.lijian.authclientdemo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.Filter;

@Configuration
@EnableOAuth2Client
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private OAuth2ClientContext oauth2ClientContext;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/login.html");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/**")
                .authorizeRequests().anyRequest().authenticated().and()
                .formLogin().loginPage("/login.html").successForwardUrl("/index.html").and()
                .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
    }

    /**
     * 若需要集成多个服务器，可以配置多个filter，并加入到流程中，参考：.addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
     * @return
     */
    private Filter ssoFilter() {
        OAuth2ClientAuthenticationProcessingFilter authServerFilter = new OAuth2ClientAuthenticationProcessingFilter("/login/authServer");
        OAuth2RestTemplate authServerTemplate = new OAuth2RestTemplate(authClient(), oauth2ClientContext);
        AuthorizationCodeAccessTokenProvider authCodeProvider = new AuthorizationCodeAccessTokenProvider();
        authServerTemplate.setAccessTokenProvider(authCodeProvider);
        authServerFilter.setRestTemplate(authServerTemplate);

        UserInfoTokenServices tokenServices = new UserInfoTokenServices(authResource().getUserInfoUri(), authClient().getClientId());
        tokenServices.setRestTemplate(authServerTemplate);
        authServerFilter.setTokenServices(tokenServices);
        return authServerFilter;
    }

    @Bean
    @ConfigurationProperties("auth-server.client")
    public AuthorizationCodeResourceDetails authClient() {
        return new AuthorizationCodeResourceDetails();
    }

    @Bean
    @ConfigurationProperties("auth-server.resource")
    @Primary
    public ResourceServerProperties authResource() {
        return new ResourceServerProperties();
    }

    @Bean
    public FilterRegistrationBean<OAuth2ClientContextFilter> oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
        FilterRegistrationBean<OAuth2ClientContextFilter> registration = new FilterRegistrationBean<OAuth2ClientContextFilter>();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }
}
