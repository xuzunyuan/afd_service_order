package com.afd.order.dao;

import com.afd.model.order.LogisticsCompany;

public interface LogisticsCompanyMapper {
    int deleteByPrimaryKey(Byte logisticsCompId);

    int insert(LogisticsCompany record);

    int insertSelective(LogisticsCompany record);

    LogisticsCompany selectByPrimaryKey(Byte logisticsCompId);

    int updateByPrimaryKeySelective(LogisticsCompany record);

    int updateByPrimaryKey(LogisticsCompany record);
}