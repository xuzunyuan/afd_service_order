package com.afd.order.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.model.order.ReturnOrder;
import com.afd.order.dao.ReturnOrderMapper;
import com.afd.service.order.IRetOrderService;

@Service("retOrderService")
public class RetOrderServiceImpl implements IRetOrderService {
	@Autowired
	private ReturnOrderMapper retOrderMapper;

	@Override
	public List<ReturnOrder> getRetOrdersByUserId(long userId) {
		return this.retOrderMapper.getRetOrdersByUserId(userId);
	}

}
