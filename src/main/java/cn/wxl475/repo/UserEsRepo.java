package cn.wxl475.repo;

import cn.wxl475.pojo.User;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserEsRepo extends ElasticsearchRepository<User, Long> {
}
