package com.afd.test;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.afd.model.order.ReturnOrder;
import com.afd.model.order.ReturnOrderItem;
import com.afd.order.dao.OrderItemMapper;
import com.afd.order.dao.ReturnOrderItemMapper;
import com.afd.order.dao.ReturnOrderMapper;
import com.afd.order.service.impl.OrderServiceImpl;
import com.alibaba.druid.filter.config.ConfigTools;

@ActiveProfiles("develop")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:spring/*.xml","classpath:spring-dubbo-consumer.xml"})
public class MyTest {
	@Autowired
	private ReturnOrderMapper retOrderMapper;
	@Autowired
	private ReturnOrderItemMapper retOrderItemMapper;

	@Test
	public void aaa(){
	}
	
	@Test
	public void bbb(){
		ReturnOrderItem selectByPrimaryKey = retOrderItemMapper.selectByPrimaryKey(1);
		System.out.println(selectByPrimaryKey.getItemId());
	}
	
	public static void main(String[] args) {
		try {
			System.out.println(ConfigTools.decrypt("Biyu5YzU+6sxDRbmWEa3B2uUcImzDo0BuXjTlL505+/pTb+/0Oqd3ou1R6J8+9Fy3CYrM18nBDqf6wAaPgUGOg=="));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
