package com.zc.base.security.event;

/**
 * 登录结果事件，由日志模块监听并写入登录日志
 *
 * @param username  登录用户名
 * @param ip        客户端 IP
 * @param userAgent 请求头 User-Agent
 * @param success   是否登录成功
 * @param message   提示消息
 */
public record LoginEvent(String username, String ip, String userAgent, boolean success, String message) {
}
