package com.afd.order.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.common.util.DateUtils;
import com.afd.constants.order.OrderConstants;
import com.afd.model.order.OrderItem;
import com.afd.model.order.ReturnOrder;
import com.afd.model.order.ReturnOrderItem;
import com.afd.order.dao.OrderItemMapper;
import com.afd.order.dao.ReturnOrderItemMapper;
import com.afd.order.dao.ReturnOrderMapper;
import com.afd.service.order.IRetOrderService;

@Service("retOrderService")
public class RetOrderServiceImpl implements IRetOrderService {
	@Autowired
	private ReturnOrderMapper retOrderMapper;
	@Autowired
	private ReturnOrderItemMapper retOrderItemMapper;
	@Autowired
	private OrderItemMapper orderItemMapper;

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
				OrderItem oi = new OrderItem();
				oi.setOrderItemId(retOrderItems.get(0).getItemId());
				oi.setStatus(OrderConstants.ORDERITEM_STATUS_RETURN_APPLY);
				this.orderItemMapper.updateByPrimaryKeySelective(oi);
				
				return 1;
			}
		}
		return 0;
	}

}
