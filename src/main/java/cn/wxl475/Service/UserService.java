package cn.wxl475.Service;



import cn.wxl475.pojo.Page;
import cn.wxl475.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserService extends IService<User>{

    void addUser(User user);

    User getByUsername(String username);

    List<User> getAllUsers();

    User getUserById(Long uid);

    Page<User> getByNickname(String keyword, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder);

    String getNicknameById(Long uid);
}
