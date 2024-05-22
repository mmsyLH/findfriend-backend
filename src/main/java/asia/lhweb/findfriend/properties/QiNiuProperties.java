package asia.lhweb.findfriend.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 罗汉
 */
@Data
@ConfigurationProperties(prefix = "super.qiniu")
@Component
public class QiNiuProperties {

    private String accessKey;

    private String secretKey;

    private String bucket;

    private String url;
}
