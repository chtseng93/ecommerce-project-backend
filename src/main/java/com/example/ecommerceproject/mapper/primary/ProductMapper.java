package com.example.ecommerceproject.mapper.primary;




import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.ecommerceproject.entity.Product;
import com.github.yulichang.base.MPJBaseMapper;

@Mapper
public interface ProductMapper extends MPJBaseMapper<Product>{
	@Select("select distinct product_type from product")
	List<String> getProductTypes();

	@Update("update product set status='0' where product_id=#{productId}")
	void updateProductStatusById(@Param("productId")Integer productId);
	
	
//	@Insert("insert into room(price, type, picture) values(#{price},#{type},#{picture})")
//	@Options(useGeneratedKeys=true, keyProperty="id",keyColumn = "id")
//	int insertRoom(Room room);

}
