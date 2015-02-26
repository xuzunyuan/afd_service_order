package com.afd.order.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.common.util.DateUtils;
import com.afd.constants.order.OrderConstants;
import com.afd.model.order.OrderItem;
import com.afd.model.order.ReturnOrder;
import com.afd.model.order.ReturnOrderItem;
import com.afd.model.product.Sku;
import com.afd.model.seller.Seller;
import com.afd.order.dao.OrderItemMapper;
import com.afd.order.dao.ReturnOrderItemMapper;
import com.afd.order.dao.ReturnOrderMapper;
import com.afd.service.order.IRetOrderService;
import com.afd.service.product.IProductService;
import com.afd.service.seller.ISellerService;

@Service("retOrderService")
public class RetOrderServiceImpl implements IRetOrderService {
	@Autowired
	private ReturnOrderMapper retOrderMapper;
	@Autowired
	private ReturnOrderItemMapper retOrderItemMapper;
	@Autowired
	private OrderItemMapper orderItemMapper;
	@Autowired
	private IProductService prodService;
	@Autowired
	private ISellerService sellerService;

	@Override
	public List<ReturnOrder> getRetOrdersByUserId(long userId) {
		 List<ReturnOrder> retOrders = this.retOrderMapper.getRetOrdersByUserId(userId);
		 Set<Long> sellerIds = new HashSet<Long>();
		 Set<Integer> skuIds = new HashSet<Integer>();
		 if(retOrders!=null&&retOrders.size()>0){
			 for(ReturnOrder retOrder : retOrders){
				 sellerIds.add(retOrder.getSellerId());
				 List<ReturnOrderItem> retOrderItems = retOrder.getRetOrderItems();
				 if(retOrderItems!=null&&retOrderItems.size()>0){
					 for(ReturnOrderItem retOrderItem : retOrderItems){
						 skuIds.add(retOrderItem.getSkuId().intValue());
					 }
				 }
			 }
			 List<Sku> skus = this.prodService.getSkusBySkuIds(new ArrayList<Integer>(skuIds));
			 List<Seller> sellers = this.sellerService.getSellersByIds(sellerIds);
			 if(skus!=null&&skus.size()>0){
				 Map<Integer,Sku> skuMap = new HashMap<Integer,Sku>();
				 for(Sku sku : skus){
					 skuMap.put(sku.getSkuId(), sku);
				 }
				 
				 for(ReturnOrder retOrder : retOrders){
					 List<ReturnOrderItem> retOrderItems = retOrder.getRetOrderItems();
					 if(retOrderItems!=null&&retOrderItems.size()>0){
						 for(ReturnOrderItem retOrderItem : retOrderItems){
							 retOrderItem.setSku(skuMap.get(retOrderItem.getSkuId().intValue()));
						 }
					 }
				 }
			 }
		 }
		 
		 return retOrders;
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
	
	@Override
	public ReturnOrder getRetOrderByRetOrderId(Integer retOrderId) {
		ReturnOrder returnOrder=this.retOrderMapper.selectByPrimaryKey(retOrderId);
		return returnOrder;
	}

}
