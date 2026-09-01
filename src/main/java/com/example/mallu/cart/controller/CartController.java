package com.example.mallu.cart.controller;

import com.example.mallu.common.result.Result;
import com.example.mallu.cart.dto.CartAddDTO;
import com.example.mallu.cart.dto.CartUpdateDTO;
import com.example.mallu.cart.dto.CartVO;
import com.example.mallu.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public Result<CartVO> add(@RequestBody @Valid CartAddDTO cartAddDTO) {
        return Result.success(cartService.addToCart(cartAddDTO));
    }

    @GetMapping
    public Result<List<CartVO>> list() {
        return Result.success(cartService.listCart());
    }

    @PutMapping("/{id}")
    public Result<CartVO> update(@PathVariable Long id, @RequestBody @Valid CartUpdateDTO updateDTO) {
        return Result.success(cartService.updateQuantity(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.deleteCartItem(id);
        return Result.success();
    }
}
