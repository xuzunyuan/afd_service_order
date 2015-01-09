package com.afd.order.dao;

import com.afd.model.order.ReturnOrder;

public interface ReturnOrderMapper {
    int deleteByPrimaryKey(Integer retOrderId);

    int insert(ReturnOrder record);

    int insertSelective(ReturnOrder record);

    ReturnOrder selectByPrimaryKey(Integer retOrderId);

    int updateByPrimaryKeySelective(ReturnOrder record);

    int updateByPrimaryKey(ReturnOrder record);
}