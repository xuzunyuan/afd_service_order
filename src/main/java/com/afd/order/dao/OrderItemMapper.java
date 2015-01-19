package com.afd.order.dao;

import com.afd.model.order.OrderItem;

public interface OrderItemMapper {
    int deleteByPrimaryKey(Long orderItemId);

	int insert(OrderItem record);

	int insertSelective(OrderItem record);

	OrderItem selectByPrimaryKey(Long orderItemId);

	int updateByPrimaryKeySelective(OrderItem record);

	int updateByPrimaryKey(OrderItem record);

}