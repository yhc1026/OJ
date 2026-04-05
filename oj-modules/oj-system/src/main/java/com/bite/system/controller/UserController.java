package com.bite.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bite.domain.Result;
import com.bite.system.domain.User;
import com.bite.system.domain.dto.UserUpdateByPermissionRequest;
import com.bite.system.service.UserService;
import org.springframework.web.bind.annotation.*;

/**
 * C 端用户（tb_user）管理接口。
 */
@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<IPage<User>> list(@RequestParam(value = "page", defaultValue = "1") long page) {
        return userService.list(page);
    }

    @GetMapping("/getUserById")
    public Result<User> getUserById(@RequestParam("userId") Long userId) {
        return userService.getUserById(userId);
    }

    @GetMapping("/getUserByName")
    public Result<User> getUserByName(@RequestParam("name") String name) {
        return userService.getUserByName(name);
    }

    @PostMapping("/addUser")
    public Result<Long> addUser(@RequestBody User body) {
        return userService.addUser(body);
    }

    @DeleteMapping("/deleteUserById")
    public Result<Boolean> deleteUserById(@RequestParam("userId") Long userId) {
        return userService.deleteUserById(userId);
    }

    @DeleteMapping("/deleteUserByName")
    public Result<Integer> deleteUserByName(@RequestParam("name") String name) {
        return userService.deleteUserByName(name);
    }

    @PutMapping("/updateUserById")
    public Result<Boolean> updateUserById(@RequestBody User body) {
        return userService.updateUserById(body);
    }

    @PutMapping("/updateUserByName")
    public Result<Integer> updateUserByName(@RequestBody User body) {
        return userService.updateUserByName(body);
    }

    /**
     * 统一编辑普通用户（经网关 token 鉴权）：
     * - root(sysuser) 可改所有普通 user；
     * - 普通 sysuser 可改所有普通 user；
     * - 普通 user 仅可改自己。
     */
    @PutMapping("/updateUserByPermission")
    public Result<Boolean> updateUserByPermission(@RequestBody UserUpdateByPermissionRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        return userService.updateUserByPermission(request.getTargetUserId(), request.getContent());
    }

    @PutMapping("/banUserById")
    public Result<Boolean> banUserById(@RequestParam("userId") Long userId) {
        return userService.banUserById(userId);
    }

    @PutMapping("/unbanUserById")
    public Result<Boolean> unbanUserById(@RequestParam("userId") Long userId) {
        return userService.unbanUserById(userId);
    }
}

