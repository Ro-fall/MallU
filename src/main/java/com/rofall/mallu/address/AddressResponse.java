package com.rofall.mallu.address;

public record AddressResponse(Long id, String receiverName, String phone, String province,
                              String city, String district, String detail, boolean defaultAddress) {
    static AddressResponse from(Address address) {
        return new AddressResponse(address.getId(), address.getReceiverName(), address.getPhone(), address.getProvince(),
                address.getCity(), address.getDistrict(), address.getDetail(), address.getDefaultAddress());
    }
}
