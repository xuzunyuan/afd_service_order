package com.afd.order.dao;

import com.afd.model.order.PaymentDetail;

public interface PaymentDetailMapper {
    int deleteByPrimaryKey(Integer pDetailId);

    int insert(PaymentDetail record);

    int insertSelective(PaymentDetail record);

    PaymentDetail selectByPrimaryKey(Integer pDetailId);

    int updateByPrimaryKeySelective(PaymentDetail record);

    int updateByPrimaryKey(PaymentDetail record);
}