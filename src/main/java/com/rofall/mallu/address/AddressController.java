package com.rofall.mallu.address;

import com.rofall.mallu.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @PostMapping
    public ApiResponse<AddressResponse> create(@RequestBody @Valid AddressRequest request) {
        return ApiResponse.success(addressService.create(request));
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list() {
        return ApiResponse.success(addressService.list());
    }

    @PutMapping("/{addressId}")
    public ApiResponse<AddressResponse> update(@PathVariable Long addressId, @RequestBody @Valid AddressRequest request) {
        return ApiResponse.success(addressService.update(addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> delete(@PathVariable Long addressId) {
        addressService.delete(addressId);
        return ApiResponse.success();
    }
}
