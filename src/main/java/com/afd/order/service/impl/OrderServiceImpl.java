package com.afd.order.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.common.util.DateUtils;
import com.afd.model.order.Order;
import com.afd.order.dao.OrderMapper;

@Service("orderService")
public class OrderServiceImpl {
	@Autowired
	private OrderMapper orderMappder;
	
	public int addOrder(){
		Order order = new Order();
		order.setUserName("liubo");
		order.setCreatedDate(DateUtils.currentDate());
		return orderMappder.insert(order);
	}
}
