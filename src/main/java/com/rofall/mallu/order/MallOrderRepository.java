package com.rofall.mallu.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MallOrderRepository extends JpaRepository<MallOrder, Long> {
    List<MallOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<MallOrder> findByIdAndUserId(Long id, Long userId);
}
