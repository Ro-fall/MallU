package com.rofall.mallu.order;

import com.rofall.mallu.coupon.Coupon;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OrderAmountCalculatorTest {
    @Test
    void appliesFullReductionOnlyAfterThreshold() {
        Coupon coupon = mock(Coupon.class);
        when(coupon.getType()).thenReturn("FULL_REDUCTION");
        when(coupon.getThresholdAmount()).thenReturn(new BigDecimal("1000.00"));
        when(coupon.getValue()).thenReturn(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), OrderAmountCalculator.discount(new BigDecimal("1200.00"), coupon));
        assertEquals(BigDecimal.ZERO, OrderAmountCalculator.discount(new BigDecimal("999.00"), coupon));
    }

    @Test
    void calculatesDiscountCouponWithTwoDecimalPlaces() {
        Coupon coupon = mock(Coupon.class);
        when(coupon.getType()).thenReturn("DISCOUNT");
        when(coupon.getValue()).thenReturn(new BigDecimal("0.90"));
        assertEquals(new BigDecimal("10.00"), OrderAmountCalculator.discount(new BigDecimal("100.00"), coupon));
    }
}
