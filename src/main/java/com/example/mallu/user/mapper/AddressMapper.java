package com.example.mallu.user.mapper;

import com.example.mallu.user.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AddressMapper {

    int insert(Address address);

    int updateById(Address address);

    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    Address selectById(@Param("id") Long id, @Param("userId") Long userId);

    List<Address> selectByUserId(Long userId);

    int countByUserId(Long userId);

    int clearDefaultByUserId(Long userId);
}
