package com.wimone.enjoytix.comment.service.impl;

import com.wimone.enjoytix.comment.dao.entity.ProjectChatMessageDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectChatReplyDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectRatingSummaryDO;
import com.wimone.enjoytix.comment.dao.entity.ProjectReviewDO;
import com.wimone.enjoytix.comment.dto.req.ChatMessageCreateReqDTO;
import com.wimone.enjoytix.comment.dto.req.ChatMessageUpdateReqDTO;
import com.wimone.enjoytix.comment.dto.req.ChatReplyCreateReqDTO;
import com.wimone.enjoytix.comment.dto.req.ChatReplyUpdateReqDTO;
import com.wimone.enjoytix.comment.dto.req.ReviewCreateReqDTO;
import com.wimone.enjoytix.comment.dto.req.ReviewUpdateReqDTO;
import com.wimone.enjoytix.comment.dto.resp.ChatMessageRespDTO;
import com.wimone.enjoytix.comment.dto.resp.ChatReplyRespDTO;
import com.wimone.enjoytix.comment.dto.resp.CursorPageRespDTO;
import com.wimone.enjoytix.comment.dto.resp.ProjectReviewRespDTO;
import com.wimone.enjoytix.comment.dto.resp.RatingSummaryRespDTO;
import com.wimone.enjoytix.comment.remote.OrderRemoteService;
import com.wimone.enjoytix.comment.remote.PerformanceRemoteService;
import com.wimone.enjoytix.comment.remote.dto.OrderPurchaseCheckRespDTO;
import com.wimone.enjoytix.comment.repository.CommentRepository;
import com.wimone.enjoytix.comment.service.CommentService;
import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.exception.RemoteException;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_REVIEW_LENGTH = 2000;
    private static final int DEFAULT_CURSOR_PAGE_SIZE = 20;
    private static final int MAX_CURSOR_PAGE_SIZE = 100;
    private static final int DEFAULT_REPLY_PREVIEW_SIZE = 3;
    private static final long DEFAULT_PAGE_CURRENT = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final CommentRepository commentRepository;
    private final IdGeneratorManager idGeneratorManager;
    private final PerformanceRemoteService performanceRemoteService;
    private final OrderRemoteService orderRemoteService;

    public CommentServiceImpl(
            CommentRepository commentRepository,
            IdGeneratorManager idGeneratorManager,
            PerformanceRemoteService performanceRemoteService,
            OrderRemoteService orderRemoteService) {
        this.commentRepository = commentRepository;
        this.idGeneratorManager = idGeneratorManager;
        this.performanceRemoteService = performanceRemoteService;
        this.orderRemoteService = orderRemoteService;
    }

    @Override
    @Transactional
    public ChatMessageRespDTO createMessage(Long userId, Long performanceId, ChatMessageCreateReqDTO requestParam) {
        assertUser(userId);
        ensurePerformanceExists(performanceId);
        String content = normalizeContent(requestParam.content(), MAX_MESSAGE_LENGTH);
        ProjectChatMessageDO message = new ProjectChatMessageDO();
        message.setId(idGeneratorManager.nextId());
        message.setPerformanceId(performanceId);
        message.setUserId(userId);
        message.setContent(content);
        message.setEditCount(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        message.setDelFlag(0);
        commentRepository.saveChatMessage(message);
        return convertMessage(message);
    }

    @Override
    @Transactional
    public ChatMessageRespDTO updateMessage(Long userId, Long messageId, ChatMessageUpdateReqDTO requestParam) {
        assertUser(userId);
        ProjectChatMessageDO message = findMessage(messageId);
        assertOwner(userId, message.getUserId(), "Message does not belong to current user");
        message.setContent(normalizeContent(requestParam.content(), MAX_MESSAGE_LENGTH));
        message.setEditCount(nullToZero(message.getEditCount()) + 1);
        message.setUpdateTime(LocalDateTime.now());
        commentRepository.saveChatMessage(message);
        return convertMessage(message);
    }

    @Override
    @Transactional
    public Boolean deleteMessage(Long userId, Long messageId) {
        assertUser(userId);
        ProjectChatMessageDO message = findMessage(messageId);
        assertOwner(userId, message.getUserId(), "Message does not belong to current user");
        message.setDelFlag(1);
        message.setUpdateTime(LocalDateTime.now());
        commentRepository.saveChatMessage(message);
        return Boolean.TRUE;
    }

    @Override
    public CursorPageRespDTO<ChatMessageRespDTO> listMessages(Long performanceId, String cursor, Integer size) {
        ensurePerformanceExists(performanceId);
        int pageSize = normalizeCursorPageSize(size);
        Long cursorId = parseCursor(cursor);
        List<ProjectChatMessageDO> messages = commentRepository.listChatMessages(performanceId, cursorId, pageSize + 1);
        boolean hasMore = messages.size() > pageSize;
        List<ChatMessageRespDTO> records = messages.stream()
                .limit(pageSize)
                .map(this::convertMessage)
                .toList();
        String nextCursor = hasMore && !records.isEmpty()
                ? String.valueOf(records.get(records.size() - 1).messageId())
                : "";
        return new CursorPageRespDTO<>(nextCursor, hasMore, records);
    }

    @Override
    @Transactional
    public ChatReplyRespDTO createReply(Long userId, Long messageId, ChatReplyCreateReqDTO requestParam) {
        assertUser(userId);
        ProjectChatMessageDO message = findMessage(messageId);
        String content = normalizeContent(requestParam.content(), MAX_MESSAGE_LENGTH);
        ProjectChatReplyDO reply = new ProjectChatReplyDO();
        reply.setId(idGeneratorManager.nextId());
        reply.setMessageId(message.getId());
        reply.setPerformanceId(message.getPerformanceId());
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setEditCount(0);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        reply.setDelFlag(0);
        commentRepository.saveChatReply(reply);
        return convertReply(reply);
    }

    @Override
    @Transactional
    public ChatReplyRespDTO updateReply(Long userId, Long replyId, ChatReplyUpdateReqDTO requestParam) {
        assertUser(userId);
        ProjectChatReplyDO reply = findReplyForUpdate(replyId);
        assertOwner(userId, reply.getUserId(), "Reply does not belong to current user");
        reply.setContent(normalizeContent(requestParam.content(), MAX_MESSAGE_LENGTH));
        reply.setEditCount(nullToZero(reply.getEditCount()) + 1);
        reply.setUpdateTime(LocalDateTime.now());
        commentRepository.saveChatReply(reply);
        return convertReply(reply);
    }

    @Override
    @Transactional
    public Boolean deleteReply(Long userId, Long replyId) {
        assertUser(userId);
        ProjectChatReplyDO reply = findReplyForUpdate(replyId);
        assertOwner(userId, reply.getUserId(), "Reply does not belong to current user");
        reply.setDelFlag(1);
        reply.setUpdateTime(LocalDateTime.now());
        commentRepository.saveChatReply(reply);
        return Boolean.TRUE;
    }

    @Override
    public CursorPageRespDTO<ChatReplyRespDTO> listReplies(Long messageId, String cursor, Integer size) {
        findMessage(messageId);
        return replyPage(messageId, cursor, normalizeCursorPageSize(size));
    }

    @Override
    @Transactional
    public ProjectReviewRespDTO createReview(Long userId, Long performanceId, ReviewCreateReqDTO requestParam) {
        assertUser(userId);
        ensurePerformanceExists(performanceId);
        Integer rating = normalizeRating(requestParam.rating());
        String content = normalizeContent(requestParam.content(), MAX_REVIEW_LENGTH);
        OrderPurchaseCheckRespDTO purchase = ensurePurchased(userId, performanceId);
        ProjectReviewDO existing = commentRepository
                .findReviewByPerformanceAndUserIncludingDeleted(performanceId, userId)
                .orElse(null);
        if (existing != null && !Integer.valueOf(1).equals(existing.getDelFlag())) {
            throw new ClientException("Current user has already reviewed this performance");
        }
        ProjectReviewDO review = existing == null ? new ProjectReviewDO() : existing;
        if (review.getId() == null) {
            review.setId(idGeneratorManager.nextId());
        }
        review.setPerformanceId(performanceId);
        review.setUserId(userId);
        review.setOrderId(purchase.orderId());
        review.setRating(rating);
        review.setContent(content);
        review.setEditCount(existing == null ? 0 : nullToZero(existing.getEditCount()) + 1);
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        review.setDelFlag(0);
        commentRepository.saveReview(review);
        updateRatingSummary(performanceId, null, rating);
        return convertReview(review);
    }

    @Override
    @Transactional
    public ProjectReviewRespDTO updateReview(Long userId, Long reviewId, ReviewUpdateReqDTO requestParam) {
        assertUser(userId);
        ProjectReviewDO review = findReviewForUpdate(reviewId);
        assertOwner(userId, review.getUserId(), "Review does not belong to current user");
        OrderPurchaseCheckRespDTO purchase = ensurePurchased(userId, review.getPerformanceId());
        Integer oldRating = normalizeRating(review.getRating());
        Integer newRating = normalizeRating(requestParam.rating());
        review.setOrderId(purchase.orderId());
        review.setRating(newRating);
        review.setContent(normalizeContent(requestParam.content(), MAX_REVIEW_LENGTH));
        review.setEditCount(nullToZero(review.getEditCount()) + 1);
        review.setUpdateTime(LocalDateTime.now());
        commentRepository.saveReview(review);
        updateRatingSummary(review.getPerformanceId(), oldRating, newRating);
        return convertReview(review);
    }

    @Override
    @Transactional
    public Boolean deleteReview(Long userId, Long reviewId) {
        assertUser(userId);
        ProjectReviewDO review = findReviewForUpdate(reviewId);
        assertOwner(userId, review.getUserId(), "Review does not belong to current user");
        Integer oldRating = normalizeRating(review.getRating());
        review.setDelFlag(1);
        review.setUpdateTime(LocalDateTime.now());
        commentRepository.saveReview(review);
        updateRatingSummary(review.getPerformanceId(), oldRating, null);
        return Boolean.TRUE;
    }

    @Override
    public PageResponse<ProjectReviewRespDTO> listReviews(Long performanceId, Integer rating, Long current, Long size) {
        ensurePerformanceExists(performanceId);
        Integer ratingFilter = rating == null ? null : normalizeRating(rating);
        long normalizedCurrent = current == null ? DEFAULT_PAGE_CURRENT : Math.max(1, current);
        long normalizedSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        long total = commentRepository.countReviews(performanceId, ratingFilter);
        long offset = pageOffset(normalizedCurrent, normalizedSize);
        List<ProjectReviewRespDTO> records = offset >= total
                ? List.of()
                : commentRepository.pageReviews(performanceId, ratingFilter, offset, normalizedSize)
                .stream()
                .map(this::convertReview)
                .toList();
        return new PageResponse<>(normalizedCurrent, normalizedSize, total, records);
    }

    @Override
    public ProjectReviewRespDTO myReview(Long userId, Long performanceId) {
        assertUser(userId);
        ensurePerformanceExists(performanceId);
        return commentRepository.findActiveReviewByPerformanceAndUser(performanceId, userId)
                .map(this::convertReview)
                .orElse(null);
    }

    @Override
    public RatingSummaryRespDTO ratingSummary(Long performanceId) {
        ensurePerformanceExists(performanceId);
        return commentRepository.findRatingSummary(performanceId)
                .map(this::convertSummary)
                .orElseGet(() -> new RatingSummaryRespDTO(performanceId, 0, BigDecimal.ZERO.setScale(2), 0, 0, 0, 0, 0));
    }

    private void updateRatingSummary(Long performanceId, Integer oldRating, Integer newRating) {
        ProjectRatingSummaryDO summary = commentRepository.findRatingSummaryForUpdate(performanceId)
                .orElseGet(() -> newSummary(performanceId));
        int reviewCount = nullToZero(summary.getReviewCount());
        int ratingSum = nullToZero(summary.getRatingSum());
        if (oldRating == null && newRating != null) {
            reviewCount++;
            ratingSum += newRating;
            addStar(summary, newRating, 1);
        } else if (oldRating != null && newRating == null) {
            reviewCount = Math.max(0, reviewCount - 1);
            ratingSum = Math.max(0, ratingSum - oldRating);
            addStar(summary, oldRating, -1);
        } else if (oldRating != null && newRating != null && !Objects.equals(oldRating, newRating)) {
            ratingSum = ratingSum - oldRating + newRating;
            addStar(summary, oldRating, -1);
            addStar(summary, newRating, 1);
        }
        summary.setReviewCount(reviewCount);
        summary.setRatingSum(ratingSum);
        summary.setAvgRating(calculateAverage(ratingSum, reviewCount));
        summary.setUpdateTime(LocalDateTime.now());
        commentRepository.saveRatingSummary(summary);
    }

    private ProjectRatingSummaryDO newSummary(Long performanceId) {
        LocalDateTime now = LocalDateTime.now();
        ProjectRatingSummaryDO summary = new ProjectRatingSummaryDO();
        summary.setId(idGeneratorManager.nextId());
        summary.setPerformanceId(performanceId);
        summary.setReviewCount(0);
        summary.setRatingSum(0);
        summary.setAvgRating(BigDecimal.ZERO.setScale(2));
        summary.setStar1Count(0);
        summary.setStar2Count(0);
        summary.setStar3Count(0);
        summary.setStar4Count(0);
        summary.setStar5Count(0);
        summary.setCreateTime(now);
        summary.setUpdateTime(now);
        summary.setDelFlag(0);
        return summary;
    }

    private void addStar(ProjectRatingSummaryDO summary, Integer rating, int delta) {
        switch (rating) {
            case 1 -> summary.setStar1Count(Math.max(0, nullToZero(summary.getStar1Count()) + delta));
            case 2 -> summary.setStar2Count(Math.max(0, nullToZero(summary.getStar2Count()) + delta));
            case 3 -> summary.setStar3Count(Math.max(0, nullToZero(summary.getStar3Count()) + delta));
            case 4 -> summary.setStar4Count(Math.max(0, nullToZero(summary.getStar4Count()) + delta));
            case 5 -> summary.setStar5Count(Math.max(0, nullToZero(summary.getStar5Count()) + delta));
            default -> throw new ClientException("Rating must be between 1 and 5");
        }
    }

    private BigDecimal calculateAverage(int ratingSum, int reviewCount) {
        if (reviewCount <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(ratingSum)
                .divide(BigDecimal.valueOf(reviewCount), 2, RoundingMode.HALF_UP);
    }

    private void ensurePerformanceExists(Long performanceId) {
        if (performanceId == null) {
            throw new ClientException("Performance id is required");
        }
        var result = performanceRemoteService.detail(performanceId);
        if (!result.isSuccess() || result.getData() == null) {
            throw new RemoteException("Query performance failed: " + result.getMessage());
        }
    }

    private OrderPurchaseCheckRespDTO ensurePurchased(Long userId, Long performanceId) {
        var result = orderRemoteService.checkPurchase(userId, performanceId);
        if (!result.isSuccess() || result.getData() == null) {
            throw new RemoteException("Check purchase failed: " + result.getMessage());
        }
        OrderPurchaseCheckRespDTO purchase = result.getData();
        if (!Boolean.TRUE.equals(purchase.purchased())) {
            throw new ClientException("Only purchased users can review this performance");
        }
        return purchase;
    }

    private ProjectChatMessageDO findMessage(Long messageId) {
        if (messageId == null) {
            throw new ClientException("Message id is required");
        }
        return commentRepository.findChatMessage(messageId)
                .orElseThrow(() -> new ClientException("Message does not exist"));
    }

    private ProjectChatReplyDO findReplyForUpdate(Long replyId) {
        if (replyId == null) {
            throw new ClientException("Reply id is required");
        }
        return commentRepository.findChatReplyForUpdate(replyId)
                .orElseThrow(() -> new ClientException("Reply does not exist"));
    }

    private ProjectReviewDO findReviewForUpdate(Long reviewId) {
        if (reviewId == null) {
            throw new ClientException("Review id is required");
        }
        return commentRepository.findReviewForUpdate(reviewId)
                .orElseThrow(() -> new ClientException("Review does not exist"));
    }

    private void assertUser(Long userId) {
        if (userId == null) {
            throw new ClientException("Current user id is required");
        }
    }

    private void assertOwner(Long currentUserId, Long ownerId, String message) {
        if (!Objects.equals(currentUserId, ownerId)) {
            throw new ClientException(message);
        }
    }

    private String normalizeContent(String content, int maxLength) {
        if (content == null || content.isBlank()) {
            throw new ClientException("Content is required");
        }
        String normalized = content.trim();
        if (normalized.length() > maxLength) {
            throw new ClientException("Content length cannot exceed " + maxLength);
        }
        return normalized;
    }

    private Integer normalizeRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ClientException("Rating must be between 1 and 5");
        }
        return rating;
    }

    private int normalizeCursorPageSize(Integer size) {
        if (size == null) {
            return DEFAULT_CURSOR_PAGE_SIZE;
        }
        return Math.min(Math.max(1, size), MAX_CURSOR_PAGE_SIZE);
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(cursor);
        } catch (NumberFormatException ex) {
            throw new ClientException("Invalid cursor");
        }
    }

    private long pageOffset(long current, long size) {
        long pageIndex = current - 1;
        if (pageIndex > Long.MAX_VALUE / size) {
            return Long.MAX_VALUE;
        }
        return pageIndex * size;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private ChatMessageRespDTO convertMessage(ProjectChatMessageDO message) {
        CursorPageRespDTO<ChatReplyRespDTO> replies = replyPage(message.getId(), null, DEFAULT_REPLY_PREVIEW_SIZE);
        return new ChatMessageRespDTO(
                message.getId(),
                message.getPerformanceId(),
                message.getUserId(),
                message.getContent(),
                nullToZero(message.getEditCount()),
                commentRepository.countChatReplies(message.getId()),
                replies,
                message.getCreateTime(),
                message.getUpdateTime()
        );
    }

    private CursorPageRespDTO<ChatReplyRespDTO> replyPage(Long messageId, String cursor, int pageSize) {
        Long cursorId = parseCursor(cursor);
        List<ProjectChatReplyDO> replies = commentRepository.listChatReplies(messageId, cursorId, pageSize + 1);
        boolean hasMore = replies.size() > pageSize;
        List<ChatReplyRespDTO> records = replies.stream()
                .limit(pageSize)
                .map(this::convertReply)
                .toList();
        String nextCursor = hasMore && !records.isEmpty()
                ? String.valueOf(records.get(records.size() - 1).replyId())
                : "";
        return new CursorPageRespDTO<>(nextCursor, hasMore, records);
    }

    private ChatReplyRespDTO convertReply(ProjectChatReplyDO reply) {
        return new ChatReplyRespDTO(
                reply.getId(),
                reply.getMessageId(),
                reply.getPerformanceId(),
                reply.getUserId(),
                reply.getContent(),
                nullToZero(reply.getEditCount()),
                reply.getCreateTime(),
                reply.getUpdateTime()
        );
    }

    private ProjectReviewRespDTO convertReview(ProjectReviewDO review) {
        return new ProjectReviewRespDTO(
                review.getId(),
                review.getPerformanceId(),
                review.getUserId(),
                review.getOrderId(),
                review.getRating(),
                review.getContent(),
                nullToZero(review.getEditCount()),
                review.getCreateTime(),
                review.getUpdateTime()
        );
    }

    private RatingSummaryRespDTO convertSummary(ProjectRatingSummaryDO summary) {
        return new RatingSummaryRespDTO(
                summary.getPerformanceId(),
                nullToZero(summary.getReviewCount()),
                summary.getAvgRating() == null ? BigDecimal.ZERO.setScale(2) : summary.getAvgRating(),
                nullToZero(summary.getStar1Count()),
                nullToZero(summary.getStar2Count()),
                nullToZero(summary.getStar3Count()),
                nullToZero(summary.getStar4Count()),
                nullToZero(summary.getStar5Count())
        );
    }
}
