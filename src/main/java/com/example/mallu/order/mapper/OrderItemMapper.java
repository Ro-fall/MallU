package com.example.mallu.order.mapper;

import com.example.mallu.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    int batchInsert(List<OrderItem> items);

    List<OrderItem> selectByOrderId(Long orderId);
}
