package com.zkl.zklaiagent.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
public class SysUser {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 昵称
     */
    private String nickName;
    
    /**
     * 用户类型
     */
    private String userType;
    
    /**
     * 状态（0正常 1停用）
     */
    private String status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否超级管理员
     */
    private Boolean superAdmin;
    
    /**
     * 区域代码
     */
    private String areaCode;
}