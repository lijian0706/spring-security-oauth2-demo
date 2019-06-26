package com.lijian.authserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * @Author: Lijian
 * @Date: 2019-06-25 15:18
 */
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
