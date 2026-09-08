package com.example.ecommerceproject.mapper.primary;

import java.util.List;

import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ecommerceproject.entity.Role;
import com.example.ecommerceproject.entity.User;
import com.example.ecommerceproject.entity.UserVo;
import com.github.yulichang.base.MPJBaseMapper;

public interface UserMapper extends MPJBaseMapper<User>{
	
	@Results(value = {
	         @Result(property="userId", column="user_id"),
	         @Result(property="firstName", column="first_name"),
	         @Result(property="lastName", column="last_name"),
	         @Result(property="email", column="email"),
	         @Result(property="password", column="password"),
	         @Result(property="roles", column="user_id", 
	         many=@Many(select="com.example.ecommerceproject.mapper.primary.RoleMapper.getRoleByUserId"))
	    })
	@Select("select u.* from user_profile u where ${ew.SqlSegment} ")
	UserVo getUserList(@Param("ew") QueryWrapper queryWrapper);
	
	
	@Select("select * from user_profile u left join user_role ur on ur.user_id=u.user_id where ur.role_id=#{roleId} ")
	List<Role> getUserByRoleId(@Param("roleId")Integer roleId);
}
