package com.rofall.mallu.address;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "address")
@Getter
@NoArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "receiver_name", nullable = false)
    private String receiverName;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String province;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String district;
    @Column(name = "detail", nullable = false)
    private String detail;
    @Column(name = "is_default", nullable = false)
    private Boolean defaultAddress;

    public Address(Long userId, AddressRequest request) {
        this.userId = userId;
        this.receiverName = request.receiverName();
        this.phone = request.phone();
        this.province = request.province();
        this.city = request.city();
        this.district = request.district();
        this.detail = request.detail();
        this.defaultAddress = request.defaultAddress();
    }

    public void update(AddressRequest request) {
        this.receiverName = request.receiverName();
        this.phone = request.phone();
        this.province = request.province();
        this.city = request.city();
        this.district = request.district();
        this.detail = request.detail();
        this.defaultAddress = request.defaultAddress();
    }

    public void clearDefault() {
        this.defaultAddress = false;
    }
}
