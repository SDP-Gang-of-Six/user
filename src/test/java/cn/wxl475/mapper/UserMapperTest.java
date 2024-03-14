package cn.wxl475.mapper;


import cn.wxl475.pojo.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void selectTest() {
        User user = userMapper.selectById(1L);
        System.out.println(user);
    }
}
