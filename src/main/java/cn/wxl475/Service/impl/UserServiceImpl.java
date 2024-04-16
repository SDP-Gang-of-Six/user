package cn.wxl475.Service.impl;

import cn.wxl475.mapper.UserMapper;
import cn.wxl475.Service.UserService;
import cn.wxl475.pojo.Illness;
import cn.wxl475.pojo.Page;
import cn.wxl475.pojo.User;
import cn.wxl475.redis.CacheClient;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.wxl475.redis.RedisConstants.*;


@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public void addUser(User user) {
        userMapper.insert(user);
    }

    @DS("slave")
    @Override
    public User getByUsername(String username) {
        return cacheClient.queryWithPassThrough(
                CACHE_USERS_KEY,
                LOCK_USERS_KEY,
                username,
                User.class,
                id ->  userMapper.selectOne(new QueryWrapper<User>().eq("username", username)),
                CACHE_USERS_TTL,
                TimeUnit.MINUTES
        );
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

    @Override
    public Page<User> getByNickname(String keyword, Integer pageNum, Integer pageSize, String sortField, Integer sortOrder) {
        Page<User> users = new Page<>(0L, new ArrayList<>());
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withPageable(PageRequest.of(pageNum - 1, pageSize));
        if(keyword != null && !keyword.isEmpty()){
            queryBuilder.withQuery(QueryBuilders.matchQuery("nickname", keyword));
        }
        if(sortField == null || sortField.isEmpty()){
            sortField = "uid";
        }
        if(sortOrder == null || !(sortOrder == 1 || sortOrder == -1)){
            sortOrder = -1;
        }
        queryBuilder.withSorts(SortBuilders.fieldSort(sortField).order(sortOrder == -1? SortOrder.DESC: SortOrder.ASC));
        SearchHits<User> hits = elasticsearchRestTemplate.search(queryBuilder.build(), User.class);
        hits.forEach(user -> users.getData().add(user.getContent()));
        users.setTotalNumber(hits.getTotalHits());
        System.out.println(users);
        return users;
    }

    @DS("slave")
    @Override
    public String getNicknameById(Long uid) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("nickname").eq("uid", uid);
        return userMapper.selectOne(queryWrapper).getNickname();
    }

}
