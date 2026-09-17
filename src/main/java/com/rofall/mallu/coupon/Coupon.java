package com.rofall.mallu.coupon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@NoArgsConstructor
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String type;
    @Column(name = "threshold_amount") private BigDecimal thresholdAmount;
    @Column(nullable = false) private BigDecimal value;
    @Column(name = "start_time", nullable = false) private LocalDateTime startTime;
    @Column(name = "end_time", nullable = false) private LocalDateTime endTime;
    @Column(name = "remaining_count", nullable = false) private Integer remainingCount;
    @Column(nullable = false) private Boolean status;

    public boolean canClaim(LocalDateTime now) {
        return validNow(now) && remainingCount > 0;
    }

    public boolean validNow(LocalDateTime now) { return Boolean.TRUE.equals(status) && !now.isBefore(startTime) && !now.isAfter(endTime); }

    public void decreaseRemaining() {
        if (remainingCount <= 0) throw new IllegalStateException("优惠券余量不足");
        remainingCount--;
    }
}
