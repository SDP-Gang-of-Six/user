package cn.wxl475.controller;


import cn.wxl475.Service.UserService;
import cn.wxl475.pojo.Result;
import cn.wxl475.pojo.User;
import cn.wxl475.utils.JwtUtils;
import cn.wxl475.utils.Md5Util;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/user")
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.signKey}")
    private String signKey;

    @Value("${jwt.expire}")
    private Long expire;

    @PostMapping("/addUser")
    public Result addUser(@RequestBody User user) {
        String username = user.getUsername();
        //查询用户
        User u = userService.getByUsername(username);
        if (u == null) {
            //没有占用
            //注册
            user.setPassword("123456");
            user.setDeleted(false);
            userService.save(user);
            return Result.success();
        } else {
            //占用
            return Result.error("该账号已存在");
        }
    }

    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        String username = user.getUsername();
        String password = user.getPassword();
        //根据用户名查询用户
        User loginUser = userService.getByUsername(username);
        //判断该用户是否存在
        if (loginUser == null) {
            return Result.error("用户名错误");
        }

        //判断密码是否正确  loginUser对象中的password是密文
        if (Md5Util.getMD5String(password).equals(loginUser.getPassword())) {
            //登录成功
            Map<String, Object> claims = new HashMap<>();
            claims.put("uid", loginUser.getUid());
            claims.put("username", loginUser.getUsername());
            claims.put("userType", loginUser.getUserType());
            String token = JwtUtils.generateJwt(claims, signKey, expire);

            //把token存储到redis中
//            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
//            operations.set(token,token,1, TimeUnit.HOURS);

            return Result.success(token);
        }
        return Result.error("密码错误");
    }

    @PostMapping("/updateUser")
    public Result updateUser(@RequestBody User user) {
        userService.updateUser(user);
        return Result.success();
    }

    @GetMapping("/updatePwd/{password}")
    public Result updatePwd(@PathVariable String password) {
        userService.updatePwd(password);
        //删除redis中对应的token
//        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
//        operations.getOperations().delete(token);
        return Result.success();
    }

    @PostMapping("/updateUserTypes")
    public Result updateUserTypes(@RequestBody List<User> userList) {
        userService.updateBatchById(userList);
        return Result.success();
    }

    @PostMapping("/deleteUsers")
    public Result deleteUsers(@RequestBody List<User> userList) {
        userService.updateBatchById(userList);
        return Result.success();
    }

    @GetMapping("/findOne/{uid}")
    public Result findOne(@PathVariable Long uid) {
        return Result.success(userService.getById(uid));
    }

    // 分页查询全部用户
    @GetMapping("/findAllUserByPage/{pageNum}/{pageSize}")
    public Result findAllUserByPage(@PathVariable Integer pageNum, @PathVariable Integer pageSize) {
        return Result.success(userService.findAllUserByPage(pageNum, pageSize));
    }

}
