package com.lijian.authserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Author: Lijian
 * @Date: 2019-06-25 15:18
 */
@Controller
@RequestMapping("/oauth")
public class OauthController {

    /**
     * 自定义授权页面
     * @param model
     * @param request
     * @return
     */
//    @RequestMapping("/confirm_access")
//    public ModelAndView oauthConfirmPage(Map<String, Object> model, HttpServletRequest request){
//        View approvalView = new View() {
//            @Override
//            public String getContentType() {
//                return "text/html";
//            }
//
//            @Override
//            public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
//                response.setContentType(getContentType());
//                response.getWriter().append("<button>agree</button>&nbsp;&nbsp;<button>deny</button>");
//            }
//        };
//        return new ModelAndView(approvalView);
//    }
}
