package com.example.ecommerceproject.controller;




import java.util.ArrayList;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;

import com.example.ecommerceproject.entity.OrderVo;

import com.example.ecommerceproject.exception.InternalServerErrorException;
import com.example.ecommerceproject.model.req.OrderReq;
import com.example.ecommerceproject.model.res.OrderRes;

import com.example.ecommerceproject.service.OrderService;
import com.example.ecommerceproject.util.DateUtils;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderController{
	
	@Autowired
	OrderService orderService;
	
	
	@GetMapping("/all-orders-items")
	public ResponseEntity<List<OrderRes>> getAllOrdersWithIems(){
		log.info("all-orders-items");
	    List<OrderVo> orders = orderService.getAllOrdersWithItems();
	    List<OrderRes> orderRess =new ArrayList<>();
	    if(!CollectionUtils.isEmpty(orders)) {
	    	orders.forEach(order->{
	    		OrderRes response = new OrderRes(order.getOrderId(), order.getUserId() , DateUtils.format(order.getOrderDate()), order.getAddress(), order.getStatus(), order.getTotalPrice(),order.getOrderItems());
	    		orderRess.add(response);
	    	});
	    	
	    }

		return ResponseEntity.ok(orderRess);

	}
	
    @PreAuthorize("@accessGuard.isSelfOrAdmin(#userId, authentication)")
	@GetMapping("/get/user-order/{userId}")
	public ResponseEntity<List<OrderRes>> getOrdersWithItemsByUserId(@PathVariable int userId){
		log.info("/get/user-order/"+userId);
		List<OrderVo> orders = orderService.getAllOrdersByUserId(userId);
	    List<OrderRes> orderRess =new ArrayList<>();
	    if(!CollectionUtils.isEmpty(orders)) {
	    	orders.forEach(order->{
	    		// System.out.println("order"+order);
    		OrderRes response = new OrderRes(order.getOrderId(), order.getUserId() , DateUtils.format(order.getOrderDate()), order.getAddress(), order.getStatus(), order.getTotalPrice(),order.getOrderItems());
	    		orderRess.add(response);
	    	});
	    	
	    }

		return ResponseEntity.ok(orderRess);

	}
	
	@PostMapping("/save/order")
    public ResponseEntity<?> insertOrders(@RequestBody OrderReq orderReq){
		log.info("/save/order");
		log.info(orderReq.toString());
        try{
            Integer orderId =orderService.saveOrder(orderReq);
            return ResponseEntity.ok(
            		 orderId);

        }catch (InternalServerErrorException e){
        	log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

	@PutMapping("/changeStatus/{orderId}")
	public ResponseEntity<String> updateOrderStatus(
			 @PathVariable int orderId,
			 @RequestBody OrderReq orderReq) {
		log.info(orderId+","+orderReq.getStatus());
		orderService.updateOrderStatusById(orderId,orderReq.getStatus());
		return new ResponseEntity<String>(HttpStatus.OK);
	}

	
	

}
