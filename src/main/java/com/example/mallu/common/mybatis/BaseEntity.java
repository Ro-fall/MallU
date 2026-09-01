package com.example.mallu.common.mybatis;

import java.time.LocalDateTime;

public interface BaseEntity {

    LocalDateTime getCreatedAt();

    void setCreatedAt(LocalDateTime createdAt);

    LocalDateTime getUpdatedAt();

    void setUpdatedAt(LocalDateTime updatedAt);

    Long getCreateBy();

    void setCreateBy(Long createBy);

    Long getUpdateBy();

    void setUpdateBy(Long updateBy);
}
