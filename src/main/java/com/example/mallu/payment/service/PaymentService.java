package com.example.mallu.payment.service;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.order.entity.Order;
import com.example.mallu.order.mapper.OrderMapper;
import com.example.mallu.payment.dto.PaymentCallbackDTO;
import com.example.mallu.payment.dto.PaymentCallbackVO;
import com.example.mallu.seckill.entity.SeckillOrder;
import com.example.mallu.seckill.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TRADE_KEY_PREFIX = "pay:trade:";
    private static final long TRADE_KEY_EXPIRE_MINUTES = 10;

    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public PaymentCallbackVO handleCallback(PaymentCallbackDTO dto) {
        // 1. 幂等：同一 tradeNo 只处理一次
        String tradeKey = TRADE_KEY_PREFIX + dto.getTradeNo();
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(tradeKey, dto.getOrderNo(), TRADE_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(first)) {
            log.info("支付回调重复请求被忽略, tradeNo={}", dto.getTradeNo());
            throw new BusinessException(ResultCode.PAY_TRADE_EXISTS);
        }

        // 2. 校验订单存在
        Order order = orderMapper.selectByOrderNoForSystem(dto.getOrderNo());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        // 3. 支付失败回调：不修改订单状态，直接返回
        PaymentCallbackVO vo = new PaymentCallbackVO();
        vo.setOrderNo(dto.getOrderNo());
        vo.setTradeNo(dto.getTradeNo());
        vo.setOrderStatus(order.getStatus());
        if (!dto.isSuccess()) {
            vo.setSuccess(false);
            vo.setMessage("支付失败，订单状态不变");
            return vo;
        }

        // 4. 校验金额一致，防止回调篡改
        if (order.getPayAmount().compareTo(dto.getPayAmount()) != 0) {
            throw new BusinessException(ResultCode.PAY_AMOUNT_NOT_MATCH,
                    "订单金额 " + order.getPayAmount() + " 与回调金额 " + dto.getPayAmount() + " 不一致");
        }

        // 5. 订单已是支付状态则幂等返回（避免重复处理）
        if (order.getStatus() == 1) {
            vo.setSuccess(true);
            vo.setMessage("订单已支付，重复回调幂等返回");
            return vo;
        }

        // 6. 只有待支付订单能完成支付
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
        }

        // 7. 更新普通订单为已支付
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateStatus(order);

        // 8. 如果是秒杀订单，同步更新秒杀订单状态
        SeckillOrder seckillOrder = seckillOrderMapper.selectByOrderId(order.getId());
        if (seckillOrder != null) {
            seckillOrder.setStatus(2);
            seckillOrderMapper.updateStatus(seckillOrder);
        }

        vo.setSuccess(true);
        vo.setOrderStatus(1);
        vo.setMessage("支付成功");
        log.info("支付回调成功, orderNo={}, tradeNo={}, amount={}", order.getOrderNo(), dto.getTradeNo(), dto.getPayAmount());
        return vo;
    }
}
