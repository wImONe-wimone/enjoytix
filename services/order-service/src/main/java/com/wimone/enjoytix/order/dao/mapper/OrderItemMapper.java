package com.wimone.enjoytix.order.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimone.enjoytix.order.dao.entity.OrderItemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {
}
