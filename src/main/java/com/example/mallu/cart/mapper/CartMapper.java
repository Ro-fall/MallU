package com.example.mallu.cart.mapper;

import com.example.mallu.cart.entity.Cart;
import com.example.mallu.cart.dto.CartVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {

    int insert(Cart cart);

    int updateQuantity(@Param("id") Long id, @Param("userId") Long userId, @Param("quantity") Integer quantity);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    Cart selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    CartVO selectById(@Param("id") Long id, @Param("userId") Long userId);

    List<CartVO> selectByUserId(Long userId);
}
