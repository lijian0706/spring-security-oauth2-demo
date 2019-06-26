## 简介
- 本项目分为`auth-server`(`auth-server-demo`)和`auth-client`(`auth-clien-demo`)两个部分，两个项目都采用`Spring boot`搭建，为了演示方便，采用内存存储用户和token，登录与授权页面都引用自带的页面，虽然样式不美观，但不影响演示效果。
	- `auth-server-demo`引用了`spring-cloud-starter-oauth2`依赖，他是一个支持`oauth2`协议的`sso`服务器(既是`AuthorizationServer `，也是`ResourceServer `)，支持的授权方式`password`, `authorization_code`, `refresh_token`, `client_credentials`, `implicit`，关于`oauth2`的相关介绍请参考：`oauth2`的简单介绍。
	- `auth-client-demo`也引用了`spring-cloud-starter-oauth2`依赖，他是一个`sso`客户端，只需要修改`application.yml`中的配置就可以很方便的接入`sso`服务器，例如：github、google、facebook以及自己搭建的`auth-server`。



## Auth-client
### 添加`spring-cloud-starter-oauth2`依赖

```
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-oauth2</artifactId>
</dependency>
```

### 配置文件
```
auth-server:
  client:
    clientId: client
    clientSecret: client
    accessTokenUri: http://127.0.0.1:8080/auth/oauth/token
    userAuthorizationUri: http://127.0.0.1:8080/auth/oauth/authorize
    tokenName: access_token
    authenticationScheme: query
    clientAuthenticationScheme: form
  resource:
    userInfoUri: http://127.0.0.1:8080/auth/sysUsers/user

# 以下是github 服务器的配置
#auth-server:
#  client:
#    clientId: x
#    clientSecret: x
#    accessTokenUri: https://github.com/login/oauth/access_token
#    userAuthorizationUri: https://github.com/login/oauth/authorize
#    tokenName: access_token
#    authenticationScheme: query
#    clientAuthenticationScheme: form
#  resource:
#    userInfoUri: https://api.github.com/user
server:
  port: 8081
  servlet:
    session:
      cookie:
        name: OAUTH2SESSION
logging.level.org.springframework: debug
```
### 配置类
```
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
```

## Auth-server
### 依赖
```
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-oauth2</artifactId>
</dependency>
```
### 配置类
#### `AuthorizationConfig`

```
@Configuration
@EnableAuthorizationServer
public class AuthorizationConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.allowFormAuthenticationForClients(); // 允许客户端使用form提交，若没有这行代码，会使用basic方式提交，客户端账号、密码会放在headers中：Basic Y2xpZW50OmNsaWVudA==
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
            clients.inMemory()
                .withClient("client")
                .secret(passwordEncoder().encode("client"))
                .scopes("read")
                .authorizedGrantTypes("password", "authorization_code", "refresh_token", "client_credentials", "implicit")
                .redirectUris("http://localhost:8081/login/authServer");
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints.authenticationManager(authenticationManager); // 为了支持password授权类型，必须配置authenticationManager
    }

}
```

#### ResourceServerConfig
- ResourceServerConfig没有做任何事

```
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        super.configure(resources);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        super.configure(http);
    }
}
```

#### WebSecurityConfig
```
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(2) // 调整优先级，比ResourceServerConfig高
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("admin")
                .password(passwordEncoder.encode("admin"))
                .roles("admin");
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
    }

    /**
     * 只拦截/login，/oauth/authorize路径，其他交由oauth进行拦截
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatchers().antMatchers("/login", "/oauth/authorize").and()
                .formLogin().and()
                .csrf().disable();
    }

    /**
     * 为了支持password授权类型，必须配置authenticationManager
     * @return
     * @throws Exception
     */
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
```

#### 新增获取当前登录用户信息接口
```
@RequestMapping("/sysUsers")
@RestController
public class SysUserController {

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public Principal user(Principal user){
        return user;
    }
}
```

## 测试流程
- 浏览器打开`http://localhost:8081`，重定向到登录页，点击`使用authServer账号登录`，`OAuth2ClientAuthenticationProcessingFilter`会拦截此路径
- 重定向到`auth-server`请求授权码`code`
- `auth-server`发现用户未登录，跳转至登录页面
- 用户登录后询问是否授权
- 用户同意授权
- 带着授权码重定向回到`auth-client`
- `auth-client`携带授权码、客户端信息向`auth-server`请求换取token
- token换取成功后，`auth-client`向`auth-server`请求用户信息
- 登录成功

## 集成过程中遇到的错误
### 客户端报401或者`Access is denied`或者`possible csrf detected - state parameter was required but no state could be found`
- 报错原因可能有两个：
	- 调试手法不正确导致的(比如我)，遇到此报错重现的时候，请不要按浏览器的返回键，而是要重新走一遍流程，即：访问客户端 -> 跳转服务端登录 -> 授权 -> 返回客户端，不要图省事。
	- 本机同时在同一个域名下启动了auth-server和auth-client，导致服务端与客户端共享了同一个cookie，有两种解决方法，选择其一即可：
		- auth-server与auth-client处在不同域名下，例如，auth-server：127.0.0.1，auth-client:localhost
		- 将其中一个cookie名称修改掉，例如：
		
		```
		server:
		  port: 8081
		  servlet:
		    session:
		      cookie:
		        name: OAUTH2SESSION
		```
- 另外在遇到此报错后，经过一番调试，自以为发现了错误的根本(见如下代码)，其实是由于本人为了图省事，重现bug时只简单的按下浏览器返回键导致的。
`AuthorizationCodeAccessTokenProvider`:

```
if (request.getStateKey() != null || stateMandatory) {
			// The token endpoint has no use for the state so we don't send it back, but we are using it
			// for CSRF detection client side...
			if (preservedState == null) { // preservedState总是为null，实际是因为调试手法错误导致的
				throw new InvalidRequestException(
						"Possible CSRF detected - state parameter was required but no state could be found");
			}
		}
```

### 客户端在使用`code`换取`token`时，报401`Access is denied`
- 具体报错信息如下：

```
2019-04-26 09:03:24.149 DEBUG 10661 --- [nio-8081-exec-2] o.s.b.a.audit.listener.AuditListener     : AuditEvent [timestamp=2019-04-26T01:03:24.149Z, principal=anonymousUser, type=AUTHORIZATION_FAILURE, data={details=org.springframework.security.web.authentication.WebAuthenticationDetails@fffbcba8: RemoteIpAddress: 0:0:0:0:0:0:0:1; SessionId: A52D520C3D9C035279D0FA43C1477B1D, type=org.springframework.security.access.AccessDeniedException, message=Access is denied}]
2019-04-26 09:03:24.149 DEBUG 10661 --- [nio-8081-exec-2] o.s.s.w.a.ExceptionTranslationFilter     : Access is denied (user is anonymous); redirecting to authentication entry point
```
- 可能的原因：
	- auth-server默认采取basic Auth，即在Headers中存放Authentication信息(经过base64编码)，而auth-client采用form提交方式，即clientId和clientSecret都放在form中，因此会报401
	- 检查`auth-server`是不是没有添加注解：`@EnableResourceServer`
- 解决方法是：
	- auth-server配置类中允许form提交：`security.allowFormAuthenticationForClients()`
	- 添加`@EnableResourceServer`

###`auth-server`无法自动重定向到登录页面，浏览器端报：`Full authentication is required to access this resource`
- 具体报错信息服务端报`Access is denied`，浏览器端报：

```
<oauth>
	<error_description>
		Full authentication is required to access this resource
	</error_description>
	<error>unauthorized</error>
</oauth>
```
- 原因分析：看到oauth应该知道，被oauth相关的filter拦截掉了，原因是`ResourceServer`的配置类较`WebSecurity`配置类优先级高。
- 这里采取的解决办法是将`WebSecurity`配置类优先级往前排`@Order(2)`，另外只针对`/login`和`/oauth/authorize`两个路径进行拦截(意思是：若在未登录的情况下访问这两个路径，会被重定向到登录页面)，而其他则由`ResourceServer`进行拦截。

### `Handling OAuth2 error: error="invalid_grant", error_description="Invalid redirect: http://127.0.0.1:8081/login/authServer does not match one of the registered values."`
- 这是由于重定向的url和注册的不一致，可以进入`DefaultRedirectResolver.resolveRedirect()`中检查`registeredRedirectUris`和`requestedRedirect`，前者是注册的重定向url，后者是实际重定向的url，注意观察是否是`127.0.0.1`和`localhost`的区别导致的。

## 备注
- 授权、获取token可参考：`TokenEndpoint`、`AuthorizationEndpoint`、`CheckTokenEndpoint`
- 源码参考tag:0.0.1-SNAPSHOT
- 0.0.2-SNAPSHOT请参考文档，在此基础上进行了如下升级：
	- 用户、客户端使用数据库进行存储
	- 自定义用户授权页面

---
源码地址：[github](https://github.com/lijian0706/spring-security-oauth2-demo)
