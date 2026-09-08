package com.example.ecommerceproject.mapper.primary;

import java.util.List;

import org.apache.ibatis.annotations.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ecommerceproject.entity.Order;
import com.example.ecommerceproject.entity.OrderVo;
import com.github.yulichang.base.MPJBaseMapper;

@Mapper
public interface OrderMapper extends MPJBaseMapper<Order> {


	@Results(value = { @Result(property = "orderId", column = "order_id"),
			@Result(property = "userId", column = "user_id"), 
			@Result(property = "orderDate", column = "order_date"),
			@Result(property = "address", column = "address"),
			@Result(property = "status", column = "status"),
			@Result(property = "totalPrice", column = "total_price"),
			@Result(property = "orderItems", column = "order_id", many = @Many(select = "com.example.ecommerceproject.mapper.primary.OrderItemMapper.getOrderItemsByOrderId")) })
	@Select("select * from user_order where ${ew.SqlSegment} ")
	OrderVo getOrderByOrderId(@Param("ew") QueryWrapper queryWrapper);
	
	
	@Results(value = { @Result(property = "orderId", column = "order_id"),
			@Result(property = "userId", column = "user_id"), 
			@Result(property = "orderDate", column = "order_date"),
			@Result(property = "address", column = "address"),
			@Result(property = "status", column = "status"),
			@Result(property = "totalPrice", column = "total_price"),
			@Result(property = "orderItems", column = "order_id", many = @Many(select = "com.example.ecommerceproject.mapper.primary.OrderItemMapper.getOrderItemsByOrderId")) })
	@Select("select * from user_order")
	List <OrderVo> getAllOrdersWithItems(@Param("ew") QueryWrapper queryWrapper);
	
	
	@Update("update user_order set status=#{orderStatus} where order_id=#{orderId}")
	void updateOrderStatusById(@Param("orderId")Integer orderId,@Param("orderStatus")String orderStatus);
	

	@Results(value = { @Result(property = "orderId", column = "order_id"),
			@Result(property = "userId", column = "user_id"), 
			@Result(property = "orderDate", column = "order_date"),
			@Result(property = "address", column = "address"),
			@Result(property = "status", column = "status"),
			@Result(property = "totalPrice", column = "total_price"),
			@Result(property = "orderItems", column = "order_id", many = @Many(select = "com.example.ecommerceproject.mapper.primary.OrderItemMapper.getOrderItemsByOrderId")) })
	@Select("select * from user_order where ${ew.SqlSegment} order by order_id desc ")
	List<OrderVo> getOrdersByUserId(@Param("ew") QueryWrapper queryWrapper);
	
	
	

}
