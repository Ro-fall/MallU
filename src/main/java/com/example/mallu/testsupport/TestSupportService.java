package com.example.mallu.testsupport;

import com.example.mallu.common.exception.BusinessException;
import com.example.mallu.common.result.ResultCode;
import com.example.mallu.user.dto.LoginVO;
import com.example.mallu.user.dto.UserRegisterDTO;
import com.example.mallu.user.entity.Address;
import com.example.mallu.user.mapper.AddressMapper;
import com.example.mallu.user.mapper.UserMapper;
import com.example.mallu.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TestSupportService {

    private static final String TEST_PREFIX = "tc_";
    private static final String PASSWORD = "Test@123";
    private final UserService userService;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;

    @Transactional
    public Map<String, Object> createSession() {
        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100000, 999999);
        String username = TEST_PREFIX + suffix;
        String phone = "13" + ThreadLocalRandom.current().nextInt(100000000, 999999999);

        UserRegisterDTO registration = new UserRegisterDTO();
        registration.setUsername(username);
        registration.setPassword(PASSWORD);
        registration.setPhone(phone);
        registration.setEmail(username + "@test.local");
        LoginVO login = userService.register(registration);

        LocalDateTime now = LocalDateTime.now();
        Address address = new Address();
        address.setUserId(login.getUser().getId());
        address.setReceiverName("自动化测试用户");
        address.setPhone(phone);
        address.setProvince("上海市");
        address.setCity("上海市");
        address.setDistrict("浦东新区");
        address.setDetailAddress("测试大道 100 号");
        address.setIsDefault(1);
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        address.setCreateBy(login.getUser().getId());
        address.setUpdateBy(login.getUser().getId());
        addressMapper.insert(address);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", login.getUser().getId());
        result.put("username", username);
        result.put("password", PASSWORD);
        result.put("phone", phone);
        result.put("email", registration.getEmail());
        result.put("token", login.getToken());
        result.put("addressId", address.getId());
        result.put("note", "该会话由测试控制台生成，可一键清理。");
        return result;
    }

    @Transactional
    public void cleanupSession(Long userId, String username) {
        if (username == null || !username.startsWith(TEST_PREFIX)
                || userMapper.deleteTestUserById(userId, username) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "测试会话不存在，或不是由测试控制台创建");
        }
    }
}