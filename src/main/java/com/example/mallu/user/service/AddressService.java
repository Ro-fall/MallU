package com.example.mallu.user.service;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.interceptor.UserContext;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.user.dto.AddressDTO;
import com.example.mallu.user.dto.AddressVO;
import com.example.mallu.user.entity.Address;
import com.example.mallu.user.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressMapper addressMapper;
    private static final int MAX_ADDRESS_COUNT = 10;

    @Transactional
    public AddressVO addAddress(AddressDTO addressDTO) {
        Long userId = UserContext.getUserId();
        if (addressMapper.countByUserId(userId) >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ResultCode.ADDRESS_LIMIT_EXCEEDED);
        }

        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setUserId(userId);

        if (address.getIsDefault() == 1) {
            addressMapper.clearDefaultByUserId(userId);
        }

        addressMapper.insert(address);
        return toAddressVO(address);
    }

    @Transactional
    public AddressVO updateAddress(Long id, AddressDTO addressDTO) {
        Long userId = UserContext.getUserId();
        Address exist = addressMapper.selectById(id, userId);
        if (exist == null) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }

        Address address = new Address();
        BeanUtils.copyProperties(addressDTO, address);
        address.setId(id);
        address.setUserId(userId);

        if (address.getIsDefault() == 1) {
            addressMapper.clearDefaultByUserId(userId);
        }

        addressMapper.updateById(address);
        return toAddressVO(address);
    }

    public void deleteAddress(Long id) {
        int affected = addressMapper.deleteById(id, UserContext.getUserId());
        if (affected == 0) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }
    }

    public List<AddressVO> listAddresses() {
        return addressMapper.selectByUserId(UserContext.getUserId())
                .stream()
                .map(this::toAddressVO)
                .toList();
    }

    public AddressVO getAddress(Long id) {
        Address address = addressMapper.selectById(id, UserContext.getUserId());
        if (address == null) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }
        return toAddressVO(address);
    }

    @Transactional
    public AddressVO setDefaultAddress(Long id) {
        Long userId = UserContext.getUserId();
        Address address = addressMapper.selectById(id, userId);
        if (address == null) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_FOUND);
        }
        addressMapper.clearDefaultByUserId(userId);
        address.setIsDefault(1);
        addressMapper.updateById(address);
        return toAddressVO(address);
    }

    private AddressVO toAddressVO(Address address) {
        AddressVO addressVO = new AddressVO();
        BeanUtils.copyProperties(address, addressVO);
        return addressVO;
    }
}
