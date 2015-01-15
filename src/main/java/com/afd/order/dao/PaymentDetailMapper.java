package com.afd.order.dao;

import java.util.List;

import com.afd.model.order.PaymentDetail;

public interface PaymentDetailMapper {

	int deleteByPrimaryKey(Long pDetailId);

	
	int insert(PaymentDetail record);

	
	int insertSelective(PaymentDetail record);

	
	PaymentDetail selectByPrimaryKey(Long pDetailId);

	
	int updateByPrimaryKeySelective(PaymentDetail record);

	
	int updateByPrimaryKey(PaymentDetail record);
	
	
	int selectPayedByOrderId (Long orderId);
		
	
	List<PaymentDetail> selectByOrderId (Long orderId);
	
		
	List<PaymentDetail> selectByPaymentId (Long PaymentId);
}