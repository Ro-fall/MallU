package com.example.mallu.seckill.service;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.redis.SeckillRedisService;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.order.entity.Order;
import com.example.mallu.order.entity.OrderItem;
import com.example.mallu.order.mapper.OrderMapper;
import com.example.mallu.order.mapper.OrderItemMapper;
import com.example.mallu.product.entity.Product;
import com.example.mallu.product.mapper.ProductMapper;
import com.example.mallu.seckill.dto.SeckillGoodsVO;
import com.example.mallu.seckill.dto.SeckillOrderVO;
import com.example.mallu.seckill.entity.SeckillActivity;
import com.example.mallu.seckill.entity.SeckillGoods;
import com.example.mallu.seckill.entity.SeckillOrder;
import com.example.mallu.seckill.mapper.SeckillActivityMapper;
import com.example.mallu.seckill.mapper.SeckillGoodsMapper;
import com.example.mallu.seckill.mapper.SeckillOrderMapper;
import com.example.mallu.user.entity.Address;
import com.example.mallu.user.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillGoodsMapper goodsMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final SeckillRedisService seckillRedisService;
    private final SeckillAsyncService seckillAsyncService;

    /**
     * 秒杀落库开关：true=异步落库（默认），false=同步落库（压测基线对比用）
     */
    @Value("${seckill.async:true}")
    private boolean asyncEnabled;

    public List<SeckillActivity> listActivities() {
        return activityMapper.selectList();
    }

    public List<SeckillGoodsVO> listGoods(Long activityId) {
        List<SeckillGoodsVO> list = goodsMapper.selectByActivityId(activityId);
        for (SeckillGoodsVO vo : list) {
            Integer remaining = seckillRedisService.getRemainingStock(vo.getId());
            vo.setRemainingStock(remaining != null ? remaining : vo.getStock());
        }
        return list;
    }

    public SeckillGoodsVO goodsDetail(Long seckillGoodsId) {
        SeckillGoods goods = goodsMapper.selectById(seckillGoodsId);
        if (goods == null) {
            throw new BusinessException(ResultCode.SECKILL_GOODS_NOT_FOUND);
        }
        List<SeckillGoodsVO> list = goodsMapper.selectByActivityId(goods.getActivityId());
        SeckillGoodsVO vo = list.stream()
                .filter(item -> item.getId().equals(seckillGoodsId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.SECKILL_GOODS_NOT_FOUND));
        Integer remaining = seckillRedisService.getRemainingStock(vo.getId());
        vo.setRemainingStock(remaining != null ? remaining : vo.getStock());
        return vo;
    }

    @Transactional
    public SeckillOrderVO seckill(Long seckillGoodsId, Long addressId) {
        Long userId = UserContext.getUserId();
        LocalDateTime now = LocalDateTime.now();

        SeckillGoods goods = goodsMapper.selectById(seckillGoodsId);
        if (goods == null) {
            throw new BusinessException(ResultCode.SECKILL_GOODS_NOT_FOUND);
        }

        SeckillActivity activity = activityMapper.selectById(goods.getActivityId());
        if (activity == null || activity.getStatus() != 1
                || activity.getStartTime().isAfter(now) || activity.getEndTime().isBefore(now)) {
            throw new BusinessException(ResultCode.SECKILL_ACTIVITY_NOT_FOUND);
        }

        Product product = productMapper.selectById(goods.getProductId());
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }

        Address address = addressMapper.selectById(addressId, userId);
        if (address == null) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        // 预热库存：如果 Redis 中没有，则加载
        Integer remainingStock = seckillRedisService.getRemainingStock(seckillGoodsId);
        if (remainingStock == null) {
            seckillRedisService.preloadStock(seckillGoodsId, goods.getStock());
        }

        // Lua 原子扣减库存 + 防重复购买
        Long result = seckillRedisService.seckill(seckillGoodsId, userId);
        if (result == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀处理失败");
        }
        if (result == -1) {
            throw new BusinessException(ResultCode.SECKILL_STOCK_EMPTY);
        }
        if (result == -2) {
            throw new BusinessException(ResultCode.SECKILL_ALREADY_PURCHASED);
        }
        if (result == -3) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀库存未初始化");
        }

        // 扣减成功
        if (asyncEnabled) {
            // 异步模式：标记排队中，异步落库建订单，立即返回
            seckillRedisService.markResultProcessing(seckillGoodsId, userId);
            seckillAsyncService.createOrderAsync(seckillGoodsId, userId, address.getId());

            SeckillOrderVO vo = new SeckillOrderVO();
            vo.setSeckillGoodsId(seckillGoodsId);
            vo.setUserId(userId);
            vo.setStatus(0);
            vo.setPayAmount(goods.getSeckillPrice());
            return vo;
        }

        // 同步模式：直接落库建订单（性能基线，用于压测对比）
        try {
            SeckillOrder seckillOrder = seckillAsyncService.doCreateOrder(seckillGoodsId, userId, address.getId());
            Order order = orderMapper.selectById(seckillOrder.getOrderId(), userId);
            SeckillOrderVO vo = new SeckillOrderVO();
            vo.setId(seckillOrder.getId());
            vo.setSeckillGoodsId(seckillOrder.getSeckillGoodsId());
            vo.setOrderId(order != null ? order.getId() : null);
            vo.setOrderNo(order != null ? order.getOrderNo() : null);
            vo.setUserId(userId);
            vo.setStatus(seckillOrder.getStatus());
            vo.setPayAmount(order != null ? order.getPayAmount() : null);
            return vo;
        } catch (Exception e) {
            // 同步落库失败：恢复 Redis 库存与用户购买记录
            seckillRedisService.recoverStock(seckillGoodsId, userId);
            throw e;
        }
    }

    public SeckillOrderVO getResult(Long seckillOrderId) {
        Long userId = UserContext.getUserId();
        SeckillOrder seckillOrder = seckillOrderMapper.selectById(seckillOrderId);
        if (seckillOrder == null || !seckillOrder.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.SECKILL_ORDER_NOT_FOUND);
        }
        Order order = orderMapper.selectById(seckillOrder.getOrderId(), userId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setId(seckillOrder.getId());
        vo.setSeckillGoodsId(seckillOrder.getSeckillGoodsId());
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(userId);
        vo.setStatus(seckillOrder.getStatus());
        vo.setPayAmount(order.getPayAmount());
        return vo;
    }

    /**
     * 异步秒杀结果查询（轮询）：status=0 排队中；status=-1 失败；status=1 秒杀成功（待支付）
     */
    public SeckillOrderVO getResultByGoodsId(Long seckillGoodsId) {
        Long userId = UserContext.getUserId();
        Long result = seckillRedisService.getResult(seckillGoodsId, userId);
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setSeckillGoodsId(seckillGoodsId);
        vo.setUserId(userId);
        if (result == null) {
            vo.setStatus(0);
            return vo;
        }
        if (result == -1) {
            vo.setStatus(-1);
            return vo;
        }
        SeckillOrder seckillOrder = seckillOrderMapper.selectById(result);
        if (seckillOrder == null || !seckillOrder.getUserId().equals(userId)) {
            vo.setStatus(0);
            return vo;
        }
        Order order = orderMapper.selectById(seckillOrder.getOrderId(), userId);
        vo.setId(seckillOrder.getId());
        vo.setSeckillGoodsId(seckillOrder.getSeckillGoodsId());
        vo.setOrderId(order != null ? order.getId() : null);
        vo.setOrderNo(order != null ? order.getOrderNo() : null);
        vo.setPayAmount(order != null ? order.getPayAmount() : null);
        vo.setStatus(seckillOrder.getStatus());
        return vo;
    }

    @Transactional
    public void cancelTimeoutSeckillOrder(Long seckillOrderId) {
        SeckillOrder seckillOrder = seckillOrderMapper.selectById(seckillOrderId);
        if (seckillOrder == null || seckillOrder.getStatus() != 1) {
            return;
        }
        Order order = orderMapper.selectById(seckillOrder.getOrderId(), seckillOrder.getUserId());
        if (order == null || order.getStatus() != 0) {
            return;
        }
        Long userId = seckillOrder.getUserId();

        // 取消普通订单
        order.setStatus(2);
        orderMapper.updateStatus(order);

        // 回滚普通商品库存
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }

        // 回滚秒杀库存和 Redis
        goodsMapper.increaseStock(seckillOrder.getSeckillGoodsId(), 1);
        seckillRedisService.recoverStock(seckillOrder.getSeckillGoodsId(), userId);

        // 更新秒杀订单状态为已取消
        seckillOrder.setStatus(3);
        seckillOrderMapper.updateStatus(seckillOrder);
    }

    private String generateOrderNo() {
        return "S" + System.currentTimeMillis() + String.format("%06d", (int) (Math.random() * 1000000));
    }
}
