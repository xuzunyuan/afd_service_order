package com.afd.order.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.afd.model.order.LogisticsCompany;

public interface LogisticsCompanyMapper {
	int deleteByPrimaryKey(Byte logisticsCompId);

	int insert(LogisticsCompany record);

	int insertSelective(LogisticsCompany record);

	LogisticsCompany selectByPrimaryKey(Byte logisticsCompId);

	int updateByPrimaryKeySelective(LogisticsCompany record);

	int updateByPrimaryKey(LogisticsCompany record);

	List<LogisticsCompany> selectValidLogisticsCompanyList();

	List<LogisticsCompany> selectLogisticsCompanyByIds(@Param("ids") long[] ids);
}