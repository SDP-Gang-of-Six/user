package cn.wxl475.Service.impl;

import cn.wxl475.mapper.UserMapper;
import cn.wxl475.Service.UserService;
import cn.wxl475.pojo.Question;
import cn.wxl475.pojo.User;
import cn.wxl475.redis.CacheClient;
import cn.wxl475.utils.Md5Util;
import cn.wxl475.utils.ThreadLocalUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;
import static cn.wxl475.redis.RedisConstants.CACHE_QUESTION_TTL;


@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public void addUser(User user) {
        userMapper.insert(user);
    }

    @DS("slave")
    @Override
    public User getByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<User>().eq("username", username);
        return userMapper.selectOne(wrapper);
//        return userMapper.getByUsername(username);
    }

    @DS("slave")
    @Override
    public List<User> getByNickname(String nickname) {
        QueryWrapper<User> wrapper = new QueryWrapper<User>().eq("nickname", nickname);
        return userMapper.selectList(wrapper);
    }

    @DS("slave")
    @Override
    public List<User> getAllUsers() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc(true, "user_type");
        return userMapper.selectList(wrapper);
    }

    @DS("slave")
    @Override
    public User getUserById(Long uid) {
        return cacheClient.queryWithPassThrough(
                CACHE_USERS_KEY,
                LOCK_USERS_KEY,
                uid,
                User.class,
                id ->  userMapper.selectById(uid),
                CACHE_USERS_TTL,
                TimeUnit.MINUTES
        );
    }

}
