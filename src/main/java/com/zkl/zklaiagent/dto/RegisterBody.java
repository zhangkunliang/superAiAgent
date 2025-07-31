package com.zkl.zklaiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求体
 */
@Data
public class RegisterBody {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,16}$", message = "用户名必须是4-16位字母数字下划线")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$", 
             message = "密码必须包含大小写字母和数字，长度8-20位")
    private String password;
    
    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    
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
    
    /**
     * 昵称
     */
    private String nickName;
}