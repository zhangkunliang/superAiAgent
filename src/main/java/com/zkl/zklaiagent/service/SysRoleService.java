package com.zkl.zklaiagent.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 系统角色服务
 */
@Service
public class SysRoleService {
    
    /**
     * 获取应用ID列表
     */
    public List<String> getApplyIdList() {
        // 这里简单返回一些示例数据，实际应该从数据库查询
        return Arrays.asList("1", "2");
    }
}