package com.afd.order.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.common.util.DateUtils;
import com.afd.model.order.ReturnOrder;
import com.afd.model.order.ReturnOrderItem;
import com.afd.order.dao.ReturnOrderItemMapper;
import com.afd.order.dao.ReturnOrderMapper;
import com.afd.service.order.IRetOrderService;

@Service("retOrderService")
public class RetOrderServiceImpl implements IRetOrderService {
	@Autowired
	private ReturnOrderMapper retOrderMapper;
	@Autowired
	private ReturnOrderItemMapper retOrderItemMapper;

	@Override
	public List<ReturnOrder> getRetOrdersByUserId(long userId) {
		return this.retOrderMapper.getRetOrdersByUserId(userId);
	}

	@Override
	public int addRetOrder(ReturnOrder retOrder) {
		if(retOrder!=null){
			retOrder.setCreateDate(DateUtils.currentDate());
			this.retOrderMapper.insertSelective(retOrder);
			List<ReturnOrderItem> retOrderItems = retOrder.getRetOrderItems();
			if(retOrderItems!=null&&retOrderItems.size()>0){
				for(ReturnOrderItem roi : retOrderItems){
					roi.setRetOrderId(retOrder.getRetOrderId());
					this.retOrderItemMapper.insertSelective(roi);
				}
				return 1;
			}
		}
		return 0;
	}

}
