package com.zkl.zklaiagent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.zkl.zklaiagent.common.ResResult;
import com.zkl.zklaiagent.constant.RedisKeyConstant;
import com.zkl.zklaiagent.dao.RedisDao;
import com.zkl.zklaiagent.dto.*;
import com.zkl.zklaiagent.entity.LoginUser;
import com.zkl.zklaiagent.service.AuthService;
import com.zkl.zklaiagent.service.SysRoleService;
import com.zkl.zklaiagent.util.LoginHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统认证控制器
 */
@Slf4j
@RestController
@Tag(name = "系统认证", description = "登录、注册、验证码等认证相关接口")
@AllArgsConstructor
@RequestMapping("/auth")
public class SysAuthController {

    private final RedisDao redisDao;
    private final AuthService authService;
    private final SysRoleService sysRoleService;

    /**
     * 获取登录图片验证码
     */
    @Operation(summary = "获取登录验证码")
    @GetMapping("/captcha")
    public ResResult<Map<String, Object>> captcha() {
        String key = IdUtil.fastSimpleUUID();
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(150, 40, 5, 50);
        String redisKey = String.format("%s:%s", RedisKeyConstant.LOGIN_CAPTCHA, key);
        redisDao.set(redisKey, lineCaptcha.getCode(), 300L);
        Map<String, Object> map = new HashMap<>(2);
        map.put("key", key);
        map.put("captcha", lineCaptcha.getImageBase64());
        log.info("登录验证码:{}, key:{}", lineCaptcha.getCode(), key);
        return ResResult.ok(map);
    }

    /**
     * 获取注册图片验证码
     */
    @Operation(summary = "获取注册验证码")
    @GetMapping("/register/captcha")
    public ResResult<Map<String, Object>> registerCaptcha() {
        String key = IdUtil.fastSimpleUUID();
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(150, 40, 5, 50);
        String redisKey = String.format("%s:%s", RedisKeyConstant.REGISTER_CAPTCHA, key);
        redisDao.set(redisKey, lineCaptcha.getCode(), 300L);
        Map<String, Object> map = new HashMap<>(2);
        map.put("key", key);
        map.put("captcha", lineCaptcha.getImageBase64());
        log.info("注册验证码:{}, key:{}", lineCaptcha.getCode(), key);
        return ResResult.ok(map);
    }

    /**
     * 账号密码登录
     */
    @Operation(summary = "账号密码登录")
    @PostMapping("/login")
    public ResResult<LoginVO> login(@RequestBody @Validated LoginBody loginBody) {
        try {
            LoginVO loginVO = authService.login(loginBody);
            return ResResult.ok(loginVO);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return ResResult.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ResResult<String> register(@RequestBody @Valid RegisterBody registerBody) {
        try {
            authService.register(registerBody);
            return ResResult.ok("注册成功");
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage());
            return ResResult.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PostMapping("/updatePwd")
    @SaCheckLogin(type = StpUtil.TYPE)
    public ResResult<Object> updatePwd(@RequestBody @Valid SysUpdatePwdDto dto) {
        try {
            authService.updatePwd(dto);
            return ResResult.ok("密码修改成功");
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage());
            return ResResult.error(e.getMessage());
        }
    }

    /**
     * 微信解绑用户账号
     */
    @Operation(summary = "微信解绑用户账号")
    @PostMapping("/unbind")
    @SaCheckLogin(type = StpUtil.TYPE)
    public ResResult<String> unbind() {
        try {
            authService.unbind(LoginHelper.getUserId());
            return ResResult.ok("解绑成功");
        } catch (Exception e) {
            log.error("解绑失败: {}", e.getMessage());
            return ResResult.error(e.getMessage());
        }
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @GetMapping("/logout")
    public ResResult<String> logout() {
        try {
            StpUtil.logout();
            return ResResult.ok("退出成功");
        } catch (Exception e) {
            log.error("退出登录失败: {}", e.getMessage());
            return ResResult.error(e.getMessage());
        }
    }

    /**
     * 获取登录用户信息
     */
    @Operation(summary = "获取登录用户信息")
    @GetMapping("/getUserInfo")
    @SaCheckLogin(type = StpUtil.TYPE)
    public ResResult<Map<String, Object>> getUserInfo() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            Map<String, Object> result = BeanUtil.beanToMap(loginUser, "userId", "nickName", "userType");
            result.put("areaCode", loginUser.getAreaCode());
            List<String> list = sysRoleService.getApplyIdList();
            if (LoginHelper.isSuperAdmin()) {
                list = new ArrayList<>();
                list.add("1");
                list.add("2");
                list.add("3");
            }
            result.put("applyIdList", list);
            return ResResult.ok(result);
        } catch (Exception e) {
            log.error("获取用户信息失败: {}", e.getMessage());
            return ResResult.error(e.getMessage());
        }
    }

    /**
     * 获取登录用户权限
     */
    @Operation(summary = "获取登录用户权限")
    @GetMapping("/getAuthority")
    @SaCheckLogin(type = StpUtil.TYPE)
    public ResResult<Map<String, Object>> getAuthority(String applyId) {
        try {
            return ResResult.ok(authService.getAuthority(applyId));
        } catch (Exception e) {
            log.error("获取用户权限失败: {}", e.getMessage());
            return ResResult.error(e.getMessage());
        }
    }
}
