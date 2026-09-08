package com.example.ecommerceproject.mapper.primary;


import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ecommerceproject.entity.Role;
import com.example.ecommerceproject.entity.RoleVo;
import com.github.yulichang.base.MPJBaseMapper;


@Component
public interface RoleMapper extends MPJBaseMapper<Role>{
	
//	@Select("select * from role")
//	List<Role> getRoles();
	
	@Select("select r.* from role r left join user_role ur on ur.role_id=r.role_id where ur.user_id=#{userId} ")
	List<Role> getRoleByUserId(@Param("userId")Integer userId);
	
	@Results(value = {
	         @Result(property="roleId", column="role_id"),
	         @Result(property="name", column="name"),
	         @Result(property="users", column="role_id", 
	         many=@Many(select="com.example.ecommerceproject.mapper.primary.UserMapper.getUserByRoleId"))
	    })
	@Select("select * from role where ${ew.SqlSegment} ")
	RoleVo getRoleList(@Param("ew") QueryWrapper queryWrapper);

	@Insert("insert into user_role(user_id, role_id) value (#{userId},#{roleId}) ")
	void insertUserRole(@Param("userId")Integer userId, @Param("roleId")Integer roleId);
	
	
	
	
	
	
	
	
}
