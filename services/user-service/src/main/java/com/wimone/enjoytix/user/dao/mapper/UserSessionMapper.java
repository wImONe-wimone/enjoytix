package com.wimone.enjoytix.user.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimone.enjoytix.user.dao.entity.UserSessionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSessionDO> {
}
