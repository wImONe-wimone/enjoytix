package com.wimone.enjoytix.comment.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.wimone.enjoytix.framework.database.base.BaseDO;

import java.math.BigDecimal;

@TableName("et_project_rating_summary")
public class ProjectRatingSummaryDO extends BaseDO {

    private Long performanceId;
    private Integer reviewCount;
    private Integer ratingSum;
    private BigDecimal avgRating;
    @TableField("star_1_count")
    private Integer star1Count;
    @TableField("star_2_count")
    private Integer star2Count;
    @TableField("star_3_count")
    private Integer star3Count;
    @TableField("star_4_count")
    private Integer star4Count;
    @TableField("star_5_count")
    private Integer star5Count;

    public Long getPerformanceId() {
        return performanceId;
    }

    public void setPerformanceId(Long performanceId) {
        this.performanceId = performanceId;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getRatingSum() {
        return ratingSum;
    }

    public void setRatingSum(Integer ratingSum) {
        this.ratingSum = ratingSum;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Integer getStar1Count() {
        return star1Count;
    }

    public void setStar1Count(Integer star1Count) {
        this.star1Count = star1Count;
    }

    public Integer getStar2Count() {
        return star2Count;
    }

    public void setStar2Count(Integer star2Count) {
        this.star2Count = star2Count;
    }

    public Integer getStar3Count() {
        return star3Count;
    }

    public void setStar3Count(Integer star3Count) {
        this.star3Count = star3Count;
    }

    public Integer getStar4Count() {
        return star4Count;
    }

    public void setStar4Count(Integer star4Count) {
        this.star4Count = star4Count;
    }

    public Integer getStar5Count() {
        return star5Count;
    }

    public void setStar5Count(Integer star5Count) {
        this.star5Count = star5Count;
    }
}
