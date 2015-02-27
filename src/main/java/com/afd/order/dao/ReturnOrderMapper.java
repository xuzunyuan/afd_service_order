package com.afd.order.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.afd.common.mybatis.Page;
import com.afd.model.order.ReturnOrder;

public interface ReturnOrderMapper {
    int deleteByPrimaryKey(Integer retOrderId);

    int insert(ReturnOrder record);

    int insertSelective(ReturnOrder record);

    ReturnOrder selectByPrimaryKey(Integer retOrderId);

    int updateByPrimaryKeySelective(ReturnOrder record);

    int updateByPrimaryKey(ReturnOrder record);

	List<ReturnOrder> getRetOrdersByUserIdPage(@Param("userId")long userId,@Param("page")Page<ReturnOrder> page);
}