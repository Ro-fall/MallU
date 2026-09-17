package com.rofall.mallu.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);
    List<CartItem> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);
    List<CartItem> findByIdInAndUserId(List<Long> ids, Long userId);
}
