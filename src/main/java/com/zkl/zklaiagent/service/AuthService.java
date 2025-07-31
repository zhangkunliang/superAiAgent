package com.zkl.zklaiagent.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.zkl.zklaiagent.constant.RedisKeyConstant;
import com.zkl.zklaiagent.dao.RedisDao;
import com.zkl.zklaiagent.dto.LoginBody;
import com.zkl.zklaiagent.dto.LoginVO;
import com.zkl.zklaiagent.dto.RegisterBody;
import com.zkl.zklaiagent.dto.SysUpdatePwdDto;
import com.zkl.zklaiagent.entity.LoginUser;
import com.zkl.zklaiagent.entity.SysUser;
import com.zkl.zklaiagent.util.LoginHelper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务
 */
@Service
@AllArgsConstructor
public class AuthService {
    
    private final RedisDao redisDao;
    
    // 模拟用户数据存储（实际应该使用数据库）
    private static final Map<String, SysUser> userStorage = new ConcurrentHashMap<>();
    private static Long userIdCounter = 1L;
    
    static {
        // 初始化一个测试用户
        SysUser testUser = new SysUser();
        testUser.setUserId(1L);
        testUser.setUsername("admin");
        testUser.setPassword(BCrypt.hashpw("Admin123", BCrypt.gensalt()));
        testUser.setNickName("管理员");
        testUser.setUserType("admin");
        testUser.setStatus("0");
        testUser.setSuperAdmin(true);
        testUser.setAreaCode("000001");
        testUser.setCreateTime(LocalDateTime.now());
        userStorage.put("admin", testUser);
        userIdCounter = 2L;
    }
    
    /**
     * 用户登录
     */
    public LoginVO login(LoginBody loginBody) {
        // 校验验证码
        String redisKey = String.format("%s:%s", RedisKeyConstant.LOGIN_CAPTCHA, loginBody.getCaptchaKey());
        Object captchaCode = redisDao.get(redisKey);
        if (captchaCode == null) {
            throw new RuntimeException("验证码已过期");
        }
        if (!captchaCode.toString().equalsIgnoreCase(loginBody.getCaptcha())) {
            throw new RuntimeException("验证码错误");
        }
        
        // 删除验证码
        redisDao.delete(redisKey);
        
        // 查找用户
        SysUser user = userStorage.get(loginBody.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 校验密码
        if (!BCrypt.checkpw(loginBody.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        // 检查用户状态
        if (!"0".equals(user.getStatus())) {
            throw new RuntimeException("用户已被停用");
        }
        
        // 登录成功，生成token
        StpUtil.login(user.getUserId());
        
        // 设置登录用户信息
        LoginUser loginUser = new LoginUser();
        BeanUtil.copyProperties(user, loginUser);
        loginUser.setLoginTime(System.currentTimeMillis());
        LoginHelper.setLoginUser(loginUser);
        
        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(StpUtil.getTokenValue());
        loginVO.setUserId(user.getUserId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickName(user.getNickName());
        loginVO.setUserType(user.getUserType());
        loginVO.setExpireTime(System.currentTimeMillis() + StpUtil.getTokenTimeout() * 1000);
        
        return loginVO;
    }
    
    /**
     * 用户注册
     */
    public void register(RegisterBody registerBody) {
        // 校验验证码
        String redisKey = String.format("%s:%s", RedisKeyConstant.REGISTER_CAPTCHA, registerBody.getCaptchaKey());
        Object captchaCode = redisDao.get(redisKey);
        if (captchaCode == null) {
            throw new RuntimeException("验证码已过期");
        }
        if (!captchaCode.toString().equalsIgnoreCase(registerBody.getCaptcha())) {
            throw new RuntimeException("验证码错误");
        }
        
        // 删除验证码
        redisDao.delete(redisKey);
        
        // 校验密码确认
        if (!registerBody.getPassword().equals(registerBody.getConfirmPassword())) {
            throw new RuntimeException("两次密码输入不一致");
        }
        
        // 检查用户名是否已存在
        if (userStorage.containsKey(registerBody.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建新用户
        SysUser newUser = new SysUser();
        newUser.setUserId(userIdCounter++);
        newUser.setUsername(registerBody.getUsername());
        newUser.setPassword(BCrypt.hashpw(registerBody.getPassword(), BCrypt.gensalt()));
        newUser.setNickName(registerBody.getNickName() != null ? registerBody.getNickName() : registerBody.getUsername());
        newUser.setUserType("user");
        newUser.setStatus("0");
        newUser.setSuperAdmin(false);
        newUser.setAreaCode("000001");
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        
        // 保存用户
        userStorage.put(newUser.getUsername(), newUser);
    }
    
    /**
     * 修改密码
     */
    public void updatePwd(SysUpdatePwdDto dto) {
        // 校验新密码确认
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("两次密码输入不一致");
        }
        
        // 获取当前用户
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        
        SysUser user = userStorage.get(loginUser.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 校验旧密码
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 更新密码
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        user.setUpdateTime(LocalDateTime.now());
    }
    
    /**
     * 解绑用户账号（示例方法）
     */
    public void unbind(Long userId) {
        // 这里是示例实现，实际应该根据业务需求实现解绑逻辑
        System.out.println("解绑用户账号: " + userId);
    }
    
    /**
     * 获取用户权限
     */
    public Map<String, Object> getAuthority(String applyId) {
        Map<String, Object> result = new HashMap<>();
        
        // 这里是示例实现，实际应该根据用户角色和应用ID查询权限
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser != null && loginUser.getSuperAdmin()) {
            result.put("permissions", new String[]{"*:*:*"});
            result.put("roles", new String[]{"admin"});
        } else {
            result.put("permissions", new String[]{"system:user:view"});
            result.put("roles", new String[]{"user"});
        }
        
        return result;
    }
}