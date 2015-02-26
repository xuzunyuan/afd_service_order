/**
 * Copyright (c)2015-? by www.afd.com. All rights reserved.
 * 
 */
package com.afd.order.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.afd.model.order.LogisticsCompany;
import com.afd.order.dao.LogisticsCompanyMapper;
import com.afd.service.order.ILogisticsCompanyService;

/**
 * 物流公司服务
 * 
 * @author xuzunyuan
 * @date 2015年2月26日
 */
@Service("logisticsCompanyService")
public class LogisticsCompanyServiceImpl implements ILogisticsCompanyService {
	@Autowired
	LogisticsCompanyMapper logisticsMapper;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.afd.service.order.ILogisticsCompanyService#getValidLogisticsCompany()
	 */
	@Override
	public List<LogisticsCompany> getValidLogisticsCompany() {
		return logisticsMapper.selectValidLogisticsCompanyList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.afd.service.order.ILogisticsCompanyService#getLogisticsCompanyByIds
	 * (long[])
	 */
	@Override
	public List<LogisticsCompany> getLogisticsCompanyByIds(long[] ids) {
		return logisticsMapper.selectLogisticsCompanyByIds(ids);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.afd.service.order.ILogisticsCompanyService#getLogisticsCompanyById
	 * (java.lang.Long)
	 */
	@Override
	public LogisticsCompany getLogisticsCompanyById(Long id) {
		return logisticsMapper.selectByPrimaryKey(id.byteValue());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.afd.service.order.ILogisticsCompanyService#newLogisticsCompany(com
	 * .afd.model.order.LogisticsCompany)
	 */
	@Override
	public long newLogisticsCompany(LogisticsCompany lc) {
		return logisticsMapper.insert(lc) != 0 ? lc.getLogisticsCompId() : 0L;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.afd.service.order.ILogisticsCompanyService#modifyLogisticsCompany
	 * (com.afd.model.order.LogisticsCompany)
	 */
	@Override
	public int modifyLogisticsCompany(LogisticsCompany lc) {
		return logisticsMapper.updateByPrimaryKey(lc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.afd.service.order.ILogisticsCompanyService#deleteLogisticsCompany
	 * (java.lang.Long)
	 */
	@Override
	public int deleteLogisticsCompany(Long id) {
		return logisticsMapper.updateStatus(id.byteValue(), "0");
	}

}
