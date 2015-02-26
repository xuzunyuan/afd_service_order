package com.afd.order.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.afd.model.order.LogisticsCompany;

public interface LogisticsCompanyMapper {
	int deleteByPrimaryKey(Byte logisticsCompId);

	int insert(LogisticsCompany record);

	int insertSelective(LogisticsCompany record);

	LogisticsCompany selectByPrimaryKey(Byte logisticsCompId);

	int updateByPrimaryKeySelective(LogisticsCompany record);

	int updateByPrimaryKey(LogisticsCompany record);

	List<LogisticsCompany> selectValidLogisticsCompanyList();

	List<LogisticsCompany> selectLogisticsCompanyByIds(
			@Param("ids") List<Long> ids);

	@Update("update t_logistics_company set status = #{1} where logistics_comp_id = #{0}")
	int updateStatus(Byte logisticsCompId, String status);
}