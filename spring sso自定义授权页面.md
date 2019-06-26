## 简介
- 本次教程承接[Spring Boot + Spring Security Oauth 搭建SSO服务器和客户端教程](http://blog.lijian0706.cn/2019/06/03/spring-security-sso/)，源码tag为：`0.0.2-SNAPSHOT`，在之前的版本基础上进行了如下改进：
	- 用户、客户端使用数据库进行存储
	- 自定义用户授权页面

## 用户

- 用户的配置可参考[Spring Security 快速入门](http://blog.lijian0706.cn/2019/06/03/security-getting-started/)

## 授权客户端
- 客户端的配置与用户配置极为类似，代码如下。
### 实体类

```

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
        return new ArrayList<>();
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
```

### 配置类
- 新增 `ClientRepository`

```
public interface ClientRepository extends JpaRepository<Client, Long> {
    Client findByClientId(String clientId);
}
```

- 新增`JpaClientDetailsService`

```
public class JpaClientDetailsService implements ClientDetailsService {

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        return clientRepository.findByClientId(clientId);
    }
}
```

- `AuthorizationConfig`需要修改`configure(ClientDetailsServiceConfigurer clients)`方法的实现：

```
@Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(jpaClientDetailsService());
    }
    
    @Bean
    public JpaClientDetailsService jpaClientDetailsService(){
        return new JpaClientDetailsService();
    }
```

## 自定义授权页面
- 预制的授权页面较为丑陋，且是英文，无法满足实际需求，因此我们需要自定义授权页面，自定义较为简单，只需要定义`/oauth/confirm_access`接口，转向自定义页面即可。

### 引入Thymeleaf及js相关依赖
```
<dependency>
	<groupId>org.webjars</groupId>
	<artifactId>bootstrap</artifactId>
	<version>4.1.3</version>
</dependency>
<dependency>
	<groupId>org.webjars</groupId>
	<artifactId>jquery</artifactId>
	<version>3.3.1</version>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```
### 接口
```
@Controller
@RequestMapping("/oauth")
@SessionAttributes("authorizationRequest")
public class OauthController {

    /**
     * 自定义授权页面
     * @return
     */
    @RequestMapping("/confirm_access")
    public String oauthConfirmPage(Model model){
        return "approval.html";
    }
}

```

### 页面
- 在`resources/templates`文件夹下新增`approval.html`

```
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>授权页面</title>
    <script src="/auth/webjars/bootstrap/4.1.3/js/bootstrap.min.js"></script>
    <script src="/auth/webjars/jquery/3.3.1/jquery.min.js"></script>
    <link rel="stylesheet" type="text/css" href="/webjars/bootstrap/4.1.3/css/bootstrap.min.css">
</head>
<body>
    是否同意授权<b th:text="${authorizationRequest.clientId}"></b>?：
    <form action="/auth/oauth/authorize" method="post" id="form">
        <input type="hidden" name="user_oauth_approval" value="true"/>
        <input type="hidden" name="authorize" value="Authorize"/>
        <div id="div"></div>
        <input type="submit">
    </form>


</body>
<script th:inline="javascript">
    var authorizationRequest = [[${authorizationRequest}]];
    console.log(authorizationRequest);

    var div = document.getElementById('div');
    var scopes = authorizationRequest.scope;
    for (var i = 0; i < scopes.length; i++) {
        div.innerHTML += scopes[i] + ":" + "同意：<input type='radio' name='scope."+scopes[i]+"' value='true'>" + "拒绝：<input type='radio' name='scope."+scopes[i]+"' value='false' checked><br>";
    }
</script>
</html>
```

