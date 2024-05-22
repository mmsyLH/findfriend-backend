package asia.lhweb.findfriend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 应用程序
 *
 * @author 罗汉
 * @date 2024/01/25
 */
@SpringBootApplication
@MapperScan("asia.lhweb.findfriend.mapper")
@EnableRedisHttpSession
@EnableAspectJAutoProxy
public class FindFriendApplication {
    protected FindFriendApplication() {
    }

    /**
     * 程序入口
     *
     * @param args args
     */
    public static void main(String[] args) {
        SpringApplication.run(FindFriendApplication.class, args);
    }
}

