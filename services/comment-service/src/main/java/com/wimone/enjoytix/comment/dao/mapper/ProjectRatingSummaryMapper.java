package com.wimone.enjoytix.comment.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimone.enjoytix.comment.dao.entity.ProjectRatingSummaryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectRatingSummaryMapper extends BaseMapper<ProjectRatingSummaryDO> {

    @Select("""
            SELECT *
            FROM et_project_rating_summary
            WHERE performance_id = #{performanceId}
              AND del_flag = 0
            LIMIT 1
            FOR UPDATE
            """)
    ProjectRatingSummaryDO selectByPerformanceForUpdate(@Param("performanceId") Long performanceId);
}
