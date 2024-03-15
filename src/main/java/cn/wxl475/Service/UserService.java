package cn.wxl475.Service;



import cn.wxl475.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserService extends IService<User>{

    void addUser(User user);

    User getByUsername(String username);

    void updateUser(User user);

    void updatePwd(String password);

    List<User> getAllUsers();

    User getUserById(Long uid);

//    List<User> findAllUserByPage(int pageNum, int pageSize);

//    void deleteByUids(List<Long> uids);

//    void updateTypes(List<User> userList);
}
