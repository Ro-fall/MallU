package com.example.mallu.payment.mapper;

import com.example.mallu.payment.entity.PaymentFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentFlowMapper {
    PaymentFlow selectByTradeNo(@Param("tradeNo") String tradeNo);

    /** INSERT IGNORE 使并发重复回调能正常返回首次结果。 */
    int insertIgnore(PaymentFlow paymentFlow);
}
