package com.example.mallu.seckill.mapper;

import com.example.mallu.seckill.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeckillActivityMapper {

    SeckillActivity selectById(@Param("id") Long id);

    List<SeckillActivity> selectList();
}
