package com.example.mallu.user.controller;

import com.example.mallu.common.result.Result;
import com.example.mallu.user.dto.AddressDTO;
import com.example.mallu.user.dto.AddressVO;
import com.example.mallu.user.service.AddressService;
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
    public Result<AddressVO> add(@RequestBody @Valid AddressDTO addressDTO) {
        return Result.success(addressService.addAddress(addressDTO));
    }

    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable Long id, @RequestBody @Valid AddressDTO addressDTO) {
        return Result.success(addressService.updateAddress(id, addressDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return Result.success();
    }

    @GetMapping
    public Result<List<AddressVO>> list() {
        return Result.success(addressService.listAddresses());
    }

    @GetMapping("/{id}")
    public Result<AddressVO> get(@PathVariable Long id) {
        return Result.success(addressService.getAddress(id));
    }

    @PutMapping("/{id}/default")
    public Result<AddressVO> setDefault(@PathVariable Long id) {
        return Result.success(addressService.setDefaultAddress(id));
    }
}
