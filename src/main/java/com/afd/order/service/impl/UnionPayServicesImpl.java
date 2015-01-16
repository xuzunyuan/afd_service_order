package com.afd.order.service.impl;

import com.afd.constants.order.OrderConstants;
import com.afd.constants.order.PayModeEnum;
import com.afd.model.order.OrderLog;
import com.afd.model.order.PaymentOrder;
import com.afd.model.payment.vo.ResultVO;
import com.afd.order.dao.OrderLogMapper;
import com.afd.order.dao.UnionPayMapper;
import com.afd.service.payment.IUnionPayServices;
import com.afd.common.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created with Liu Yong
 * User: melnnyy
 * Date: 2014/7/4
 * Time: 16:18
 */
@SuppressWarnings("rawtypes")
@Service("unionPayServices")
public class UnionPayServicesImpl implements IUnionPayServices {

    @Autowired
	private UnionPayMapper unionPayMapper;

    @Autowired
	private OrderLogMapper orderLogMapper;

    @Override
    public ResultVO verifyOrder(Long orderId) {
        ResultVO result = ResultVO.getFailure();

        if(null == orderId || 0 > orderId) {
            result.setInfo("订单ID非法！");
            return result;
        }

        PaymentOrder order = unionPayMapper.getOrderByOrderId(orderId);


        if(null == order) {
            result.setInfo("订单不存在！");
            return result;
        }

        if(!order.getOrderStatus().equals(OrderConstants.ORDER_STATUS_WAITPAYMENT)) {
            result.setInfo("订单状态非法，只能对状态值为“等待支付”的订单进行支付！（"+order.getOrderStatus()+"）");
            return result;
        }

        if(!order.getPayStatus().equals(OrderConstants.PAY_STATUS_UNPAY)
                && !order.getPayStatus().equals(OrderConstants.PAY_STATUS_FAILURE)) {
            result.setInfo("订单支付状态非法，只能对支付状态值为“未支付”或“支付失败”的订单进行支付！（"+order.getOrderStatus()+"）");
            return result;
        }

        result.setResult(true);
        result.setInfo("");

        return result;
    }

    @Override
    public PaymentOrder getOrder(Long orderId) {
        return unionPayMapper.getOrderByOrderId(orderId);
    }

    @Override
    public ResultVO updateOrder(Long orderId, boolean flag, String gateId, String info) {
        ResultVO result = ResultVO.getFailure();
        PaymentOrder order = unionPayMapper.getOrderByOrderId(orderId);

        if(null == order) {
            result.setInfo("数据非法！");
            return result;
        }

        order.setInfo(info);
        PayModeEnum pme = PayModeEnum.getByGateId(gateId);
        if(null != pme) order.setReceiptMode(pme.getValue());
        // todo 硬编码
        order.setReceiptType("1");
        order.setLastUpdateDate(new Date());
        order.setLastUpdateByName("银联在线支付");

        try {
            if(flag) paySuccess(order);
            else payFailure(order);

            result.setResult(true);
            result.setInfo("更新成功！");
        } catch (Exception e) {
            result.setInfo("更新失败！" + e.getMessage());
        }

        return result;
    }

    // --------------------------------------------------------------------------

    private void paySuccess(PaymentOrder order) throws Exception {
        order.setOrderStatus(OrderConstants.ORDER_STATUS_WAITDELIVERED);
        order.setPayStatus(OrderConstants.PAY_STATUS_PAYED);
        order.setReceiptDate(order.getLastUpdateDate());
        int result = unionPayMapper.updatePaySuccessOrder(order);

        if(0 != result) {
            OrderLog orderLog = new OrderLog();
            orderLog.setFromOrderStatus(OrderConstants.ORDER_STATUS_WAITPAYMENT);
            orderLog.setOptContent("订单支付成功！");
            orderLog.setOptByName("银联在线支付");
            orderLog.setOptTime(DateUtils.currentDate());
            orderLog.setOrderId(order.getOrderId());
            orderLog.setToOrderStatus(order.getOrderStatus());
            this.orderLogMapper.insert(orderLog);
        }
    }

    private void payFailure(PaymentOrder order) throws Exception {
        order.setPayStatus(OrderConstants.PAY_STATUS_FAILURE);
        int result = unionPayMapper.updatePayFailureOrder(order);

        if(0 != result) {
            OrderLog orderLog = new OrderLog();
            orderLog.setFromOrderStatus(order.getOrderStatus());
            orderLog.setOptContent("订单支付失败！(" + order.getInfo() + ")");
            orderLog.setOptByName("银联在线支付");
            orderLog.setOptTime(DateUtils.currentDate());
            orderLog.setOrderId(order.getOrderId());
            orderLog.setToOrderStatus(order.getOrderStatus());
            this.orderLogMapper.insert(orderLog);
        }
    }
}
