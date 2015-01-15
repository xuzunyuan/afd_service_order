package com.afd.order.dao;

import com.afd.model.order.PaymentOrder;
import org.apache.ibatis.annotations.Param;


public interface UnionPayMapper {
    /**
     * 根据订单ID，得到订单信息
     * @param orderId
     * @return
     */
    PaymentOrder getOrderByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新支付成功的订单支付信息
     * @param order
     * @return
     */
    int updatePaySuccessOrder(@Param("order") PaymentOrder order);

    /**
     * 更新支付成功的订单状态
     * @param order
     * @return
     */
    int updatePaySuccessOrderStatus(@Param("order") PaymentOrder order);
    
    /**
     * 更新支付失败的订单信息
     * @param order
     * @return
     */
    int updatePayFailureOrder(@Param("order") PaymentOrder order);
    
    int updatePayingOrder(@Param("order") PaymentOrder order);
}
