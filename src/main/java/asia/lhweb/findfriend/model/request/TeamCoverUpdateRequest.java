package asia.lhweb.findfriend.model.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 团队封面更新请求
 *
 * @author 罗汉
 * @date 2024/01/25
 */
@Data
public class TeamCoverUpdateRequest {
    private Long id;
    private MultipartFile file;
}
