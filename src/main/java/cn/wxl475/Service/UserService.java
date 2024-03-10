package cn.wxl475.Service;


import cn.wxl475.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserService extends IService<User>{
    // 新增用户
//    void addUser(User user);

    User getByUsername(String username);

    void updateUser(User user);

    void updatePwd(String password);

//    void deleteByUids(List<Long> uids);

//    void updateTypes(List<User> userList);
}
