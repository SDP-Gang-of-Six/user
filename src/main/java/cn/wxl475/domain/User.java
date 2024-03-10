package cn.wxl475.domain;


import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {
    //对应数据库中的主键（UUID、自增id、雪花算法、redis、zookeeper）
    @TableId(type = IdType.ASSIGN_ID)
    private Long uid;

    @TableField("nickname")
    private String nickname;

    @TableField("password")
    private String password;

    @TableField("username")
    private String username;

    @TableField("user_type")
    private Boolean userType;

    //注解填充字段 @TableField(.. fill = FieldFill.INSERT) 生成器策略部分也可以配置！
    @TableField(fill = FieldFill.INSERT)
    private Data createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Data updateTime;
}
