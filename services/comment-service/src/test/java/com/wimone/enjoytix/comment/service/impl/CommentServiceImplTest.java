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
import com.wimone.enjoytix.comment.dto.resp.CursorPageRespDTO;
import com.wimone.enjoytix.comment.dto.resp.ProjectReviewRespDTO;
import com.wimone.enjoytix.comment.dto.resp.RatingSummaryRespDTO;
import com.wimone.enjoytix.comment.remote.OrderRemoteService;
import com.wimone.enjoytix.comment.remote.PerformanceRemoteService;
import com.wimone.enjoytix.comment.remote.dto.OrderPurchaseCheckRespDTO;
import com.wimone.enjoytix.comment.remote.dto.PerformanceDetailRespDTO;
import com.wimone.enjoytix.comment.repository.CommentRepository;
import com.wimone.enjoytix.framework.convention.exception.ClientException;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import com.wimone.enjoytix.framework.distributedid.core.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentServiceImplTest {

    private FakeCommentRepository repository;
    private FakeOrderRemoteService orderRemoteService;
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        repository = new FakeCommentRepository();
        orderRemoteService = new FakeOrderRemoteService();
        commentService = new CommentServiceImpl(
                repository,
                new IdGeneratorManager(new SnowflakeIdGenerator(10)),
                new FakePerformanceRemoteService(),
                orderRemoteService
        );
    }

    @Test
    void chatMessagesShouldCreateUpdateDeleteAndList() {
        var created = commentService.createMessage(1L, 1001L, new ChatMessageCreateReqDTO("  hello  "));

        assertEquals("hello", created.content());
        assertEquals(1L, created.userId());

        var updated = commentService.updateMessage(1L, created.messageId(), new ChatMessageUpdateReqDTO("updated"));

        assertEquals("updated", updated.content());
        assertEquals(1, updated.editCount());

        CursorPageRespDTO<?> page = commentService.listMessages(1001L, null, 20);
        assertEquals(1, page.records().size());

        assertTrue(commentService.deleteMessage(1L, created.messageId()));
        assertEquals(0, commentService.listMessages(1001L, null, 20).records().size());
    }

    @Test
    void chatRepliesShouldCreateListPaginateUpdateAndDelete() {
        var message = commentService.createMessage(1L, 1001L, new ChatMessageCreateReqDTO("topic"));

        var first = commentService.createReply(2L, message.messageId(), new ChatReplyCreateReqDTO(" first "));
        commentService.createReply(3L, message.messageId(), new ChatReplyCreateReqDTO("second"));
        commentService.createReply(4L, message.messageId(), new ChatReplyCreateReqDTO("third"));
        commentService.createReply(5L, message.messageId(), new ChatReplyCreateReqDTO("fourth"));

        var messagePage = commentService.listMessages(1001L, null, 20);
        var listedMessage = messagePage.records().get(0);
        assertEquals(4L, listedMessage.replyCount());
        assertEquals(3, listedMessage.replies().records().size());
        assertTrue(listedMessage.replies().hasMore());

        var nextReplies = commentService.listReplies(message.messageId(), listedMessage.replies().nextCursor(), 2);
        assertEquals(1, nextReplies.records().size());
        assertEquals("fourth", nextReplies.records().get(0).content());

        var updated = commentService.updateReply(2L, first.replyId(), new ChatReplyUpdateReqDTO("updated"));
        assertEquals("updated", updated.content());
        assertEquals(1, updated.editCount());
        assertThrows(ClientException.class, () ->
                commentService.deleteReply(3L, first.replyId()));

        assertTrue(commentService.deleteReply(2L, first.replyId()));
        assertEquals(3L, commentService.listMessages(1001L, null, 20).records().get(0).replyCount());
    }

    @Test
    void reviewShouldRequirePurchase() {
        assertThrows(ClientException.class, () ->
                commentService.createReview(1L, 1001L, new ReviewCreateReqDTO(5, "great")));
    }

    @Test
    void reviewShouldCreateAndUpdateRatingSummary() {
        orderRemoteService.allowPurchase(1L, 1001L, 9001L);

        ProjectReviewRespDTO review = commentService.createReview(1L, 1001L, new ReviewCreateReqDTO(5, "great"));

        assertEquals(5, review.rating());
        RatingSummaryRespDTO summary = commentService.ratingSummary(1001L);
        assertEquals(1, summary.reviewCount());
        assertEquals(new BigDecimal("5.00"), summary.avgRating());
        assertEquals(1, summary.star5Count());

        ProjectReviewRespDTO updated = commentService.updateReview(1L, review.reviewId(), new ReviewUpdateReqDTO(3, "ok"));

        assertEquals(3, updated.rating());
        RatingSummaryRespDTO updatedSummary = commentService.ratingSummary(1001L);
        assertEquals(1, updatedSummary.reviewCount());
        assertEquals(new BigDecimal("3.00"), updatedSummary.avgRating());
        assertEquals(0, updatedSummary.star5Count());
        assertEquals(1, updatedSummary.star3Count());
    }

    @Test
    void duplicateActiveReviewShouldBeRejected() {
        orderRemoteService.allowPurchase(1L, 1001L, 9001L);
        commentService.createReview(1L, 1001L, new ReviewCreateReqDTO(5, "great"));

        assertThrows(ClientException.class, () ->
                commentService.createReview(1L, 1001L, new ReviewCreateReqDTO(4, "again")));
    }

    @Test
    void deleteReviewShouldRemoveItFromSummaryAndAllowRepublish() {
        orderRemoteService.allowPurchase(1L, 1001L, 9001L);
        ProjectReviewRespDTO review = commentService.createReview(1L, 1001L, new ReviewCreateReqDTO(4, "good"));

        assertTrue(commentService.deleteReview(1L, review.reviewId()));
        assertNull(commentService.myReview(1L, 1001L));
        RatingSummaryRespDTO emptySummary = commentService.ratingSummary(1001L);
        assertEquals(0, emptySummary.reviewCount());
        assertEquals(new BigDecimal("0.00"), emptySummary.avgRating());

        ProjectReviewRespDTO republished = commentService.createReview(1L, 1001L, new ReviewCreateReqDTO(5, "better"));

        assertEquals(review.reviewId(), republished.reviewId());
        assertEquals(5, republished.rating());
        assertEquals(1, commentService.ratingSummary(1001L).reviewCount());
    }

    private static final class FakePerformanceRemoteService implements PerformanceRemoteService {

        @Override
        public Result<PerformanceDetailRespDTO> detail(Long performanceId) {
            if (performanceId == null || performanceId <= 0) {
                return Result.success(null);
            }
            return Result.success(new PerformanceDetailRespDTO(performanceId));
        }
    }

    private static final class FakeOrderRemoteService implements OrderRemoteService {

        private final Map<String, Long> purchases = new HashMap<>();

        void allowPurchase(Long userId, Long performanceId, Long orderId) {
            purchases.put(userId + ":" + performanceId, orderId);
        }

        @Override
        public Result<OrderPurchaseCheckRespDTO> checkPurchase(Long userId, Long performanceId) {
            Long orderId = purchases.get(userId + ":" + performanceId);
            return Result.success(new OrderPurchaseCheckRespDTO(orderId != null, orderId, orderId == null ? null : LocalDateTime.now()));
        }
    }

    private static final class FakeCommentRepository implements CommentRepository {

        private final Map<Long, ProjectChatMessageDO> messages = new HashMap<>();
        private final Map<Long, ProjectChatReplyDO> replies = new HashMap<>();
        private final Map<Long, ProjectReviewDO> reviews = new HashMap<>();
        private final Map<Long, ProjectRatingSummaryDO> summaries = new HashMap<>();

        @Override
        public void saveChatMessage(ProjectChatMessageDO messageDO) {
            messages.put(messageDO.getId(), messageDO);
        }

        @Override
        public Optional<ProjectChatMessageDO> findChatMessage(Long messageId) {
            ProjectChatMessageDO message = messages.get(messageId);
            return message == null || Integer.valueOf(1).equals(message.getDelFlag())
                    ? Optional.empty()
                    : Optional.of(message);
        }

        @Override
        public List<ProjectChatMessageDO> listChatMessages(Long performanceId, Long cursorId, int limit) {
            return messages.values()
                    .stream()
                    .filter(each -> !Integer.valueOf(1).equals(each.getDelFlag()))
                    .filter(each -> Objects.equals(performanceId, each.getPerformanceId()))
                    .filter(each -> cursorId == null || each.getId() < cursorId)
                    .sorted(Comparator.comparing(ProjectChatMessageDO::getId).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public void saveChatReply(ProjectChatReplyDO replyDO) {
            replies.put(replyDO.getId(), replyDO);
        }

        @Override
        public Optional<ProjectChatReplyDO> findChatReply(Long replyId) {
            ProjectChatReplyDO reply = replies.get(replyId);
            return reply == null || Integer.valueOf(1).equals(reply.getDelFlag())
                    ? Optional.empty()
                    : Optional.of(reply);
        }

        @Override
        public Optional<ProjectChatReplyDO> findChatReplyForUpdate(Long replyId) {
            return findChatReply(replyId);
        }

        @Override
        public List<ProjectChatReplyDO> listChatReplies(Long messageId, Long cursorId, int limit) {
            return replies.values()
                    .stream()
                    .filter(each -> !Integer.valueOf(1).equals(each.getDelFlag()))
                    .filter(each -> Objects.equals(messageId, each.getMessageId()))
                    .filter(each -> cursorId == null || each.getId() > cursorId)
                    .sorted(Comparator.comparing(ProjectChatReplyDO::getId))
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countChatReplies(Long messageId) {
            return replies.values()
                    .stream()
                    .filter(each -> !Integer.valueOf(1).equals(each.getDelFlag()))
                    .filter(each -> Objects.equals(messageId, each.getMessageId()))
                    .count();
        }

        @Override
        public void saveReview(ProjectReviewDO reviewDO) {
            reviews.put(reviewDO.getId(), reviewDO);
        }

        @Override
        public Optional<ProjectReviewDO> findReview(Long reviewId) {
            ProjectReviewDO review = reviews.get(reviewId);
            return review == null || Integer.valueOf(1).equals(review.getDelFlag())
                    ? Optional.empty()
                    : Optional.of(review);
        }

        @Override
        public Optional<ProjectReviewDO> findReviewForUpdate(Long reviewId) {
            return findReview(reviewId);
        }

        @Override
        public Optional<ProjectReviewDO> findActiveReviewByPerformanceAndUser(Long performanceId, Long userId) {
            return reviews.values()
                    .stream()
                    .filter(each -> !Integer.valueOf(1).equals(each.getDelFlag()))
                    .filter(each -> Objects.equals(performanceId, each.getPerformanceId()))
                    .filter(each -> Objects.equals(userId, each.getUserId()))
                    .findFirst();
        }

        @Override
        public Optional<ProjectReviewDO> findReviewByPerformanceAndUserIncludingDeleted(Long performanceId, Long userId) {
            return reviews.values()
                    .stream()
                    .filter(each -> Objects.equals(performanceId, each.getPerformanceId()))
                    .filter(each -> Objects.equals(userId, each.getUserId()))
                    .findFirst();
        }

        @Override
        public List<ProjectReviewDO> pageReviews(Long performanceId, Integer rating, long offset, long size) {
            return reviews.values()
                    .stream()
                    .filter(each -> !Integer.valueOf(1).equals(each.getDelFlag()))
                    .filter(each -> Objects.equals(performanceId, each.getPerformanceId()))
                    .filter(each -> rating == null || Objects.equals(rating, each.getRating()))
                    .sorted(Comparator.comparing(ProjectReviewDO::getCreateTime).reversed()
                            .thenComparing(Comparator.comparing(ProjectReviewDO::getId).reversed()))
                    .skip(offset)
                    .limit(size)
                    .toList();
        }

        @Override
        public long countReviews(Long performanceId, Integer rating) {
            return reviews.values()
                    .stream()
                    .filter(each -> !Integer.valueOf(1).equals(each.getDelFlag()))
                    .filter(each -> Objects.equals(performanceId, each.getPerformanceId()))
                    .filter(each -> rating == null || Objects.equals(rating, each.getRating()))
                    .count();
        }

        @Override
        public void saveRatingSummary(ProjectRatingSummaryDO summaryDO) {
            summaries.put(summaryDO.getPerformanceId(), summaryDO);
        }

        @Override
        public Optional<ProjectRatingSummaryDO> findRatingSummary(Long performanceId) {
            return Optional.ofNullable(summaries.get(performanceId));
        }

        @Override
        public Optional<ProjectRatingSummaryDO> findRatingSummaryForUpdate(Long performanceId) {
            return findRatingSummary(performanceId);
        }
    }
}
