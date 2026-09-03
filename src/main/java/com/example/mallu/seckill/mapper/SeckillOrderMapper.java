package com.example.mallu.seckill.mapper;

import com.example.mallu.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeckillOrderMapper {

    int insert(SeckillOrder seckillOrder);

    SeckillOrder selectById(@Param("id") Long id);

    SeckillOrder selectByOrderId(@Param("orderId") Long orderId);

    SeckillOrder selectByUserIdAndGoodsId(@Param("userId") Long userId, @Param("seckillGoodsId") Long seckillGoodsId);

    int updateStatus(SeckillOrder seckillOrder);

    /**
     * 已取消的秒杀订单保留在普通订单表中用于追溯；删除秒杀资格记录，允许用户重新参与。
     */
    int deleteByOrderId(@Param("orderId") Long orderId);

    List<SeckillOrder> selectTimeoutOrders(@Param("timeoutMinutes") Integer timeoutMinutes);

    List<Long> selectActiveUserIdsByGoodsId(@Param("seckillGoodsId") Long seckillGoodsId);
}
