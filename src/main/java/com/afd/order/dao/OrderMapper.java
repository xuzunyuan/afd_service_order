package com.afd.order.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.afd.common.mybatis.Page;
import com.afd.model.order.Order;

public interface OrderMapper {
    int deleteByPrimaryKey(Long orderId);

    int insert(Order record);

    int insertSelective(Order record);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);
    
    Order getOrderById(@Param("orderId") Long orderId);
    
    List<Order> getOrdersByIdsAndUserId(@Param("orderIds")Long[] orderIds, @Param("userId") Long userId);

	List<Order> getOrdersByUserId(@Param("userId")Long userId);
	
	public List<Order> queryOrderByCondition(@Param("cond") Map<String, ?> map, @Param("page") Page<Order> page);
	
	public List<Order> getOrdersByIds(@Param("orderIds")Long[] orderIds);
}