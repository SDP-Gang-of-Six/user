package cn.wxl475.controller;


import cn.wxl475.Service.UserService;
import cn.wxl475.pojo.Result;
import cn.wxl475.pojo.User;
import cn.wxl475.repo.UserEsRepo;
import cn.wxl475.utils.JwtUtils;
import cn.wxl475.utils.Md5Util;
import cn.wxl475.utils.PasswordValidator;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.CACHE_USERS_KEY;

@Slf4j
@RequestMapping("/user")
@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserEsRepo userEsRepo;

    //用户登录次数计数redisKey前缀
    private final String LOGIN_COUNT = "login-count:";
    //用户登录是否被锁定一小时redisKey前缀
    private final String IS_LOCK = "is-lock:";

    @Value("${jwt.signKey}")
    private String signKey;

    @Value("${jwt.expire}")
    private Long expire;

    @PostMapping("/addUser")
    public Result addUser(@RequestHeader("Authorization") String token, @RequestBody User user) {
        Claims claims = JwtUtils.parseJWT(token, signKey);
        Boolean userType = (Boolean) claims.get("userType");
        if(!userType) {
            return Result.error("无增加用户权限");
        }
        String username = user.getUsername();
        //查询用户
        User u = userService.getByUsername(username);
        if (u == null) {
            //没有占用
            //注册
            user.setPassword(Md5Util.getMD5String("pet123456"));
            user.setUserType(false);
            userService.addUser(user);
            System.out.println(user);
            userEsRepo.save(user);
            return Result.success();
        } else {
            //占用
            return Result.error("该账号已存在");
        }
    }

    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String username = user.getUsername();
        String password = user.getPassword();
        if("LOCK".equals(operations.get(IS_LOCK + username))) {
            return Result.error("密码错误次数过多，请稍后再尝试");
        }
        //根据用户名查询用户
        User loginUser = userService.getByUsername(username);
        //判断该用户是否存在
        if (loginUser == null) {
            return Result.error("该用户不存在");
        }

        //判断密码是否正确  loginUser对象中的password是密文
        if (!Md5Util.getMD5String(password).equals(loginUser.getPassword())) {
            operations.increment(LOGIN_COUNT + username, 1);
            if (Integer.parseInt(operations.get(LOGIN_COUNT + username)) >= 5) {
                operations.set(IS_LOCK + username, "LOCK", 1, TimeUnit.MINUTES);
                stringRedisTemplate.expire(LOGIN_COUNT + username, 1, TimeUnit.MINUTES);
                return Result.error("密码错误次数过多，请稍后再尝试");
            }
            return Result.error("密码错误");
        }

        //登录成功
        operations.set(LOGIN_COUNT + username, "0");
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", loginUser.getUid());
        claims.put("username", loginUser.getUsername());
        claims.put("userType", loginUser.getUserType());
        String token = JwtUtils.generateJwt(claims, signKey, expire);
        return Result.success(token);
    }


    @GetMapping("/updatePwd/{password}")
    public Result updatePwd(@RequestHeader("Authorization") String token , @PathVariable String password) {
        if(!PasswordValidator.isCharacterAndNumber(password)) {
            return Result.error("请输入长度为8-16的同时包含数字和字母的密码");
        }

        Claims claims = JwtUtils.parseJWT(token, signKey);

        // 需要先转为String类型，再转为Long类型
        String uidStr = String.valueOf(claims.get("uid"));
        Long uid = Long.parseLong(uidStr);

        User loginUser = userService.getUserById(uid);
        loginUser.setPassword(Md5Util.getMD5String(password));
        userService.updateById(loginUser);
        userEsRepo.save(loginUser);
        stringRedisTemplate.delete(CACHE_USERS_KEY + uid);
        return Result.success();
    }

    @PostMapping("/updateUserTypes")
    public Result updateUserTypes(@RequestHeader("Authorization") String token, @RequestBody List<User> userList) {
        Claims claims = JwtUtils.parseJWT(token, signKey);
        Boolean userType = (Boolean) claims.get("userType");
        if(!userType) {
            return Result.error("无修改用户类型权限");
        }
        userService.updateBatchById(userList);
        List<User> users = new ArrayList<>();
        for(User user: userList) {
            Long uid = user.getUid();
            User newUser = userService.getUserById(uid);
            users.add(newUser);
        }
        System.out.println(users);
        userEsRepo.saveAll(users);
        for(User user: userList) {
            Long uid = user.getUid();
            stringRedisTemplate.delete(CACHE_USERS_KEY + uid);
        }
        return Result.success();
    }

    @PostMapping("/deleteUsers")
    public Result deleteUsers(@RequestHeader("Authorization") String token, @RequestBody List<Long> ids) {
        Claims claims = JwtUtils.parseJWT(token, signKey);
        Boolean userType = (Boolean) claims.get("userType");
        if(!userType) {
            return Result.error("无删除用户权限");
        }
        userService.removeByIds(ids);
        userEsRepo.deleteAllById(ids);
        for(Long id: ids) {
            stringRedisTemplate.delete(CACHE_USERS_KEY + id);
        }
        return Result.success();
    }

    @GetMapping("/getUserById/{uid}")
    public Result getUserById(@RequestHeader("Authorization") String token, @PathVariable Long uid) {
        User user = userService.getUserById(uid);
        if(user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @PostMapping("/getByNickname")
    public Result getByNickname(@RequestHeader("Authorization") String token,
                                         @RequestParam String keyword,
                                         @RequestParam Integer pageNum,
                                         @RequestParam Integer pageSize,
                                         @RequestParam(required = false) String sortField,
                                         @RequestParam(required = false) Integer sortOrder){
        if(pageNum <= 0 || pageSize <= 0){
            return Result.error("页码或页大小不合法");
        }
        return Result.success(userService.getByNickname(keyword,pageNum,pageSize,sortField,sortOrder));
    }

    @GetMapping("getByUsername/{username}")
    public Result getByUsername(@RequestHeader("Authorization") String token, @PathVariable String username) {
        User user = userService.getByUsername(username);
        if(user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    // 分页查询全部用户
    @GetMapping("/userPage/{pageNum}/{pageSize}")
    public Result userPage(@RequestHeader("Authorization") String token, @PathVariable Integer pageNum, @PathVariable Integer pageSize){
        try {
            //1.引入分页插件,pageNum是第几页，pageSize是每页显示多少条,默认查询总数count
            PageHelper.startPage(pageNum, pageSize);
            //2.紧跟的查询就是一个分页查询-必须紧跟.后面的其他查询不会被分页
            List<User> userList = userService.getAllUsers();
            //3.使用PageInfo包装查询后的结果, pageSize是连续显示的条数
            PageInfo pageInfo = new PageInfo(userList, pageSize);
            return Result.success(pageInfo);
        }finally {
            //清理 ThreadLocal 存储的分页参数,保证线程安全
            PageHelper.clearPage();
        }
    }


}
