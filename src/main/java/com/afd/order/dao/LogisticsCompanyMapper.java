package com.afd.order.dao;

import java.util.List;

import com.afd.model.order.LogisticsCompany;

public interface LogisticsCompanyMapper {
	int deleteByPrimaryKey(Byte logisticsCompId);

	int insert(LogisticsCompany record);

	int insertSelective(LogisticsCompany record);

	LogisticsCompany selectByPrimaryKey(Byte logisticsCompId);

	int updateByPrimaryKeySelective(LogisticsCompany record);

	int updateByPrimaryKey(LogisticsCompany record);

	List<LogisticsCompany> selectValidLogisticsCompanyList();
}