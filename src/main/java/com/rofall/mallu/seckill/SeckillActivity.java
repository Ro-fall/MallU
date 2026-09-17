package com.rofall.mallu.seckill;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name = "seckill_activity") @Getter @NoArgsConstructor
public class SeckillActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(name = "start_time", nullable = false) private LocalDateTime startTime;
    @Column(name = "end_time", nullable = false) private LocalDateTime endTime;
    @Column(nullable = false) private Boolean status;
    public boolean activeNow() { LocalDateTime now = LocalDateTime.now(); return Boolean.TRUE.equals(status) && !now.isBefore(startTime) && !now.isAfter(endTime); }
}
