package asia.lhweb.findfriend.service.impl;

import asia.lhweb.findfriend.common.ErrorCode;
import asia.lhweb.findfriend.constants.FriendConstant;
import asia.lhweb.findfriend.constants.RedissonConstant;
import asia.lhweb.findfriend.exception.BusinessException;
import asia.lhweb.findfriend.mapper.FriendsMapper;
import asia.lhweb.findfriend.model.domain.Friends;
import asia.lhweb.findfriend.model.domain.User;
import asia.lhweb.findfriend.model.request.FriendAddRequest;
import asia.lhweb.findfriend.model.vo.FriendsRecordVO;
import asia.lhweb.findfriend.service.FriendsService;
import asia.lhweb.findfriend.service.UserService;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 朋友服务实现
 *
 * @author 罗汉
 * @description 针对表【friends(好友申请管理表)】的数据库操作Service实现
 * @createDate 2023-06-18 14:10:45
 * @date 2024/01/25
 */
@Log4j2
@Service
public class FriendsServiceImpl extends ServiceImpl<FriendsMapper, Friends>
        implements FriendsService {

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 添加好友记录
     *
     * @param loginUser        登录用户
     * @param friendAddRequest 好友添加请求
     * @return boolean
     */
    @Override
    public boolean addFriendRecords(User loginUser, FriendAddRequest friendAddRequest) {
        validateRequest(loginUser, friendAddRequest);
        RLock lock = redissonClient.getLock(RedissonConstant.APPLY_LOCK + loginUser.getId());
        try {
            // 抢到锁并执行
            if (lock.tryLock(RedissonConstant.DEFAULT_WAIT_TIME, RedissonConstant.DEFAULT_LEASE_TIME, TimeUnit.MILLISECONDS)) {
                // 2.条数大于等于1 就不能再添加
                LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
                friendsLambdaQueryWrapper.eq(Friends::getReceiveId, friendAddRequest.getReceiveId());
                friendsLambdaQueryWrapper.eq(Friends::getFromId, loginUser.getId());
                List<Friends> list = this.list(friendsLambdaQueryWrapper);
                list.forEach(friends -> {
                    if (list.size() > 1 && friends.getStatus() == FriendConstant.DEFAULT_STATUS) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复申请");
                    }
                });
                Friends newFriend = new Friends();
                newFriend.setFromId(loginUser.getId());
                newFriend.setReceiveId(friendAddRequest.getReceiveId());
                if (StringUtils.isBlank(friendAddRequest.getRemark())) {
                    newFriend.setRemark("我是" + userService.getById(loginUser.getId()).getUsername());
                } else {
                    newFriend.setRemark(friendAddRequest.getRemark());
                }
                newFriend.setCreateTime(new Date());
                return this.save(newFriend);
            }
        } catch (InterruptedException e) {
            log.error("joinTeam error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                log.info("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * 校验添加好友请求
     *
     * @param loginUser        登录用户
     * @param friendAddRequest 好友添加请求
     */
    public void validateRequest(User loginUser, FriendAddRequest friendAddRequest) {
        if (StringUtils.isNotBlank(
                friendAddRequest.getRemark())
                &&
                friendAddRequest.getRemark().length() > FriendConstant.MAXIMUM_REMARK_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "申请备注最多120个字符");
        }
        if (ObjectUtils.anyNull(loginUser.getId(), friendAddRequest.getReceiveId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "添加失败");
        }
        // 1.添加的不能是自己
        if (Objects.equals(loginUser.getId(), friendAddRequest.getReceiveId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能添加自己为好友");
        }
    }

    /**
     * 获取好友申请记录
     *
     * @param loginUser 登录用户
     * @return {@link List}<{@link FriendsRecordVO}>
     */
    @Override
    public List<FriendsRecordVO> obtainFriendApplicationRecords(User loginUser) {
        // 查询出当前用户所有申请、同意记录
        LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        friendsLambdaQueryWrapper.eq(Friends::getReceiveId, loginUser.getId());
        return toFriendsVo(friendsLambdaQueryWrapper);
    }

    /**
     * 致朋友vo
     *
     * @param friendsLambdaQueryWrapper friends lambda查询包装器
     * @return {@link List}<{@link FriendsRecordVO}>
     */
    private List<FriendsRecordVO> toFriendsVo(LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper) {
        List<Friends> friendsList = this.list(friendsLambdaQueryWrapper);
        Collections.reverse(friendsList);
        return friendsList.stream().map(friend -> {
            FriendsRecordVO friendsRecordVO = new FriendsRecordVO();
            BeanUtils.copyProperties(friend, friendsRecordVO);
            User user = userService.getById(friend.getFromId());
            friendsRecordVO.setApplyUser(userService.getSafetyUser(user));
            return friendsRecordVO;
        }).collect(Collectors.toList());
    }

    /**
     * 获取我记录
     *
     * @param loginUser 登录用户
     * @return {@link List}<{@link FriendsRecordVO}>
     */
    @Override
    public List<FriendsRecordVO> getMyRecords(User loginUser) {
        // 查询出当前用户所有申请、同意记录
        LambdaQueryWrapper<Friends> myApplyLambdaQueryWrapper = new LambdaQueryWrapper<>();
        myApplyLambdaQueryWrapper.eq(Friends::getFromId, loginUser.getId());
        List<Friends> friendsList = this.list(myApplyLambdaQueryWrapper);
        Collections.reverse(friendsList);
        return friendsList.stream().map(friend -> {
            FriendsRecordVO friendsRecordVO = new FriendsRecordVO();
            BeanUtils.copyProperties(friend, friendsRecordVO);
            User user = userService.getById(friend.getReceiveId());
            friendsRecordVO.setApplyUser(userService.getSafetyUser(user));
            return friendsRecordVO;
        }).collect(Collectors.toList());
    }

    /**
     * 获取记录计数
     *
     * @param loginUser 登录用户
     * @return int
     */
    @Override
    public int getRecordCount(User loginUser) {
        LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        friendsLambdaQueryWrapper.eq(Friends::getReceiveId, loginUser.getId());
        List<Friends> friendsList = this.list(friendsLambdaQueryWrapper);
        int count = 0;
        for (Friends friend : friendsList) {
            if (friend.getStatus() == FriendConstant.DEFAULT_STATUS && friend.getIsRead() == FriendConstant.NOT_READ) {
                count++;
            }
        }
        return count;
    }

    /**
     * 阅读
     *
     * @param loginUser 登录用户
     * @param ids       ids
     * @return boolean
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toRead(User loginUser, Set<Long> ids) {
        boolean flag = false;
        for (Long id : ids) {
            Friends friend = this.getById(id);
            if (friend.getStatus() == FriendConstant.DEFAULT_STATUS && friend.getIsRead() == FriendConstant.NOT_READ) {
                friend.setIsRead(FriendConstant.READ);
                flag = this.updateById(friend);
            }
        }
        return flag;
    }

    /**
     * 同意申请
     *
     * @param loginUser 登录用户
     * @param fromId    从…起id
     * @return boolean
     */
    @Override
    public boolean agreeToApply(User loginUser, Long fromId) {
        // 0. 根据receiveId查询所有接收的申请记录
        LambdaQueryWrapper<Friends> friendsLambdaQueryWrapper = new LambdaQueryWrapper<>();
        friendsLambdaQueryWrapper.eq(Friends::getReceiveId, loginUser.getId());
        friendsLambdaQueryWrapper.eq(Friends::getFromId, fromId);
        List<Friends> recordCount = this.list(friendsLambdaQueryWrapper);
        List<Friends> collect = recordCount.stream()
                .filter(f -> f.getStatus() == FriendConstant.DEFAULT_STATUS)
                .collect(Collectors.toList());
        // 条数小于1 就不能再同意
        if (collect.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该申请不存在");
        }
        if (collect.size() > 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "操作有误,请重试");
        }
        AtomicBoolean flag = new AtomicBoolean(false);
        collect.forEach(friend -> {
            if (DateUtil.between(new Date(),
                    friend.getCreateTime(),
                    DateUnit.DAY) >= FriendConstant.MAXIMUM_APPLY_TIME || friend.getStatus() == FriendConstant.EXPIRED_STATUS) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该申请已过期");
            }
            // 1. 分别查询receiveId和fromId的用户，更改userIds中的数据
            User receiveUser = userService.getById(loginUser.getId());
            User fromUser = userService.getById(fromId);
            Set<Long> receiveUserIds = asia.lhweb.findfriend.utils.StringUtils.stringJsonListToLongSet(receiveUser.getFriendIds());
            Set<Long> fromUserUserIds = asia.lhweb.findfriend.utils.StringUtils.stringJsonListToLongSet(fromUser.getFriendIds());

            fromUserUserIds.add(receiveUser.getId());
            receiveUserIds.add(fromUser.getId());

            Gson gson = new Gson();
            String jsonFromUserUserIds = gson.toJson(fromUserUserIds);
            String jsonReceiveUserIds = gson.toJson(receiveUserIds);
            receiveUser.setFriendIds(jsonReceiveUserIds);
            fromUser.setFriendIds(jsonFromUserUserIds);
            // 2. 修改状态由0改为1
            friend.setStatus(FriendConstant.AGREE_STATUS);
            flag.set(userService.updateById(fromUser)
                    && userService.updateById(receiveUser)
                    && this.updateById(friend));
        });
        return flag.get();
    }

    /**
     * 取消申请
     *
     * @param id        id
     * @param loginUser 登录用户
     * @return boolean
     */
    @Override
    public boolean canceledApply(Long id, User loginUser) {
        Friends friend = this.getById(id);
        if (friend.getStatus() != FriendConstant.DEFAULT_STATUS) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该申请已过期或已通过");
        }
        friend.setStatus(FriendConstant.REVOKE_STATUS);
        return this.updateById(friend);
    }
}




