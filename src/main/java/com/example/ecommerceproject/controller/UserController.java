package com.example.ecommerceproject.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerceproject.entity.Role;

import com.example.ecommerceproject.entity.UserVo;
import com.example.ecommerceproject.model.res.UserRes;
import com.example.ecommerceproject.service.RoleService;
import com.example.ecommerceproject.service.UserService;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController  {
	
	@Autowired
	RoleService roleService;
	
	@Autowired
	UserService userService;
	
	@GetMapping(path="/hello", produces="text/plain")
	public String HelloWorld() {
		return "HEY!";
	}
	
	@GetMapping(path="/arole", produces="application/json")
    public Role getARole() {
	 log.info("/arole");
        return roleService.getRoleInfo((Integer) 1);
    }
    
	@PreAuthorize("@accessGuard.isSelfOrAdmin(#userId, authentication)")
	@GetMapping("/get/user/{userId}")
    public ResponseEntity<?> getUserByUserId(@PathVariable("userId") int userId){
		
		log.info("/get/user/"+userId);
        try{
            UserVo theUser = userService.getUserInfo(userId);
            UserRes response = new UserRes(theUser.getUserId(),theUser.getFirstName(),theUser.getLastName(),theUser.getEmail(),theUser.getRoleName(theUser.getRoles()));
           
            return ResponseEntity.ok(response);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching user");
        }
    }

}
