package com.example.mallu.payment.service;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.order.entity.Order;
import com.example.mallu.order.mapper.OrderMapper;
import com.example.mallu.payment.dto.PaymentCallbackDTO;
import com.example.mallu.payment.dto.PaymentCallbackVO;
import com.example.mallu.payment.entity.PaymentFlow;
import com.example.mallu.payment.mapper.PaymentFlowMapper;
import com.example.mallu.seckill.entity.SeckillOrder;
import com.example.mallu.seckill.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final PaymentFlowMapper paymentFlowMapper;

    @Transactional
    public PaymentCallbackVO handleCallback(PaymentCallbackDTO dto) {
        PaymentFlow existingFlow = paymentFlowMapper.selectByTradeNo(dto.getTradeNo());
        if (existingFlow != null) {
            return buildIdempotentResponse(existingFlow, dto);
        }

        // 2. 校验订单存在
        Order order = orderMapper.selectByOrderNoForSystem(dto.getOrderNo());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        // 3. 无效请求不占用流水号；有效回调先落库，保证重启后仍可幂等。
        if (order.getPayAmount().compareTo(dto.getPayAmount()) != 0) {
            throw new BusinessException(ResultCode.PAY_AMOUNT_NOT_MATCH,
                    "订单金额 " + order.getPayAmount() + " 与回调金额 " + dto.getPayAmount() + " 不一致");
        }
        PaymentFlow flow = new PaymentFlow();
        flow.setTradeNo(dto.getTradeNo());
        flow.setOrderNo(order.getOrderNo());
        flow.setOrderId(order.getId());
        flow.setPayAmount(dto.getPayAmount());
        flow.setResult(dto.getResult().toUpperCase());
        if (paymentFlowMapper.insertIgnore(flow) == 0) {
            return buildIdempotentResponse(paymentFlowMapper.selectByTradeNo(dto.getTradeNo()), dto);
        }

        // 4. 支付失败回调：记录流水，不修改订单状态。
        PaymentCallbackVO vo = new PaymentCallbackVO();
        vo.setOrderNo(dto.getOrderNo());
        vo.setTradeNo(dto.getTradeNo());
        vo.setOrderStatus(order.getStatus());
        if (!dto.isSuccess()) {
            vo.setSuccess(false);
            vo.setIdempotent(false);
            vo.setMessage("支付失败，订单状态不变");
            return vo;
        }

        // 5. 条件更新，避免支付与取消并发时相互覆盖。
        if (order.getStatus() != 0 || orderMapper.updateStatusIfExpected(
                order.getId(), order.getUserId(), 0, 1, LocalDateTime.now()) == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
        }

        // 6. 如果是秒杀订单，同步更新秒杀订单状态
        SeckillOrder seckillOrder = seckillOrderMapper.selectByOrderId(order.getId());
        if (seckillOrder != null) {
            seckillOrder.setStatus(2);
            seckillOrderMapper.updateStatus(seckillOrder);
        }

        vo.setSuccess(true);
        vo.setIdempotent(false);
        vo.setOrderStatus(1);
        vo.setMessage("支付成功");
        log.info("支付回调成功, orderNo={}, tradeNo={}, amount={}", order.getOrderNo(), dto.getTradeNo(), dto.getPayAmount());
        return vo;
    }

    private PaymentCallbackVO buildIdempotentResponse(PaymentFlow flow, PaymentCallbackDTO dto) {
        if (flow == null || !flow.getOrderNo().equals(dto.getOrderNo())
                || flow.getPayAmount().compareTo(dto.getPayAmount()) != 0
                || !flow.getResult().equalsIgnoreCase(dto.getResult())) {
            throw new BusinessException(ResultCode.PAY_TRADE_EXISTS, "支付流水号已被另一笔回调使用");
        }
        Order order = orderMapper.selectByOrderNoForSystem(flow.getOrderNo());
        PaymentCallbackVO vo = new PaymentCallbackVO();
        vo.setOrderNo(flow.getOrderNo());
        vo.setTradeNo(flow.getTradeNo());
        vo.setSuccess("SUCCESS".equalsIgnoreCase(flow.getResult()));
        vo.setIdempotent(true);
        vo.setOrderStatus(order == null ? null : order.getStatus());
        vo.setMessage("重复回调已按首次处理结果返回");
        return vo;
    }
}
