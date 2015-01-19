package com.afd.order.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.afd.model.order.Order;

public interface OrderMapper {
    int deleteByPrimaryKey(Long orderId);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Long orderId);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);
    
    Order getOrderById(@Param("orderId") Long orderId);
    
    List<Order> getOrdersByIdsAndUserId(@Param("orderIds")Long[] orderIds, @Param("userId") Long userId);

	List<Order> getOrdersByUserId(@Param("userId")Long userId);
}