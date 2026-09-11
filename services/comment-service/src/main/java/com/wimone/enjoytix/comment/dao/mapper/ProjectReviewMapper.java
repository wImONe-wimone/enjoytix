package com.wimone.enjoytix.comment.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimone.enjoytix.comment.dao.entity.ProjectReviewDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectReviewMapper extends BaseMapper<ProjectReviewDO> {

    @Select("""
            SELECT *
            FROM et_project_review
            WHERE id = #{reviewId}
            LIMIT 1
            """)
    ProjectReviewDO selectByIdIncludingDeleted(@Param("reviewId") Long reviewId);

    @Select("""
            SELECT *
            FROM et_project_review
            WHERE performance_id = #{performanceId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    ProjectReviewDO selectByPerformanceAndUserIncludingDeleted(
            @Param("performanceId") Long performanceId,
            @Param("userId") Long userId);

    @Update("""
            UPDATE et_project_review
            SET performance_id = #{performanceId},
                user_id = #{userId},
                order_id = #{orderId},
                rating = #{rating},
                content = #{content},
                edit_count = #{editCount},
                create_time = #{createTime},
                update_time = #{updateTime},
                del_flag = #{delFlag}
            WHERE id = #{id}
            """)
    int updateIncludingDeleted(ProjectReviewDO reviewDO);
}
