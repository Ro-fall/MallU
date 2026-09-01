package com.example.mallu.common.mybatis;

import com.example.mallu.common.interceptor.UserContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AutoFillInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        SqlCommandType commandType = mappedStatement.getSqlCommandType();
        if (commandType == SqlCommandType.INSERT || commandType == SqlCommandType.UPDATE) {
            fillParameter(parameter, commandType);
        }

        return invocation.proceed();
    }

    private void fillParameter(Object parameter, SqlCommandType commandType) {
        if (parameter == null) {
            return;
        }

        if (parameter instanceof BaseEntity) {
            fillEntity((BaseEntity) parameter, commandType);
        } else if (parameter instanceof Collection<?>) {
            ((Collection<?>) parameter).forEach(item -> fillParameter(item, commandType));
        } else if (parameter instanceof Map) {
            ((Map<?, ?>) parameter).values().forEach(value -> fillParameter(value, commandType));
        }
    }

    private void fillEntity(BaseEntity entity, SqlCommandType commandType) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = UserContext.getUserId();

        if (commandType == SqlCommandType.INSERT) {
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setCreateBy(userId);
            entity.setUpdateBy(userId);
        } else if (commandType == SqlCommandType.UPDATE) {
            entity.setUpdatedAt(now);
            entity.setUpdateBy(userId);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
