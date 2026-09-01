package com.example.mallu.product.mapper;

import com.example.mallu.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    Product selectById(Long id);

    List<Product> selectPage(@Param("offset") Integer offset, @Param("size") Integer size);

    Long countAll();

    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
