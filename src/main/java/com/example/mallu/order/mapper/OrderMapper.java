package com.example.mallu.order.mapper;

import com.example.mallu.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    int insert(Order order);

    Order selectByOrderNo(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    Order selectByOrderNoForSystem(@Param("orderNo") String orderNo);

    Order selectById(@Param("id") Long id, @Param("userId") Long userId);

    Order selectByIdForSystem(@Param("id") Long id);

    List<Order> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("size") Integer size);

    Long countByUserId(Long userId);

    int updateStatus(Order order);

    int updateStatusIfExpected(@Param("id") Long id,
                               @Param("userId") Long userId,
                               @Param("expectedStatus") Integer expectedStatus,
                               @Param("status") Integer status,
                               @Param("payTime") java.time.LocalDateTime payTime);
}
