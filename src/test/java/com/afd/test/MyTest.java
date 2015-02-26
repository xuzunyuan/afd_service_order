package com.afd.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.afd.model.order.OrderItem;
import com.afd.order.dao.OrderItemMapper;
import com.afd.order.service.impl.OrderServiceImpl;
import com.alibaba.druid.filter.config.ConfigTools;

@ActiveProfiles("develop")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:spring/*.xml","classpath:spring-dubbo-consumer.xml"})
public class MyTest {
	@Autowired
	@Qualifier("orderService")
	private OrderServiceImpl orderService;
	@Autowired
	private OrderItemMapper oiMapper;

	@Test
	public void aaa(){
		OrderItem oi = oiMapper.getOrderItemById(5l);
		System.out.println(oi.getOrder().getrName());
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
