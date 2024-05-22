package asia.lhweb.findfriend.service.impl;

import asia.lhweb.findfriend.mapper.TagMapper;
import asia.lhweb.findfriend.model.domain.Tag;
import asia.lhweb.findfriend.service.TagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 罗汉
 * @description 针对表【tag】的数据库操作Service实现
 * @createDate 2023-05-07 19:05:01
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
}




