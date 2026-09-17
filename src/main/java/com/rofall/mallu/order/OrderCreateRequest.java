package com.rofall.mallu.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreateRequest(@NotNull Long addressId, Long userCouponId, @NotEmpty List<@NotNull Long> cartItemIds) { }
