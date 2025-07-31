package com.zkl.zklaiagent.common;

import lombok.Data;

/**
 * 通用响应结果
 */
@Data
public class ResResult<T> {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 成功标识
     */
    private Boolean success;
    
    public ResResult() {}
    
    public ResResult(Integer code, String message, T data, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }
    
    /**
     * 成功响应
     */
    public static <T> ResResult<T> ok() {
        return new ResResult<>(200, "操作成功", null, true);
    }
    
    /**
     * 成功响应带数据
     */
    public static <T> ResResult<T> ok(T data) {
        return new ResResult<>(200, "操作成功", data, true);
    }
    
    /**
     * 成功响应带消息和数据
     */
    public static <T> ResResult<T> ok(String message, T data) {
        return new ResResult<>(200, message, data, true);
    }
    
    /**
     * 失败响应
     */
    public static <T> ResResult<T> error() {
        return new ResResult<>(500, "操作失败", null, false);
    }
    
    /**
     * 失败响应带消息
     */
    public static <T> ResResult<T> error(String message) {
        return new ResResult<>(500, message, null, false);
    }
    
    /**
     * 失败响应带码和消息
     */
    public static <T> ResResult<T> error(Integer code, String message) {
        return new ResResult<>(code, message, null, false);
    }
}