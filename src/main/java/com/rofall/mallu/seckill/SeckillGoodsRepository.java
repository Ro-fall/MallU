package com.rofall.mallu.seckill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SeckillGoodsRepository extends JpaRepository<SeckillGoods, Long> { List<SeckillGoods> findByActivityId(Long activityId); }
