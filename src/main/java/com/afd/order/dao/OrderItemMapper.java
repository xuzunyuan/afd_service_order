package com.afd.order.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.afd.model.order.OrderItem;

public interface OrderItemMapper {
    int deleteByPrimaryKey(Long orderItemId);

	int insert(OrderItem record);

	int insertSelective(OrderItem record);

	OrderItem selectByPrimaryKey(Long orderItemId);

	int updateByPrimaryKeySelective(OrderItem record);

	int updateByPrimaryKey(OrderItem record);
	
	public List<OrderItem> getOrderItemsByOrderId(Integer orderId);
	
	public List<OrderItem> getOrderItemsByOrderIds(List<Long> orderIds);
	
	public List<OrderItem> getOrderItemsByItemIds(List<Long> orderItemIds);

	OrderItem getOrderItemById(@Param("orderItemId") Long orderItemId);

}