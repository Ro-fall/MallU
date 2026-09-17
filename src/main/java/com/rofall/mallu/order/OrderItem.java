package com.rofall.mallu.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "product_name", nullable = false) private String productName;
    @Column(name = "product_price", nullable = false) private BigDecimal productPrice;
    @Column(nullable = false) private Integer quantity;
    @Column(name = "total_amount", nullable = false) private BigDecimal totalAmount;
    public OrderItem(Long orderId, Long productId, String productName, BigDecimal productPrice, int quantity) {
        this.orderId = orderId; this.productId = productId; this.productName = productName; this.productPrice = productPrice;
        this.quantity = quantity; this.totalAmount = productPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
