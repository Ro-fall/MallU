package com.rofall.mallu.cart;

import com.rofall.mallu.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart-items")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping
    public ApiResponse<CartResponse> add(@RequestBody @Valid CartAddRequest request) {
        return ApiResponse.success(cartService.add(request));
    }

    @GetMapping
    public ApiResponse<List<CartResponse>> list() {
        return ApiResponse.success(cartService.list());
    }

    @PutMapping("/{cartItemId}")
    public ApiResponse<CartResponse> update(@PathVariable Long cartItemId, @RequestBody @Valid CartUpdateRequest request) {
        return ApiResponse.success(cartService.update(cartItemId, request));
    }

    @DeleteMapping("/{cartItemId}")
    public ApiResponse<Void> delete(@PathVariable Long cartItemId) {
        cartService.delete(cartItemId);
        return ApiResponse.success();
    }
}
