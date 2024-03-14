package cn.wxl475.mapper;


import cn.wxl475.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper extends BaseMapper<User> {

//    @Select("select * from user.user where username = #{username}")
//    User getByUsername(String username);
}

