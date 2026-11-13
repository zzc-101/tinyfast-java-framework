package com.zc.base.log.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("sys_operation_log")
public class SysOperationLog implements Serializable {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 操作模块 */
    private String module;

    /** 操作描述 */
    private String description;

    /** 请求方式 */
    private String requestMethod;

    /** 请求URL */
    private String requestUrl;

    /** 请求参数 */
    private String requestParams;

    /** 响应结果 */
    private String responseBody;

    /** 操作状态(1:成功 0:失败) */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时(毫秒) */
    private Long costTime;

    /** 操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作人IP */
    private String operatorIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
