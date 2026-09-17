package com.rofall.mallu.cart;

import com.rofall.mallu.catalog.Product;
import com.rofall.mallu.catalog.ProductRepository;
import com.rofall.mallu.common.BusinessException;
import com.rofall.mallu.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CartResponse add(CartAddRequest request) {
        Long userId = UserContext.requireUserId();
        Product product = availableProduct(request.productId());
        CartItem item = cartItemRepository.findByUserIdAndProductId(userId, product.getId())
                .orElseGet(() -> new CartItem(userId, product.getId(), 0));
        int targetQuantity = item.getQuantity() + request.quantity();
        ensureStock(product, targetQuantity);
        item.changeQuantity(targetQuantity);
        return toResponse(cartItemRepository.save(item), product);
    }

    @Transactional(readOnly = true)
    public List<CartResponse> list() {
        return cartItemRepository.findByUserIdOrderByUpdatedAtDesc(UserContext.requireUserId()).stream()
                .map(item -> toResponse(item, availableProduct(item.getProductId()))).toList();
    }

    @Transactional
    public CartResponse update(Long cartItemId, CartUpdateRequest request) {
        CartItem item = owned(cartItemId);
        Product product = availableProduct(item.getProductId());
        ensureStock(product, request.quantity());
        item.changeQuantity(request.quantity());
        return toResponse(item, product);
    }

    @Transactional
    public void delete(Long cartItemId) {
        cartItemRepository.delete(owned(cartItemId));
    }

    private CartItem owned(Long cartItemId) {
        return cartItemRepository.findByIdAndUserId(cartItemId, UserContext.requireUserId())
                .orElseThrow(() -> new BusinessException(4044, HttpStatus.NOT_FOUND, "购物车项不存在"));
    }

    private Product availableProduct(Long productId) {
        return productRepository.findById(productId).filter(Product::getStatus)
                .orElseThrow(() -> new BusinessException(4042, HttpStatus.NOT_FOUND, "商品不存在或已下架"));
    }

    private void ensureStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new BusinessException(4003, HttpStatus.CONFLICT, "商品库存不足");
        }
    }

    private CartResponse toResponse(CartItem item, Product product) {
        return new CartResponse(item.getId(), product.getId(), product.getName(), product.getPrice(),
                item.getQuantity(), product.getStock());
    }
}
