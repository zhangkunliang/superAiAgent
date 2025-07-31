package com.zkl.zklaiagent.entity;

import lombok.Data;

/**
 * 登录用户信息
 */
@Data
public class LoginUser {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickName;
    
    /**
     * 用户类型
     */
    private String userType;
    
    /**
     * 是否超级管理员
     */
    private Boolean superAdmin;
    
    /**
     * 区域代码
     */
    private String areaCode;
    
    /**
     * 登录时间
     */
    private Long loginTime;
}