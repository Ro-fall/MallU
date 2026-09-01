package com.example.mallu.order.mapper;

import com.example.mallu.order.entity.OrderEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderEventLogMapper {

    int insert(OrderEventLog log);

    List<OrderEventLog> selectByOrderId(@Param("orderId") Long orderId);
}
