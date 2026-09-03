package com.example.mallu.seckill.mapper;

import com.example.mallu.seckill.dto.SeckillGoodsVO;
import com.example.mallu.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeckillGoodsMapper {

    SeckillGoods selectById(@Param("id") Long id);

    List<SeckillGoodsVO> selectByActivityId(@Param("activityId") Long activityId);

    List<SeckillGoods> selectActiveForRecovery();

    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    int increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
