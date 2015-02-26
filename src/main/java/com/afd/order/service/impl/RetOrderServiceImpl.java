package com.afd.order.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.common.util.DateUtils;
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

	@Override
	public int addRetOrder(ReturnOrder retOrder) {
		if(retOrder!=null){
			retOrder.setCreateDate(DateUtils.currentDate());
			this.retOrderMapper.insertSelective(retOrder);
		}
		return 0;
	}
	
	@Override
	public ReturnOrder getRetOrderByRetOrderId(Integer retOrderId) {
		ReturnOrder returnOrder=this.retOrderMapper.selectByPrimaryKey(retOrderId);
		return returnOrder;
	}

}
