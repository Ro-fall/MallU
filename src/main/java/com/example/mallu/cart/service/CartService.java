package com.example.mallu.cart.service;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.cart.dto.CartAddDTO;
import com.example.mallu.cart.dto.CartUpdateDTO;
import com.example.mallu.cart.dto.CartVO;
import com.example.mallu.cart.entity.Cart;
import com.example.mallu.cart.mapper.CartMapper;
import com.example.mallu.product.entity.Product;
import com.example.mallu.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    @Transactional
    public CartVO addToCart(CartAddDTO cartAddDTO) {
        Long userId = UserContext.getUserId();
        Product product = productMapper.selectById(cartAddDTO.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }
        if (product.getStock() < cartAddDTO.getQuantity()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "商品库存不足");
        }

        Cart existCart = cartMapper.selectByUserIdAndProductId(userId, cartAddDTO.getProductId());
        if (existCart != null) {
            int newQuantity = existCart.getQuantity() + cartAddDTO.getQuantity();
            if (product.getStock() < newQuantity) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "商品库存不足");
            }
            cartMapper.updateQuantity(existCart.getId(), userId, newQuantity);
            return cartMapper.selectById(existCart.getId(), userId);
        }

        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(cartAddDTO.getProductId());
        cart.setQuantity(cartAddDTO.getQuantity());
        cartMapper.insert(cart);
        return cartMapper.selectById(cart.getId(), userId);
    }

    public List<CartVO> listCart() {
        return cartMapper.selectByUserId(UserContext.getUserId());
    }

    @Transactional
    public CartVO updateQuantity(Long id, CartUpdateDTO updateDTO) {
        Long userId = UserContext.getUserId();
        CartVO cartVO = cartMapper.selectById(id, userId);
        if (cartVO == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车记录不存在");
        }
        if (cartVO.getStock() < updateDTO.getQuantity()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "商品库存不足");
        }
        cartMapper.updateQuantity(id, userId, updateDTO.getQuantity());
        return cartMapper.selectById(id, userId);
    }

    public void deleteCartItem(Long id) {
        int affected = cartMapper.deleteById(id, UserContext.getUserId());
        if (affected == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车记录不存在");
        }
    }
}
