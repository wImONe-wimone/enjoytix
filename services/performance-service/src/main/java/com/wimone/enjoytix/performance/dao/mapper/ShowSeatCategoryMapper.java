package com.wimone.enjoytix.performance.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimone.enjoytix.performance.dao.entity.ShowSeatCategoryDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShowSeatCategoryMapper extends BaseMapper<ShowSeatCategoryDO> {

    @Delete("DELETE FROM et_show_seat_category WHERE show_id = #{showId}")
    int deletePhysicallyByShowId(@Param("showId") Long showId);
}
