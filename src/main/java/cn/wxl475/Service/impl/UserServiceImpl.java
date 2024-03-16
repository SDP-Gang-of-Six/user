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

    @Override
    public void updateUser(User user) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Long uid = (Long) map.get("uid");
        user.setUid(uid);
        userMapper.updateById(user);
//        // 将该用户的全部信息保存至redis缓存
//        User user1 = userMapper.selectById(uid);
//        cacheClient.setWithLogicalExpire(CACHE_USERS_KEY + user1.getUid().toString(),
//                user1, CACHE_USERS_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.delete(CACHE_USERS_KEY + uid);
    }

    @Override
    public void updatePwd(String password) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Long uid = (Long) map.get("uid");
        User user = userMapper.selectById(uid);
        user.setPassword(Md5Util.getMD5String(password));
        userMapper.updateById(user);
//        cacheClient.setWithLogicalExpire(CACHE_USERS_KEY + user.getUid().toString(),
//                user, CACHE_USERS_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.delete(CACHE_USERS_KEY + uid);
    }

    @DS("slave")
    @Override
    public List<User> getAllUsers() {
        return userMapper.selectList(null);
    }

    @DS("slave")
    @Override
    public User getUserById(Long uid) {
        return cacheClient.queryWithPassThrough(
                CACHE_USERS_KEY,
                uid,
                User.class,
                id ->  userMapper.selectById(uid),
                CACHE_USERS_TTL,
                TimeUnit.MINUTES
        );
    }


//    @Override
//    public void updateTypes(List<User> userList) {
//        for(User user: userList) {
//            userMapper.updateById(user);
//        }
//    }
}
