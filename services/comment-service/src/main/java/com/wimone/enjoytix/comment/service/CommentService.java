package com.wimone.enjoytix.comment.service;

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
import com.wimone.enjoytix.framework.convention.page.PageResponse;

public interface CommentService {

    ChatMessageRespDTO createMessage(Long userId, Long performanceId, ChatMessageCreateReqDTO requestParam);

    ChatMessageRespDTO updateMessage(Long userId, Long messageId, ChatMessageUpdateReqDTO requestParam);

    Boolean deleteMessage(Long userId, Long messageId);

    CursorPageRespDTO<ChatMessageRespDTO> listMessages(Long performanceId, String cursor, Integer size);

    ChatReplyRespDTO createReply(Long userId, Long messageId, ChatReplyCreateReqDTO requestParam);

    ChatReplyRespDTO updateReply(Long userId, Long replyId, ChatReplyUpdateReqDTO requestParam);

    Boolean deleteReply(Long userId, Long replyId);

    CursorPageRespDTO<ChatReplyRespDTO> listReplies(Long messageId, String cursor, Integer size);

    ProjectReviewRespDTO createReview(Long userId, Long performanceId, ReviewCreateReqDTO requestParam);

    ProjectReviewRespDTO updateReview(Long userId, Long reviewId, ReviewUpdateReqDTO requestParam);

    Boolean deleteReview(Long userId, Long reviewId);

    PageResponse<ProjectReviewRespDTO> listReviews(Long performanceId, Integer rating, Long current, Long size);

    ProjectReviewRespDTO myReview(Long userId, Long performanceId);

    RatingSummaryRespDTO ratingSummary(Long performanceId);
}
