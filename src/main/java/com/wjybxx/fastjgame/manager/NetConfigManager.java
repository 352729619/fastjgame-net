/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.utils.ConfigLoader;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 网络层配置管理器，不做热加载，网络层本身不需要经常修改，而且很多配置也无法热加载。
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 15:09
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class NetConfigManager  {

    /**
     * 网络包配置文件名字
     */
    private static final String NET_CONFIG_NAME = "net_config.properties";
    /** 原始配置文件 */
    private final ConfigWrapper configWrapper;

    /** 帧间隔 */
    private final int frameInterval;

    // 参数含义及单位见get方法或配置文件
    private final byte[] tokenKeyBytes;
    private final int tokenForbiddenTimeout;

    private final int maxIoThreadNum;
    private final int maxFrameLength;
    private final int sndBufferAsServer;
    private final int revBufferAsServer;
    private final int sndBufferAsClient;
    private final int revBufferAsClient;

    private final int connectMaxTryTimes;
    private final int connectTimeout;
    private final int waitTokenResultTimeout;
    private final int loginTokenTimeout;
    private final int ackTimeout;
    private final int sessionTimeout;

    private final int serverMaxCacheNum;
    private final int clientMaxCacheNum;
    private final int flushThreshold;

    private final int httpRequestTimeout;
    private final int httpSessionTimeout;

    private final int rpcCallbackTimeoutMs;
    private final int syncRpcTimeoutMs;

    @Inject
    public NetConfigManager() throws IOException {
        configWrapper = ConfigLoader.loadConfig(NetConfigManager.class.getClassLoader(), NET_CONFIG_NAME);

        frameInterval = configWrapper.getAsInt("frameInterval");

        tokenKeyBytes = configWrapper.getAsString("tokenKey").getBytes(StandardCharsets.UTF_8);
        tokenForbiddenTimeout = configWrapper.getAsInt("tokenForbiddenTimeout",3600);

        maxIoThreadNum = configWrapper.getAsInt("maxIoThreadNum");
        maxFrameLength = configWrapper.getAsInt("maxFrameLength");
        sndBufferAsServer = configWrapper.getAsInt("sndBufferAsServer");
        revBufferAsServer = configWrapper.getAsInt("revBufferAsServer");
        sndBufferAsClient = configWrapper.getAsInt("sndBufferAsClient");
        revBufferAsClient = configWrapper.getAsInt("revBufferAsClient");

        serverMaxCacheNum = configWrapper.getAsInt("serverMaxCacheNum");
        clientMaxCacheNum = configWrapper.getAsInt("clientMaxCacheNum");
        flushThreshold = configWrapper.getAsInt("flushThreshold", 20);

        connectMaxTryTimes = configWrapper.getAsInt("connectMaxTryTimes");
        connectTimeout = configWrapper.getAsInt("connectTimeout");
        waitTokenResultTimeout = configWrapper.getAsInt("waitTokenResultTimeout");
        loginTokenTimeout = configWrapper.getAsInt("loginTokenTimeout");
        ackTimeout = configWrapper.getAsInt("ackTimeout");
        sessionTimeout = configWrapper.getAsInt("sessionTimeout");


        httpRequestTimeout = configWrapper.getAsInt("httpRequestTimeout");
        httpSessionTimeout = configWrapper.getAsInt("httpSessionTimeout");

        rpcCallbackTimeoutMs = configWrapper.getAsInt("rpcCallbackTimeoutMs");
        syncRpcTimeoutMs = configWrapper.getAsInt("syncRpcTimeoutMs");
    }

    public int frameInterval() {
        return frameInterval;
    }

    /**
     * 获取原始的config保证其，以获取不在本类中定义的属性
     */
    public ConfigWrapper properties() {
        return configWrapper;
    }

    /**
     * 用于默认的异或加密token的秘钥
     */
    public byte[] getTokenKeyBytes(){
        return tokenKeyBytes;
    }
    /**
     * netty IO 线程数量
     */
    public int maxIoThreadNum() {
        return maxIoThreadNum;
    }
    /**
     * 最大帧长度
     */
    public int maxFrameLength(){
        return maxFrameLength;
    }

    /**
     * 作为服务器时的发送缓冲区
     */
    public int sndBufferAsServer(){
        return sndBufferAsServer;
    }
    /**
     * 作为服务器时的接收缓冲区
     */
    public int revBufferAsServer(){
        return revBufferAsServer;
    }
    /**
     * 作为客户端时的发送缓冲区
     * @return
     */
    public int sndBufferAsClient(){
        return sndBufferAsClient;
    }
    /**
     * 作为客户端时的接收缓冲区
     */
    public int revBufferAsClient(){
        return revBufferAsClient;
    }

    /**
     * 获取服务器最大可缓存消息数
     * @return
     */
    public int serverMaxCacheNum(){
        return serverMaxCacheNum;
    }

    /**
     * 获取客户端最大可缓存消息数
     */
    public int clientMaxCacheNum(){
        return clientMaxCacheNum;
    }

    /**
     * 异步通信会话超时时间(秒)
     */
    public int sessionTimeout(){
        return sessionTimeout;
    }

    /**
     * 获取Token登录超时时间(秒，登录Token时效性)
     */
    public int loginTokenTimeout() {
        return loginTokenTimeout;
    }

    /**
     * 最大重连尝试次数(连接状态下尝试连接次数)
     */
    public int connectMaxTryTimes() {
        return connectMaxTryTimes;
    }

    /**
     * 异步建立连接超时时间(毫秒)
     */
    public long connectTimeout() {
        return connectTimeout;
    }

    /**
     * 等待token验证结果超时时间，需要适当的长一点(毫秒)
     */
    public long waitTokenResultTimeout(){
        return waitTokenResultTimeout;
    }

    /**
     * 异步通信中ack超时时间(毫秒)
     */
    public long ackTimeout() {
        return ackTimeout;
    }

    /**
     * okHttpClient请求超时时间(秒)
     */
    public int httpRequestTimeout(){
        return httpRequestTimeout;
    }

    /**
     * http会话超时时间(秒)
     * 此外，它也是检查session超时的间隔
     */
    public int httpSessionTimeout(){
        return httpSessionTimeout;
    }

    /** token被禁用的超时时间(秒) */
    public int tokenForbiddenTimeout(){
        return tokenForbiddenTimeout;
    }

    /** rpc异步回调超时时间(毫秒) */
    public int rpcCallbackTimeoutMs() {
        return rpcCallbackTimeoutMs;
    }

    /** 同步rpc调用超时时间(毫秒) */
    public int syncRpcTimeoutMs() {
        return syncRpcTimeoutMs;
    }

    /** 当缓存的消息数到达该值时，立即清空缓冲区 */
    public int flushThreshold() {
        return flushThreshold;
    }
}
