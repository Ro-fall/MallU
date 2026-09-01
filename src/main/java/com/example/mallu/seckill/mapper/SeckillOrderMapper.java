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

    List<SeckillOrder> selectTimeoutOrders(@Param("timeoutMinutes") Integer timeoutMinutes);
}
