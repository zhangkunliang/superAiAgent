package com.zkl.zklaiagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体
 */
@Data
public class LoginBody {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captcha;
    
    /**
     * 验证码key
     */
    @NotBlank(message = "验证码key不能为空")
    private String captchaKey;
}