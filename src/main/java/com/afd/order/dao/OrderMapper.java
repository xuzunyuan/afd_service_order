package com.afd.order.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.afd.common.mybatis.Page;
import com.afd.model.order.Order;
import com.afd.param.order.OrderCondition;

public interface OrderMapper {
    int deleteByPrimaryKey(Long orderId);

    int insert(Order record);

    int insertSelective(Order record);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);
    
    Order getOrderById(@Param("orderId") Long orderId);
    
    List<Order> getOrdersByIdsAndUserId(@Param("orderIds")Long[] orderIds, @Param("userId") Long userId);

	List<Order> getOrdersByUserIdByPage(@Param("userId")Long userId, @Param("page") Page<Order> page);
	
	public List<Order> queryOrderByPage(@Param("cond") Map<String, ?> map, @Param("page") Page<Order> page);
	
	public List<Order> getOrdersByConditionPage(@Param("cond") OrderCondition cond, @Param("page") Page<Order> page);
	
	public List<Order> getOrdersByIds(@Param("orderIds")Long[] orderIds);

	public List<Order> getOrdersByUserIdAndStatusByPage(@Param("userId") Long userId,@Param("status") String status, @Param("page") Page<Order> page);
}