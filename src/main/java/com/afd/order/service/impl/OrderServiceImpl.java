package com.afd.order.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.afd.common.util.DateUtils;
import com.afd.model.order.Order;
import com.afd.model.user.User;
import com.afd.model.user.UserAddress;
import com.afd.order.dao.OrderItemMapper;
import com.afd.order.dao.OrderLogMapper;
import com.afd.order.dao.OrderMapper;
import com.afd.param.cart.Trade;
import com.afd.param.order.OrderInfo;
import com.afd.service.order.IOrderService;
import com.afd.service.product.IProductService;
import com.afd.service.user.IAddressService;
import com.afd.service.user.IUserService;
import com.afd.constants.order.OrderConstants;
import com.afd.model.order.OrderItem;
import com.afd.param.cart.TradeItem;
import com.afd.model.order.OrderLog;

@Service("orderService")
public class OrderServiceImpl implements IOrderService {
	@Autowired
	private OrderMapper orderMapper;
	@Autowired
	private OrderItemMapper orderItemMapper;
	@Autowired
	private OrderLogMapper orderLogMapper;
	
	@Autowired 
	private IProductService productService;
	@Autowired
	private IAddressService addressService;
	@Autowired
	private IUserService userService;
	
	@Autowired
	@Qualifier("redisNumber")
	private RedisTemplate<String, Map<Long,Long>> redisStock;

	@Autowired
	@Qualifier("redisNumber")
	private RedisTemplate<String, String> redisNumber;

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
	public List<Order> getOrdersByIdsAndUserId(Long[] orderIds,
			Long userId) {
		List<Order> orders = this.orderMapper.getOrdersByIdsAndUserId(orderIds, userId);
		return orders;
	}

	@Override
	public int cancelOrderByBoss(List<Long> orderIds, String optName,
			String cancelReason) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Order> getOrdersByUserId(Long userId) {
		return this.orderMapper.getOrdersByUserId(userId);
	}

	@Override
	public List<OrderInfo> batchCreateOrders(List<Trade> trades) {
		List<OrderInfo> orderInfos = new ArrayList<OrderInfo>();
		for(Trade trade : trades) {
			OrderInfo orderInfo = this.saveOrder(trade);
			orderInfos.add(orderInfo);
		}
		return orderInfos;
	}
	
	private OrderInfo saveOrder(Trade trade) {
		OrderInfo orderInfo = new OrderInfo();
		UserAddress addr = this.addressService.getAddressById(trade.getAddressId());
		User user = this.userService.getUserById(trade.getUserId());
		if(null == addr) {
			orderInfo.setCode(-1);
			return orderInfo;
		}
		if(null == user) {
			orderInfo.setCode(-2);
			return orderInfo;
		}
		Order order = this.createOrder(trade, addr, user);
		this.orderMapper.insert(order);
		Map<Long,Long> stockMapMq = new HashMap<Long,Long>();
		Map<Long,Long> brandShowStockMapMq = new HashMap<Long,Long>();
		List<OrderItem> orderItems = this.createOrderItems(trade.getTradeItems(), order, stockMapMq, brandShowStockMapMq);
//		order.setOrderItems(orderItems);
		order.setOrderFee(order.getProdFee().add(order.getDeliverFee()));
		if(order.getOrderFee().compareTo(BigDecimal.ZERO)<=0){
			order.setOrderFee(BigDecimal.ZERO);
			order.setOrderStatus(OrderConstants.ORDER_STATUS_WAITDELIVERED);
		}
		
		this.orderMapper.updateByPrimaryKeySelective(order);
		this.createOrderLog(order,trade);
		
		this.updateRedisInventory(stockMapMq,brandShowStockMapMq,trade.getUserId());
		redisNumber.convertAndSend("stock", stockMapMq);
		redisNumber.convertAndSend("brandShowStock", brandShowStockMapMq);
		orderInfo.setCode(1);
		orderInfo.setOrder(order);
		return orderInfo;
	}
	
	private void updateRedisInventory(Map<Long, Long> stockMapMq, Map<Long, Long> brandShowStockMapMq, Long userId) {
		// TODO Auto-generated method stub
		
	}

	private Order createOrder(Trade trade, UserAddress addr, User user) {
		Order order = new Order();
		Date now = new Date();
		order.setCreatedByName(trade.getOptName());
		order.setCreatedDate(now);
		order.setDeliverFee(trade.getDeliverFee()!=null?trade.getDeliverFee():BigDecimal.ZERO);
		order.setDeliverDiscount(trade.getDeliverDiscountFee()!=null?trade.getDeliverDiscountFee():BigDecimal.ZERO);
		order.setLastUpdateByName(trade.getOptName());
		order.setLastUpdateDate(now);
		order.setOrderSource(trade.getOrderSource());
		order.setOrderType(trade.getOrderType());
		order.setPayType(trade.getPayType());
		order.setPayMode(trade.getPayMode());
		order.setPayStatus(OrderConstants.PAY_STATUS_UNPAY);
		//货到付款
		if(order.getPayType().equals(OrderConstants.PAY_TYPE_COD)){
			//等待发货
			order.setOrderStatus(OrderConstants.ORDER_STATUS_WAITDELIVERED);
		}
		//在线支付
		else if(order.getPayType().equals(OrderConstants.PAY_TYPE_ONLINE)){
			//等待付款
			order.setOrderStatus(OrderConstants.ORDER_STATUS_WAITPAYMENT);
		}
		order.setUserRemark(trade.getUserRemark());
		order.setSignedStatus(OrderConstants.SIGNED_STATUS_UNSIGNED);
		order.setSellerId(trade.getSellerId());
		order.setLogisticsCompa(null);
		order.setLogisticsName(null);
		order.setOrderCode(null);
		order.setOrderId(null);
		order.setProdFee(null);
		order.setOrderFee(null);

		order.setrAddr(addr.getAddr());
		order.setrCity(addr.getCityName());
		order.setrCounty(addr.getDistrictName());
		order.setrEmail(user.getEmail());
		order.setrMobile(addr.getMobile());
		order.setrName(addr.getReceiver());
		order.setrPhone(addr.getTel());
		order.setrProvince(addr.getProvinceName());
		order.setrTown(addr.getTownName());
		order.setrZipcode(addr.getZipCode());
		
		order.setUserId(user.getUserId());
		order.setUserName(user.getUserName());
		
		return order;
	}
	
	private List<OrderItem> createOrderItems(List<TradeItem> tradeItems,Order order, Map<Long, Long> stockMapMq, Map<Long, Long> brandShowStockMapMq) {
		List<OrderItem> orderItems = new ArrayList<OrderItem>();
		if(tradeItems != null && tradeItems.size() > 0){
			BigDecimal prodFee = BigDecimal.ZERO;
			BigDecimal discountFee = BigDecimal.ZERO;
			for(TradeItem tradeItem : tradeItems){
				OrderItem orderItem = this.createOrderItem(tradeItem,order);
				if(orderItem != null){
					orderItems.add(orderItem);
					BigDecimal transPrice = orderItem.getTransPrice()!=null?orderItem.getTransPrice():BigDecimal.ZERO;
					BigDecimal salePrice = orderItem.getSalePrice()!=null?orderItem.getSalePrice():BigDecimal.ZERO;
					prodFee = prodFee.add(transPrice.multiply(new BigDecimal(orderItem.getNumber())));
					discountFee = discountFee.add(salePrice.subtract(transPrice).multiply(new BigDecimal(orderItem.getNumber())));

					stockMapMq.put(orderItem.getSkuId().longValue(), -orderItem.getNumber());
					brandShowStockMapMq.put(tradeItem.getBrandShowDetailId(), -orderItem.getNumber());
				}
			}
			order.setProdDiscountFee(discountFee);
			order.setProdFee(prodFee);
			return orderItems;
		}
		return null;
	}
	
	private OrderItem createOrderItem(TradeItem tradeItem,Order order) {
		if(tradeItem != null){
			OrderItem orderItem = new OrderItem();
			orderItem.setIsComment(OrderConstants.ORDERITEM_COMMENT_NO);
			orderItem.setNumber(tradeItem.getNum());
			orderItem.setOrderCode(order.getOrderCode());
			orderItem.setOrderId(order.getOrderId().intValue());
			orderItem.setProdId(tradeItem.getProdId());
			orderItem.setProdCode(tradeItem.getProdCode());
			orderItem.setSkuId(tradeItem.getSkuId());
			orderItem.setSkuCode(tradeItem.getSkuCode());
			orderItem.setProdSpecId(tradeItem.getProdSpecId());
			orderItem.setProdSpecName(tradeItem.getProdSpecName());
			orderItem.setProdTitle(tradeItem.getProdTitle());
			orderItem.setSalePrice(tradeItem.getMarketPrice());
			orderItem.setTransPrice(tradeItem.getShowPrice());
			orderItem.setStatus(OrderConstants.ORDERITEM_STATUS_NORMAL);
			orderItem.setSellerId(tradeItem.getSellerId());
			orderItem.setBcId(tradeItem.getBcId());
			
			this.orderItemMapper.insert(orderItem);
			return orderItem;
		}
		return null;
	}
	
	/**
	 * 创建订单日志
	 * @param order
	 * @param trade
	 */
	private void createOrderLog(Order order,Trade trade) {
		if(order != null){
			OrderLog orderLog = new OrderLog();
			orderLog.setOrderId(order.getOrderId());
			orderLog.setOrderCode(order.getOrderCode());
			orderLog.setOptTime(order.getCreatedDate());
			orderLog.setOptIp(trade.getUserIP());
			orderLog.setOptByName(trade.getOptName());
			orderLog.setOptContent("创建订单");
			orderLog.setFromOrderStatus(null);
			orderLog.setToOrderStatus(order.getOrderStatus());
			this.orderLogMapper.insert(orderLog);
		}
		
	}
}
