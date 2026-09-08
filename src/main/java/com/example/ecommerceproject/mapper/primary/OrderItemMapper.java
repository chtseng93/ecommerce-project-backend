package com.example.ecommerceproject.mapper.primary;



import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.ecommerceproject.entity.OrderItem;

import com.github.yulichang.base.MPJBaseMapper;
@Mapper
public interface OrderItemMapper extends MPJBaseMapper<OrderItem> {
//	
//	@Select("select * from orderitem where order_id=#{orderId} ")
//	List<OrderItem> getOrderItemsByOrderId(int orderId);
	
	@Select("select i.*, p.name as productName from orderitem i join product p on i.product_Id=p.product_Id where order_id=#{orderId} ")
	List<OrderItem> getOrderItemsByOrderId(@Param("orderId")Integer orderId);
	
 
	

}
