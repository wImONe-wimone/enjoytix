package com.wimone.enjoytix.comment.repository;

import com.wimone.enjoytix.comment.dao.entity.ProjectChatMessageDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectChatReplyDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectRatingSummaryDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectReviewDO;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {

    void saveChatMessage(ProjectChatMessageDO messageDO);

    Optional<ProjectChatMessageDO> findChatMessage(Long messageId);

    List<ProjectChatMessageDO> listChatMessages(Long performanceId, Long cursorId, int limit);

    void saveChatReply(ProjectChatReplyDO replyDO);

    Optional<ProjectChatReplyDO> findChatReply(Long replyId);

    Optional<ProjectChatReplyDO> findChatReplyForUpdate(Long replyId);

    List<ProjectChatReplyDO> listChatReplies(Long messageId, Long cursorId, int limit);

    long countChatReplies(Long messageId);

    void saveReview(ProjectReviewDO reviewDO);

    Optional<ProjectReviewDO> findReview(Long reviewId);

    Optional<ProjectReviewDO> findReviewForUpdate(Long reviewId);

    Optional<ProjectReviewDO> findActiveReviewByPerformanceAndUser(Long performanceId, Long userId);

    Optional<ProjectReviewDO> findReviewByPerformanceAndUserIncludingDeleted(Long performanceId, Long userId);

    List<ProjectReviewDO> pageReviews(Long performanceId, Integer rating, long offset, long size);

    long countReviews(Long performanceId, Integer rating);

    void saveRatingSummary(ProjectRatingSummaryDO summaryDO);

    Optional<ProjectRatingSummaryDO> findRatingSummary(Long performanceId);

    Optional<ProjectRatingSummaryDO> findRatingSummaryForUpdate(Long performanceId);
}
