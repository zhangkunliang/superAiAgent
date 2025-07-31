package com.zkl.zklaiagent.dto;

import lombok.Data;

/**
 * 登录响应VO
 */
@Data
public class LoginVO {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
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
     * 过期时间
     */
    private Long expireTime;
}