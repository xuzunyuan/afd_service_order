package com.afd.order.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.common.util.DateUtils;
import com.afd.model.order.Order;
import com.afd.model.order.OrderItem;
import com.afd.order.dao.OrderMapper;
import com.afd.service.order.IOrderService;

@Service("orderService")
public class OrderServiceImpl implements IOrderService{
	@Autowired
	private OrderMapper orderMapper;
	
	public int addOrder(){
		Order order = new Order();
		order.setUserName("liubo");
		order.setCreatedDate(DateUtils.currentDate());
		return orderMapper.insert(order);
	}

	@Override
	public Order getOrderById(Long orderId) {
		Order order = this.orderMapper.getOrderById(orderId);
		// TODO Auto-generated method stub
		return order;
	}

	@Override
	public List<Order> getOrdersByIdsAndUserIdNodelay(Long[] orderIds,
			Long userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int cancelOrderByBoss(List<Long> orderIds, String optName,
			String cancelReason) {
		// TODO Auto-generated method stub
		return 0;
	}
}
