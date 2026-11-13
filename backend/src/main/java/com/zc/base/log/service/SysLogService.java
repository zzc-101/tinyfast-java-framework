package com.zc.base.log.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.zc.base.common.core.PageQuery;
import com.zc.base.common.core.PageResult;
import com.zc.base.log.entity.SysLoginLog;
import com.zc.base.log.entity.SysOperationLog;
import com.zc.base.log.mapper.SysLoginLogMapper;
import com.zc.base.log.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.zc.base.log.entity.table.SysOperationLogTableDef.SYS_OPERATION_LOG;
import static com.zc.base.log.entity.table.SysLoginLogTableDef.SYS_LOGIN_LOG;

@Service
@RequiredArgsConstructor
public class SysLogService {

    private final SysOperationLogMapper operationLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    /**
     * 操作日志分页查询
     */
    public PageResult<SysOperationLog> pageOperationLog(PageQuery query, String module, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_OPERATION_LOG.MODULE.like(module, StrUtil.isNotBlank(module)))
                .and(SYS_OPERATION_LOG.STATUS.eq(status, status != null))
                .orderBy(SYS_OPERATION_LOG.CREATE_TIME.desc());

        Page<SysOperationLog> page = operationLogMapper.paginate(
                Page.of(query.getPageNum(), query.getPageSize()), wrapper);

        return new PageResult<>(page.getTotalRow(), page.getRecords(),
                (int) page.getPageNumber(), (int) page.getPageSize());
    }

    /**
     * 登录日志分页查询
     */
    public PageResult<SysLoginLog> pageLoginLog(PageQuery query, String username, Integer status) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SYS_LOGIN_LOG.USERNAME.like(username, StrUtil.isNotBlank(username)))
                .and(SYS_LOGIN_LOG.STATUS.eq(status, status != null))
                .orderBy(SYS_LOGIN_LOG.CREATE_TIME.desc());

        Page<SysLoginLog> page = loginLogMapper.paginate(
                Page.of(query.getPageNum(), query.getPageSize()), wrapper);

        return new PageResult<>(page.getTotalRow(), page.getRecords(),
                (int) page.getPageNumber(), (int) page.getPageSize());
    }

    /**
     * 记录登录日志
     */
    public void recordLoginLog(SysLoginLog loginLog) {
        loginLogMapper.insert(loginLog);
    }

    /**
     * 清空操作日志
     */
    public void clearOperationLog() {
        operationLogMapper.deleteByCondition(SYS_OPERATION_LOG.ID.isNotNull());
    }

    /**
     * 清空登录日志
     */
    public void clearLoginLog() {
        loginLogMapper.deleteByCondition(SYS_LOGIN_LOG.ID.isNotNull());
    }

}
