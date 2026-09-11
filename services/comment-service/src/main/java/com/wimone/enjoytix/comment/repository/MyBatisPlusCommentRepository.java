package com.wimone.enjoytix.comment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wimone.enjoytix.comment.dao.entity.ProjectChatMessageDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectChatReplyDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectRatingSummaryDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectReviewDO;
import com.wimone.enjoytix.comment.dao.mapper.ProjectChatMessageMapper;
import com.wimone.enjoytix.comment.dao.mapper.ProjectChatReplyMapper;
import com.wimone.enjoytix.comment.dao.mapper.ProjectRatingSummaryMapper;
import com.wimone.enjoytix.comment.dao.mapper.ProjectReviewMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisPlusCommentRepository implements CommentRepository {

    private final ProjectChatMessageMapper chatMessageMapper;
    private final ProjectChatReplyMapper chatReplyMapper;
    private final ProjectReviewMapper reviewMapper;
    private final ProjectRatingSummaryMapper ratingSummaryMapper;

    public MyBatisPlusCommentRepository(
            ProjectChatMessageMapper chatMessageMapper,
            ProjectChatReplyMapper chatReplyMapper,
            ProjectReviewMapper reviewMapper,
            ProjectRatingSummaryMapper ratingSummaryMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.chatReplyMapper = chatReplyMapper;
        this.reviewMapper = reviewMapper;
        this.ratingSummaryMapper = ratingSummaryMapper;
    }

    @Override
    public void saveChatMessage(ProjectChatMessageDO messageDO) {
        if (Integer.valueOf(1).equals(messageDO.getDelFlag())) {
            chatMessageMapper.deleteById(messageDO.getId());
            return;
        }
        if (messageDO.getId() == null || chatMessageMapper.selectById(messageDO.getId()) == null) {
            chatMessageMapper.insert(messageDO);
            return;
        }
        chatMessageMapper.updateById(messageDO);
    }

    @Override
    public Optional<ProjectChatMessageDO> findChatMessage(Long messageId) {
        return Optional.ofNullable(chatMessageMapper.selectById(messageId));
    }

    @Override
    public List<ProjectChatMessageDO> listChatMessages(Long performanceId, Long cursorId, int limit) {
        LambdaQueryWrapper<ProjectChatMessageDO> wrapper = Wrappers.lambdaQuery(ProjectChatMessageDO.class)
                .eq(ProjectChatMessageDO::getPerformanceId, performanceId)
                .orderByDesc(ProjectChatMessageDO::getId)
                .last("LIMIT " + Math.max(1, limit));
        if (cursorId != null) {
            wrapper.lt(ProjectChatMessageDO::getId, cursorId);
        }
        return chatMessageMapper.selectList(wrapper);
    }

    @Override
    public void saveChatReply(ProjectChatReplyDO replyDO) {
        if (Integer.valueOf(1).equals(replyDO.getDelFlag())) {
            chatReplyMapper.deleteById(replyDO.getId());
            return;
        }
        if (replyDO.getId() == null || chatReplyMapper.selectById(replyDO.getId()) == null) {
            chatReplyMapper.insert(replyDO);
            return;
        }
        chatReplyMapper.updateById(replyDO);
    }

    @Override
    public Optional<ProjectChatReplyDO> findChatReply(Long replyId) {
        return Optional.ofNullable(chatReplyMapper.selectById(replyId));
    }

    @Override
    public Optional<ProjectChatReplyDO> findChatReplyForUpdate(Long replyId) {
        return Optional.ofNullable(chatReplyMapper.selectOne(Wrappers.lambdaQuery(ProjectChatReplyDO.class)
                .eq(ProjectChatReplyDO::getId, replyId)
                .last("FOR UPDATE")));
    }

    @Override
    public List<ProjectChatReplyDO> listChatReplies(Long messageId, Long cursorId, int limit) {
        LambdaQueryWrapper<ProjectChatReplyDO> wrapper = Wrappers.lambdaQuery(ProjectChatReplyDO.class)
                .eq(ProjectChatReplyDO::getMessageId, messageId)
                .orderByAsc(ProjectChatReplyDO::getId)
                .last("LIMIT " + Math.max(1, limit));
        if (cursorId != null) {
            wrapper.gt(ProjectChatReplyDO::getId, cursorId);
        }
        return chatReplyMapper.selectList(wrapper);
    }

    @Override
    public long countChatReplies(Long messageId) {
        return chatReplyMapper.selectCount(Wrappers.lambdaQuery(ProjectChatReplyDO.class)
                .eq(ProjectChatReplyDO::getMessageId, messageId));
    }

    @Override
    public void saveReview(ProjectReviewDO reviewDO) {
        if (reviewDO.getId() == null || reviewMapper.selectByIdIncludingDeleted(reviewDO.getId()) == null) {
            reviewMapper.insert(reviewDO);
            return;
        }
        if (Integer.valueOf(1).equals(reviewDO.getDelFlag())) {
            reviewMapper.updateIncludingDeleted(reviewDO);
            return;
        }
        reviewMapper.updateIncludingDeleted(reviewDO);
    }

    @Override
    public Optional<ProjectReviewDO> findReview(Long reviewId) {
        return Optional.ofNullable(reviewMapper.selectById(reviewId));
    }

    @Override
    public Optional<ProjectReviewDO> findReviewForUpdate(Long reviewId) {
        return Optional.ofNullable(reviewMapper.selectOne(Wrappers.lambdaQuery(ProjectReviewDO.class)
                .eq(ProjectReviewDO::getId, reviewId)
                .last("FOR UPDATE")));
    }

    @Override
    public Optional<ProjectReviewDO> findActiveReviewByPerformanceAndUser(Long performanceId, Long userId) {
        return Optional.ofNullable(reviewMapper.selectOne(Wrappers.lambdaQuery(ProjectReviewDO.class)
                .eq(ProjectReviewDO::getPerformanceId, performanceId)
                .eq(ProjectReviewDO::getUserId, userId)
                .last("LIMIT 1")));
    }

    @Override
    public Optional<ProjectReviewDO> findReviewByPerformanceAndUserIncludingDeleted(Long performanceId, Long userId) {
        return Optional.ofNullable(reviewMapper.selectByPerformanceAndUserIncludingDeleted(performanceId, userId));
    }

    @Override
    public List<ProjectReviewDO> pageReviews(Long performanceId, Integer rating, long offset, long size) {
        LambdaQueryWrapper<ProjectReviewDO> wrapper = Wrappers.lambdaQuery(ProjectReviewDO.class)
                .eq(ProjectReviewDO::getPerformanceId, performanceId)
                .orderByDesc(ProjectReviewDO::getCreateTime)
                .orderByDesc(ProjectReviewDO::getId)
                .last("LIMIT " + Math.max(0, offset) + "," + Math.max(1, size));
        if (rating != null) {
            wrapper.eq(ProjectReviewDO::getRating, rating);
        }
        return reviewMapper.selectList(wrapper);
    }

    @Override
    public long countReviews(Long performanceId, Integer rating) {
        LambdaQueryWrapper<ProjectReviewDO> wrapper = Wrappers.lambdaQuery(ProjectReviewDO.class)
                .eq(ProjectReviewDO::getPerformanceId, performanceId);
        if (rating != null) {
            wrapper.eq(ProjectReviewDO::getRating, rating);
        }
        return reviewMapper.selectCount(wrapper);
    }

    @Override
    public void saveRatingSummary(ProjectRatingSummaryDO summaryDO) {
        if (summaryDO.getId() != null && ratingSummaryMapper.selectById(summaryDO.getId()) != null) {
            ratingSummaryMapper.updateById(summaryDO);
            return;
        }
        Optional<ProjectRatingSummaryDO> existing = findRatingSummary(summaryDO.getPerformanceId());
        if (existing.isPresent()) {
            summaryDO.setId(existing.get().getId());
            ratingSummaryMapper.updateById(summaryDO);
            return;
        }
        try {
            ratingSummaryMapper.insert(summaryDO);
        } catch (DuplicateKeyException ignored) {
            findRatingSummary(summaryDO.getPerformanceId()).ifPresent(existingSummary -> {
                summaryDO.setId(existingSummary.getId());
                ratingSummaryMapper.updateById(summaryDO);
            });
        }
    }

    @Override
    public Optional<ProjectRatingSummaryDO> findRatingSummary(Long performanceId) {
        return Optional.ofNullable(ratingSummaryMapper.selectOne(Wrappers.lambdaQuery(ProjectRatingSummaryDO.class)
                .eq(ProjectRatingSummaryDO::getPerformanceId, performanceId)
                .last("LIMIT 1")));
    }

    @Override
    public Optional<ProjectRatingSummaryDO> findRatingSummaryForUpdate(Long performanceId) {
        return Optional.ofNullable(ratingSummaryMapper.selectByPerformanceForUpdate(performanceId));
    }
}
