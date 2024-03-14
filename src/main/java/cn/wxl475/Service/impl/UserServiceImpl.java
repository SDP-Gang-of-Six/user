package cn.wxl475.Service.impl;

import cn.wxl475.mapper.UserMapper;
import cn.wxl475.Service.UserService;
import cn.wxl475.pojo.User;
import cn.wxl475.utils.Md5Util;
import cn.wxl475.utils.ThreadLocalUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

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

    @Override
    public void updateUser(User user) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Long uid = (Long) map.get("uid");
        user.setUid(uid);
        userMapper.updateById(user);
    }

    @Override
    public void updatePwd(String password) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Long uid = (Long) map.get("uid");
        User user = userMapper.selectById(uid);
        user.setPassword(Md5Util.getMD5String(password));
        userMapper.updateById(user);
    }

    @DS("slave")
    @Override
    public List<User> getAllUsers() {
        return userMapper.selectList(null);
    }



//    @Override
//    public void updateTypes(List<User> userList) {
//        for(User user: userList) {
//            userMapper.updateById(user);
//        }
//    }
}
