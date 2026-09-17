package com.rofall.mallu.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddressRequest(
        @NotBlank String receiverName,
        @NotBlank @Pattern(regexp = "[0-9+ -]{6,20}") String phone,
        @NotBlank String province,
        @NotBlank String city,
        @NotBlank String district,
        @NotBlank String detail,
        boolean defaultAddress
) {
}
