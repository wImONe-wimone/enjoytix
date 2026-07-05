package com.wimone.enjoytix.order.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimone.enjoytix.order.dao.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
}
