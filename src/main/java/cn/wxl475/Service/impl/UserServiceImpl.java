package cn.wxl475.Service.impl;

import cn.wxl475.mapper.UserMapper;
import cn.wxl475.Service.UserService;
import cn.wxl475.mysql.ReadOnly;
import cn.wxl475.pojo.User;
import cn.wxl475.utils.Md5Util;
import cn.wxl475.utils.ThreadLocalUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
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

//    @Override
//    public void addUser(User user) {
//        userMapper.insert(user);
//    }

    @Override
    @ReadOnly
    public User getByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<User>().eq("username", username);
        return userMapper.selectOne(wrapper);
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

    @Override
    @ReadOnly
    public List<User> findAllUserByPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<User> lists = userMapper.selectList(null);
        return lists;
    }


//    @Override
//    public void deleteByUids(List<Long> uids) {
//        userMapper.deleteBatchIds(uids);
//    }

//    @Override
//    public void updateTypes(List<User> userList) {
//        for(User user: userList) {
//            userMapper.updateById(user);
//        }
//    }
}
