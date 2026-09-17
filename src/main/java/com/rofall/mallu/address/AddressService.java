package com.rofall.mallu.address;

import com.rofall.mallu.common.BusinessException;
import com.rofall.mallu.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;

    @Transactional
    public AddressResponse create(AddressRequest request) {
        Long userId = UserContext.requireUserId();
        if (request.defaultAddress()) {
            addressRepository.findByUserIdAndDefaultAddressTrue(userId).forEach(Address::clearDefault);
        }
        return AddressResponse.from(addressRepository.save(new Address(userId, request)));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list() {
        return addressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(UserContext.requireUserId()).stream()
                .map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse update(Long addressId, AddressRequest request) {
        Long userId = UserContext.requireUserId();
        Address address = owned(addressId, userId);
        if (request.defaultAddress()) {
            addressRepository.findByUserIdAndDefaultAddressTrue(userId).stream()
                    .filter(item -> !item.getId().equals(addressId)).forEach(Address::clearDefault);
        }
        address.update(request);
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long addressId) {
        Long userId = UserContext.requireUserId();
        addressRepository.delete(owned(addressId, userId));
    }

    public Address owned(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new BusinessException(4043, HttpStatus.NOT_FOUND, "收货地址不存在"));
    }
}
