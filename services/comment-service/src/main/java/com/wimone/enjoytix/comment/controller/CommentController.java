package com.wimone.enjoytix.comment.controller;

import com.wimone.enjoytix.comment.common.CommentConstants;
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
import com.wimone.enjoytix.comment.service.CommentService;
import com.wimone.enjoytix.framework.convention.page.PageResponse;
import com.wimone.enjoytix.framework.convention.result.Result;
import com.wimone.enjoytix.framework.log.annotation.OperationLog;
import com.wimone.enjoytix.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/comment")
@Tag(name = "Comment API", description = "Project chat messages, purchased-user reviews, and rating summary APIs.")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "List project chat messages", description = "List chat messages under a performance by cursor.")
    @GetMapping("/performances/{performanceId}/messages")
    public Result<CursorPageRespDTO<ChatMessageRespDTO>> listMessages(
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        return Results.success(commentService.listMessages(performanceId, cursor, size));
    }

    @OperationLog("comment-message-create")
    @Operation(summary = "Create project chat message", description = "Create a normal chat message under a performance.")
    @PostMapping("/performances/{performanceId}/messages")
    public Result<ChatMessageRespDTO> createMessage(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Valid @RequestBody ChatMessageCreateReqDTO requestParam) {
        return Results.success(commentService.createMessage(userId, performanceId, requestParam));
    }

    @OperationLog("comment-message-update")
    @Operation(summary = "Update project chat message", description = "Update a chat message owned by current user.")
    @PutMapping("/messages/{messageId}")
    public Result<ChatMessageRespDTO> updateMessage(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Message id.", required = true)
            @PathVariable Long messageId,
            @Valid @RequestBody ChatMessageUpdateReqDTO requestParam) {
        return Results.success(commentService.updateMessage(userId, messageId, requestParam));
    }

    @OperationLog("comment-message-delete")
    @Operation(summary = "Delete project chat message", description = "Soft delete a chat message owned by current user.")
    @DeleteMapping("/messages/{messageId}")
    public Result<Boolean> deleteMessage(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Message id.", required = true)
            @PathVariable Long messageId) {
        return Results.success(commentService.deleteMessage(userId, messageId));
    }

    @Operation(summary = "List chat message replies", description = "List replies under a first-level chat message by cursor.")
    @GetMapping("/messages/{messageId}/replies")
    public Result<CursorPageRespDTO<ChatReplyRespDTO>> listReplies(
            @Parameter(description = "Message id.", required = true)
            @PathVariable Long messageId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        return Results.success(commentService.listReplies(messageId, cursor, size));
    }

    @OperationLog("comment-reply-create")
    @Operation(summary = "Create chat message reply", description = "Create a reply under a first-level chat message.")
    @PostMapping("/messages/{messageId}/replies")
    public Result<ChatReplyRespDTO> createReply(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Message id.", required = true)
            @PathVariable Long messageId,
            @Valid @RequestBody ChatReplyCreateReqDTO requestParam) {
        return Results.success(commentService.createReply(userId, messageId, requestParam));
    }

    @OperationLog("comment-reply-update")
    @Operation(summary = "Update chat message reply", description = "Update a reply owned by current user.")
    @PutMapping("/replies/{replyId}")
    public Result<ChatReplyRespDTO> updateReply(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Reply id.", required = true)
            @PathVariable Long replyId,
            @Valid @RequestBody ChatReplyUpdateReqDTO requestParam) {
        return Results.success(commentService.updateReply(userId, replyId, requestParam));
    }

    @OperationLog("comment-reply-delete")
    @Operation(summary = "Delete chat message reply", description = "Soft delete a reply owned by current user.")
    @DeleteMapping("/replies/{replyId}")
    public Result<Boolean> deleteReply(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Reply id.", required = true)
            @PathVariable Long replyId) {
        return Results.success(commentService.deleteReply(userId, replyId));
    }

    @Operation(summary = "List project reviews", description = "List purchased-user reviews under a performance.")
    @GetMapping("/performances/{performanceId}/reviews")
    public Result<PageResponse<ProjectReviewRespDTO>> listReviews(
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Long current,
            @RequestParam(required = false) Long size) {
        return Results.success(commentService.listReviews(performanceId, rating, current, size));
    }

    @Operation(summary = "Get current user's project review", description = "Return current user's active review under a performance.")
    @GetMapping("/performances/{performanceId}/reviews/me")
    public Result<ProjectReviewRespDTO> myReview(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId) {
        return Results.success(commentService.myReview(userId, performanceId));
    }

    @OperationLog("comment-review-create")
    @Operation(summary = "Create project review", description = "Create a rating review under a performance. Current user must have purchased it.")
    @PostMapping("/performances/{performanceId}/reviews")
    public Result<ProjectReviewRespDTO> createReview(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId,
            @Valid @RequestBody ReviewCreateReqDTO requestParam) {
        return Results.success(commentService.createReview(userId, performanceId, requestParam));
    }

    @OperationLog("comment-review-update")
    @Operation(summary = "Update project review", description = "Update a review owned by current user. Rating is required on every update.")
    @PutMapping("/reviews/{reviewId}")
    public Result<ProjectReviewRespDTO> updateReview(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Review id.", required = true)
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateReqDTO requestParam) {
        return Results.success(commentService.updateReview(userId, reviewId, requestParam));
    }

    @OperationLog("comment-review-delete")
    @Operation(summary = "Delete project review", description = "Soft delete a review owned by current user and update rating summary.")
    @DeleteMapping("/reviews/{reviewId}")
    public Result<Boolean> deleteReview(
            @Parameter(description = "Current user id.", required = true)
            @RequestHeader(CommentConstants.USER_ID_HEADER) Long userId,
            @Parameter(description = "Review id.", required = true)
            @PathVariable Long reviewId) {
        return Results.success(commentService.deleteReview(userId, reviewId));
    }

    @Operation(summary = "Get project rating summary", description = "Get average rating and star distribution under a performance.")
    @GetMapping("/performances/{performanceId}/rating-summary")
    public Result<RatingSummaryRespDTO> ratingSummary(
            @Parameter(description = "Performance id.", required = true)
            @PathVariable Long performanceId) {
        return Results.success(commentService.ratingSummary(performanceId));
    }
}
