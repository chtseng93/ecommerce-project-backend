package com.example.ecommerceproject.service.Impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.MybatisBatchUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ecommerceproject.entity.Order;
import com.example.ecommerceproject.entity.OrderItem;
import com.example.ecommerceproject.entity.OrderVo;
import com.example.ecommerceproject.entity.Product;
import com.example.ecommerceproject.entity.Role;
import com.example.ecommerceproject.mapper.primary.OrderItemMapper;
import com.example.ecommerceproject.mapper.primary.OrderMapper;
import com.example.ecommerceproject.model.req.OrderReq;
import com.example.ecommerceproject.service.OrderItemSevice;
import com.example.ecommerceproject.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

	@Autowired
	OrderMapper orderMapper;
	
	@Autowired
	OrderItemMapper orderItemMapper;
	
	@Autowired
	private TransactionTemplate transactionTemplate;
	
	@Autowired
	SqlSessionFactory sqlSessionFactory;
	
	@Override
	public OrderVo getOrderByOrderId(Integer orderId) {
		QueryWrapper<Order> wrapper = new QueryWrapper<Order>();
		wrapper.eq("order_id", orderId);
		return orderMapper.getOrderByOrderId(wrapper);

	}
	@Override
	public List<Order> getAllOrders() {
		return orderMapper.selectList(null);
	}
	@Override
	public List<OrderVo> getAllOrdersWithItems() {
		return orderMapper.getAllOrdersWithItems(null);
	}
	@Override
	public Integer saveOrder(OrderReq orderReq) {
		Integer orderNum = 0;
		if(orderReq!=null) {
    		Order newOrder = new Order();
    		newOrder.setAddress(orderReq.getAddress());
    		newOrder.setUserId(orderReq.getUserId());
    		newOrder.setOrderDate(new Date());
    		newOrder.setTotalPrice(orderReq.getTotalPrice());
    		
    		orderMapper.insert(newOrder);
    		Integer orderId = newOrder.getOrderId();
    		orderNum = newOrder.getOrderId();
    		System.out.println("orderId:"+orderId);
    		
    		if(!CollectionUtils.isEmpty(orderReq.getOrderItems())) {
    			List<OrderItem> newOrderItems= new ArrayList<>();
    			orderReq.getOrderItems().forEach(item->{
    				item.setOrderId(orderId);
    				newOrderItems.add(item);
    				
    			});
    			transactionTemplate.execute((TransactionCallback<List<BatchResult>>) status -> {
    			    MybatisBatch.Method<OrderItem> mapperMethod = new MybatisBatch.Method<>(OrderItemMapper.class);
    			    // execute inserting batch
    			   log.info("OrderServiceImpl-saveOrder-mapperMethod:"+mapperMethod);
    			    return MybatisBatchUtils.execute(sqlSessionFactory, newOrderItems, mapperMethod.insert());
    			});
//    			
    		}
    	
    		
    	}
		return orderNum;
	}
	@Override
	public List<OrderVo> getAllOrdersByUserId(int userId) {
		// TODO Auto-generated method stub
		QueryWrapper<Order> wrapper = new QueryWrapper<Order>();
		wrapper.eq("user_id", userId);
		return orderMapper.getOrdersByUserId(wrapper);
	}
	@Override
	public void updateOrderStatusById(Integer orderId, String orderStatus) {
		// TODO Auto-generated method stub
		orderMapper.updateOrderStatusById(orderId, orderStatus);
		
	}


}
