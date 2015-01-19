package com.afd.order.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.afd.constants.order.OrderConstants;
import com.afd.model.order.Order;
import com.afd.model.order.OrderLog;
import com.afd.model.order.Payment;
import com.afd.model.order.PaymentDetail;
import com.afd.model.order.PaymentOrder;
import com.afd.model.payment.vo.ResultVO;
import com.afd.model.user.User;
import com.afd.order.dao.OrderLogMapper;
import com.afd.order.dao.PaymentDetailMapper;
import com.afd.order.dao.PaymentMapper;
import com.afd.order.dao.UnionPayMapper;
import com.afd.service.order.IOrderService;
import com.afd.service.payment.IPaymentServices;
import com.afd.service.user.IUserService;

/**
 * Created with xiaotao User: xiaotao Date: 2014/8/11 Time: 16:18
 */

@Service("paymentServices")
public class PaymentServicesImpl implements IPaymentServices {

	@Autowired
	private IUserService userService;
	@Autowired
	private IOrderService orderService;
	@Autowired
	private PaymentMapper paymentMapper;
	@Autowired
	private UnionPayMapper unionPayMapper;
    @Autowired
	private OrderLogMapper orderLogMapper;
	@Autowired
	private PaymentDetailMapper paymentDetailMapper;

	public Long getPaymentId(List<Long> orderids, String paytype, String ip,
			Long userId, String payGw,String paymentType) {
		BigDecimal orderFee = new BigDecimal(0);
		List<Order> orders = new ArrayList<Order>();
		User user = this.userService.getUserById(userId);
		if (user == null) {
			return -1L;
		}
		
		if (orderids != null && orderids.size() > 0) {
			for (Long orderId : orderids) {
				Long[] orderId_arr = new Long[1];
				orderId_arr[0]=orderId;
				 List<Order> orders_back = this.orderService.getOrdersByIdsAndUserId(orderId_arr,userId);
				
				if(null==orders_back||orders_back.size()==0){
					return -2L;
				}
				Order order=orders_back.get(0);
				if(user.getUserId().compareTo(order.getUserId())!=0){
					return -3L;
				}				
				String orderStatus = order.getOrderStatus();
				if(!orderStatus.equals(OrderConstants.ORDER_STATUS_WAITPAYMENT)){
					return -4L;
				}
				String PayStatus = order.getPayStatus();
				if (PayStatus.equals(OrderConstants.PAY_STATUS_PAYED)
						|| PayStatus.equals(OrderConstants.PAY_STATUS_PAYING)) {
					return -5L;
				}
				if (!order.getPayType().equals(OrderConstants.PAY_TYPE_ONLINE)) {
					return -6L;
				}
				int payed_nums = paymentDetailMapper.selectPayedByOrderId(order.getOrderId());
				if(payed_nums>0){
					return -7L;
				}
				orders.add(order);
				BigDecimal ofee = order.getOrderFee();
				orderFee = orderFee.add(ofee);
			}
			if(orderFee.compareTo(new BigDecimal("0.00"))<1){
				return -11L;
			}
		} else {
			return -8L;
		}

		if(orderids.size()==1){
			Long orderId =orderids.get(0);
			List<PaymentDetail> pds = paymentDetailMapper.selectByOrderId(orderId);
			if(null!=pds&&pds.size()>0){
				PaymentDetail pd=pds.get(0);
				return pd.getPaymentId();
			}
		}		
		return savePaymentinfo( payGw, paytype, user, ip, orderFee,paymentType, orders);	
	}

	private Long savePaymentinfo(String payGw,String paytype,User user,String ip,BigDecimal orderFee,String paymentType,List<Order> orders){	
		Payment payment = new Payment();
		payment.setPayGw(payGw);
		payment.setStatus(OrderConstants.PAY_STATUS_UNPAY);
		payment.setType(paytype);
		payment.setUserId(user.getUserId());
		payment.setUserName(user.getUserName());
		payment.setCreateDate(new Date());
		payment.setCreateByIp(ip);
		payment.setPayAmount(orderFee);
		payment.setPaymentType(paymentType);
		int ret = this.paymentMapper.insertSelective(payment);
		if (ret > 0) {
			Long paymentId = payment.getPaymentId();
			if (orders != null && orders.size() > 0) {
				for (Order order : orders) {
					PaymentDetail record = new PaymentDetail();
					record.setAmount(order.getOrderFee());
					record.setOrderId(order.getOrderId());
					record.setPaymentId(paymentId);
					record.setPaymentType(paymentType);
					int retd = this.paymentDetailMapper.insertSelective(record);
					if (retd < 1) {
						return -9L;
					}
				}
			}
			return paymentId;
		}

		return -10L;

		
	}
	public Payment getPaymentInfo(Long paymentId) {
		Payment payment=this.paymentMapper.selectByPrimaryKey(paymentId);
		return payment;
	}

	/**
	 * 检查支付凭证的有效性
	 * 
	 * @param paymentId
	 * @return
	 */
	public ResultVO checkPaymentIdIsValid(Long paymentId) {
		ResultVO result = ResultVO.getFailure();

		if (null == paymentId) {
			result.setInfo("支付号ID非法！");
			return result;
		}
		Payment payment = this.paymentMapper.selectByPrimaryKey(paymentId);

		if (null == payment) {
			result.setInfo("支付号不存在！");
			return result;
		} else {
			String paymentStatus = payment.getStatus();
			if (paymentStatus.equals(OrderConstants.PAY_STATUS_PAYED)
					|| paymentStatus.equals(OrderConstants.PAY_STATUS_PAYING)) {
				result.setInfo("订单支付状态非法，只能对支付状态值为“未支付”或“支付失败”的订单进行支付！（"
						+ paymentStatus + "）");
				return result;
			}
		}
		result.setResult(true);
		result.setInfo("");
		return result;

	}

	/**
	 * 检查支付金额的有效性
	 * 
	 * @param paymentId
	 * @return
	 */
	public ResultVO checkAmountIsValid(Long paymentId, BigDecimal payamount) {
		ResultVO result = ResultVO.getFailure();

		if (null == paymentId) {
			result.setInfo("支付号ID非法！");
			return result;
		}
		Payment payment = this.paymentMapper.selectByPrimaryKey(paymentId);

		if (null == payment) {
			result.setInfo("支付号不存在！");
			return result;
		} else {
			String paymentStatus = payment.getStatus();
			if (paymentStatus.equals(OrderConstants.PAY_STATUS_PAYED)
					|| paymentStatus.equals(OrderConstants.PAY_STATUS_PAYING)) {
				result.setInfo("订单支付状态非法，只能对支付状态值为“未支付”或“支付失败”的订单进行支付！（"
						+ paymentStatus + "）");
				return result;
			}

			if (payment.getPayAmount().compareTo(payamount) != 0) {
				result.setInfo("支付金额不正确！");
				return result;
			}
		}
		result.setResult(true);
		result.setInfo("");
		return result;

	}

	public boolean savePaymentDetail(List<Order> orders, Long paymentId,String paymentType) {

		if (orders != null && orders.size() > 0) {
			for (Order order : orders) {
				PaymentDetail record = new PaymentDetail();
				record.setAmount(order.getOrderFee());
				record.setOrderId(order.getOrderId());
				record.setPaymentId(paymentId);
				record.setPaymentType(paymentType);
				int ret = this.paymentDetailMapper.insertSelective(record);
				if (ret < 1) {
					return false;
				}
			}
		}
		return true;
	}

	public int updatePayment(Payment payment) {
		return this.paymentMapper.updateByPrimaryKeySelective(payment);
	}
	
	/**
	 * 修改支付中的支会记录状态
	 * 
	 * @param Payment
	 * @return int
	 */
	public int updatePaymentpaying(Payment payment) {
	 return this.paymentMapper.updateByPrimaryKeyPaying(payment);
	}
	
	public int addLog(OrderLog orderLog){
		 return this.orderLogMapper.insert(orderLog);
	}
	
	public int updatePaySuccessOrder(PaymentOrder order){
		int ret = this.unionPayMapper.updatePaySuccessOrder(order);
	    this.unionPayMapper.updatePaySuccessOrderStatus(order);
	    return ret;
	}
	
	/**
	 * 修改支付中的订单状态
	 * 
	 * @param PaymentOrder
	 * @return int
	 */
	public int updatePayingOrder(PaymentOrder order){			     
		  int ret= this.unionPayMapper.updatePayingOrder(order);		 
		  return ret;
		}
	
	public int updatePayFailureOrder(PaymentOrder order){
		   return this.unionPayMapper.updatePaySuccessOrder(order);
		}
	
	public PaymentOrder getOrderByOrderId(Long orderId){
	   PaymentOrder porder =	unionPayMapper.getOrderByOrderId(orderId);
	   return porder;
	}
	
	public List<PaymentDetail> getPaymentDetailByPamentId(Long PaymentId){
		List<PaymentDetail> pds= paymentDetailMapper.selectByPaymentId (PaymentId);
		return pds;
	}
	
	/**
	 * 取消超期的未支付的订单
	 * @param days 过期时间
	 * @return void
	 */
	public void updatelNoPayOrder(int days){
		List<Payment> payments=this.paymentMapper.getPaymentExpired(days);
		if (payments != null && payments.size() > 0) {
			List<Long> ids=new ArrayList<Long>();
			for (Payment payment : payments) {
				Long PaymentId = payment.getPaymentId();
				List<PaymentDetail> pds = this.paymentDetailMapper.selectByPaymentId(PaymentId);
				if(pds != null && pds.size() > 0){
					for(PaymentDetail pd : pds){
						Long orderId = pd.getOrderId();
						Order order=this.orderService.getOrderById(orderId);
						if(order!=null){
							if(order.getOrderStatus().equals(OrderConstants.ORDER_STATUS_WAITPAYMENT)){
								ids.add(order.getOrderId());
							}
						}
					}
				}
			}
			if(ids.size()>0){
				this.orderService.cancelOrderByBoss(ids, "nopay","未支付系统自动取消！");
			}
		}
	}
}
