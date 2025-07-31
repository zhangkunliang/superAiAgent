package com.zkl.zklaiagent.util;

import cn.dev33.satoken.stp.StpUtil;
import com.zkl.zklaiagent.entity.LoginUser;

/**
 * 登录帮助工具类
 */
public class LoginHelper {
    
    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {
        return (LoginUser) StpUtil.getSession().get("loginUser");
    }
    
    /**
     * 设置登录用户信息
     */
    public static void setLoginUser(LoginUser loginUser) {
        StpUtil.getSession().set("loginUser", loginUser);
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }
    
    /**
     * 是否为超级管理员
     */
    public static Boolean isSuperAdmin() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && loginUser.getSuperAdmin();
    }
}