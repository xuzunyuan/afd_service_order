package com.afd.order.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.common.mybatis.Page;
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
	public Page<ReturnOrder> getRetOrdersByUserId(long userId,Page<ReturnOrder> page) {
		 List<ReturnOrder> retOrders = this.retOrderMapper.getRetOrdersByUserIdPage(userId,page);
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
			 
			 if(sellers!=null&&sellers.size()>0){
				 Map<Integer,Seller> sellerMap = new HashMap<Integer,Seller>();
				 for(Seller seller : sellers){
					 sellerMap.put(seller.getSellerId(), seller);
				 }
				 for(ReturnOrder retOrder : retOrders){
					 retOrder.setSeller(sellerMap.get(retOrder.getSellerId().intValue()));
				 }
			 }
		 }
		 page.setResult(retOrders);
		 return page;
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
	public ReturnOrder getRetOrderByRetOrderId(Long retOrderId) {
		ReturnOrder returnOrder=this.retOrderMapper.selectByPrimaryKey(retOrderId.intValue());
		return returnOrder;
	}

	@Override
	public int cancelRetOrderById(Long retOrderId, Long uid) {
		ReturnOrder retOrder = this.retOrderMapper.getRetOrderByIdUid(retOrderId,uid);
		//只有等待确认状态下的才可以取消
		if(retOrder!=null&&OrderConstants.order_return_wait.equals(retOrder.getStatus())){
			Long itemId = retOrder.getRetOrderItems().get(0).getItemId();
			OrderItem oi = new OrderItem();
			oi.setOrderItemId(itemId);
			oi.setStatus(OrderConstants.ORDERITEM_STATUS_NORMAL);
			this.orderItemMapper.updateByPrimaryKeySelective(oi);
			
			retOrder.setStatus(OrderConstants.order_return_cancel);
			retOrder.setCancelDate(DateUtils.currentDate());
			this.retOrderMapper.updateByPrimaryKeySelective(retOrder);
			return 1;
		}
		
		return 0;
	}

}
