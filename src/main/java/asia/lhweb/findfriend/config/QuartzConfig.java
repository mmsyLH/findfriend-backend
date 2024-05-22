package asia.lhweb.findfriend.config;

import asia.lhweb.findfriend.jobs.DisbandExpiredTeam;
import asia.lhweb.findfriend.jobs.UserRecommendationCache;

import asia.lhweb.findfriend.properties.FindFriendProperties;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Quartz配置
 *
 * @author 罗汉
 * @date 2023/07/28
 */
@Configuration
public class QuartzConfig {

    @Resource
    private FindFriendProperties findFriendProperties;

    /**
     * 解散团队到期工作细节
     *
     * @return {@link JobDetail}
     */
    @Bean
    public JobDetail disbandExpireTeamJobDetail() {
        return JobBuilder.newJob(DisbandExpiredTeam.class).storeDurably().build();
    }

    /**
     * 解散团队触发到期
     *
     * @return {@link Trigger}
     */
    @Bean
    public Trigger disbandExpireTeamTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(findFriendProperties.getJob());
        if (findFriendProperties.isEnableAutoDisbandment()) {
            return TriggerBuilder.newTrigger()
                    .forJob(disbandExpireTeamJobDetail())
                    .withSchedule(cronScheduleBuilder).build();
        } else {
            return null;
        }
    }

    /**
     * 用户推荐缓存工作细节
     *
     * @return {@link JobDetail}
     */
    @Bean
    public JobDetail userRecommendationCacheJobDetail() {
        return JobBuilder.newJob(UserRecommendationCache.class).storeDurably().build();
    }

    /**
     * 用户推荐缓存触发
     *
     * @return {@link Trigger}
     */
    @Bean
    public Trigger userRecommendationCacheTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(findFriendProperties.getJob());
        if (findFriendProperties.isEnableAutoUserCache()) {
            return TriggerBuilder.newTrigger()
                    .forJob(userRecommendationCacheJobDetail())
                    .withSchedule(cronScheduleBuilder).build();
        } else {
            return null;
        }
    }
}
