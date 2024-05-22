package asia.lhweb.findfriend.listener;

import asia.lhweb.findfriend.model.domain.Blog;
import asia.lhweb.findfriend.model.domain.Team;
import asia.lhweb.findfriend.model.domain.User;
import asia.lhweb.findfriend.properties.FindFriendProperties;
import asia.lhweb.findfriend.service.BlogService;
import asia.lhweb.findfriend.service.TeamService;
import asia.lhweb.findfriend.service.UserService;
import cn.hutool.bloomfilter.BitSetBloomFilter;
import cn.hutool.bloomfilter.BloomFilter;
import cn.hutool.bloomfilter.BloomFilterUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static asia.lhweb.findfriend.constants.BloomFilterConstants.BLOG_BLOOM_PREFIX;
import static asia.lhweb.findfriend.constants.BloomFilterConstants.EXPECTED_INCLUSION_RECORD;
import static asia.lhweb.findfriend.constants.BloomFilterConstants.HASH_FUNCTION_NUMBER;
import static asia.lhweb.findfriend.constants.BloomFilterConstants.PRE_OPENED_MAXIMUM_INCLUSION_RECORD;
import static asia.lhweb.findfriend.constants.BloomFilterConstants.TEAM_BLOOM_PREFIX;
import static asia.lhweb.findfriend.constants.BloomFilterConstants.USER_BLOOM_PREFIX;
import static asia.lhweb.findfriend.constants.RedisConstants.USER_RECOMMEND_KEY;

/**
 * 启动侦听器
 *
 * @author 罗汉
 * @date 2024/01/25
 */
@Configuration
@Log4j2
public class StartupListener implements CommandLineRunner {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private BlogService blogService;

    @Resource
    private FindFriendProperties findFriendProperties;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 启动
     *
     * @param args args
     */
    @Override
    public void run(String... args) {
        if (findFriendProperties.isEnableBloomFilter()) {
            long begin = System.currentTimeMillis();
            log.info("Starting init BloomFilter......");
            this.initBloomFilter();
            long end = System.currentTimeMillis();
            String cost = end - begin + " ms";
            log.info("BloomFilter initialed in " + cost);
        }
        if (!findFriendProperties.isEnableCache()) {
            long begin = System.currentTimeMillis();
            log.info("Starting delete cache from redis......");
            this.deleteCache();
            long end = System.currentTimeMillis();
            String cost = end - begin + " ms";
            log.info("Cache has been deleted in " + cost);
        }
    }


    /**
     * 删除原有缓存
     */
    @Bean
    public void deleteCache() {
        String key = USER_RECOMMEND_KEY + "*";
        Set<String> keys = stringRedisTemplate.keys(key);
        if (!(keys == null || keys.isEmpty())) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 初始化布隆过滤器
     *
     * @return {@link BloomFilter}
     */
    @Bean
    public BloomFilter initBloomFilter() {
        BitSetBloomFilter bloomFilter = BloomFilterUtil.createBitSet(
                PRE_OPENED_MAXIMUM_INCLUSION_RECORD,
                EXPECTED_INCLUSION_RECORD,
                HASH_FUNCTION_NUMBER
        );
        List<User> userList = userService.list(null);
        for (User user : userList) {
            bloomFilter.add(USER_BLOOM_PREFIX + user.getId());
        }
        List<Team> teamList = teamService.list(null);
        for (Team team : teamList) {
            bloomFilter.add(TEAM_BLOOM_PREFIX + team.getId());
        }

        List<Blog> blogList = blogService.list(null);
        for (Blog blog : blogList) {
            bloomFilter.add(BLOG_BLOOM_PREFIX + blog.getId());
        }
        return bloomFilter;
    }
}
