package com.zc.base.common.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import com.zc.base.security.util.SecurityUtil;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 全局配置
 * - 自动填充 createBy / updateBy
 * - 审计日志（开发环境打印SQL耗时）
 */
@Configuration
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // INSERT 时自动填充 createBy
        globalConfig.registerInsertListener(new CreateByInsertListener(),
                com.zc.base.common.core.BaseEntity.class);

        // UPDATE 时自动填充 updateBy
        globalConfig.registerUpdateListener(new UpdateByUpdateListener(),
                com.zc.base.common.core.BaseEntity.class);
    }

    /**
     * 插入时自动设置 createBy
     */
    public static class CreateByInsertListener implements com.mybatisflex.annotation.InsertListener {
        @Override
        public void onInsert(Object entity) {
            if (entity instanceof com.zc.base.common.core.BaseEntity baseEntity) {
                if (baseEntity.getCreateBy() == null) {
                    SecurityUtil.getCurrentUserId().ifPresent(baseEntity::setCreateBy);
                }
            }
        }
    }

    /**
     * 更新时自动设置 updateBy
     */
    public static class UpdateByUpdateListener implements com.mybatisflex.annotation.UpdateListener {
        @Override
        public void onUpdate(Object entity) {
            if (entity instanceof com.zc.base.common.core.BaseEntity baseEntity) {
                SecurityUtil.getCurrentUserId().ifPresent(baseEntity::setUpdateBy);
            }
        }
    }

}
