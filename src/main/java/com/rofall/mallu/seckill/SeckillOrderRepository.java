package com.rofall.mallu.seckill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SeckillOrderRepository extends JpaRepository<SeckillOrder, Long> {
    boolean existsByUserIdAndSeckillGoodsId(Long userId, Long seckillGoodsId);
    Optional<SeckillOrder> findByUserIdAndSeckillGoodsId(Long userId, Long seckillGoodsId);
    Optional<SeckillOrder> findByOrderId(Long orderId);
}
