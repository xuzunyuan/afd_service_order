package com.afd.order.dao;

import com.afd.model.order.OrderItem;

public interface OrderItemMapper {
    int deleteByPrimaryKey(Integer paymentId);

    int insert(OrderItem record);

    int insertSelective(OrderItem record);

    OrderItem selectByPrimaryKey(Integer paymentId);

    int updateByPrimaryKeySelective(OrderItem record);

    int updateByPrimaryKey(OrderItem record);
}