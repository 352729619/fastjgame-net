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
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.IntSequencer;
import com.wjybxx.fastjgame.misc.LongSequencer;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerFactory;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.FastCollectionsUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.Consumer;

/**
 * 客户端到服务器的会话控制器
 * (我发起的连接)
 *
 * 客户端什么时候断开连接？
 * 1.服务器通知验证失败(服务器让我移除)
 * 2.外部调用{@link #removeSession(long, long, String)}
 * 3.消息缓存数超过限制
 * 4.限定时间内无法连接到服务器
 * 5.验证结果表明服务器的sequence和ack异常时。
 *
 * 什么时候会关闭channel？
 * {@link #removeSession(long, long, String)}
 * {@link C2SSessionState#closeChannel()}
 * {@link ConnectedState#reconnect(String)}
 *
 * 该管理器不是线程安全的！只能由NetWorld调用。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:10
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class C2SSessionManager implements SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(C2SSessionManager.class);

    private NetManagerWrapper managerWrapper;
    private final NetConfigManager netConfigManager;
    private final AcceptorManager acceptorManager;
    private final NetTimeManager netTimeManager;
    private final TokenManager tokenManager;
    /** 所有用户的会话信息 */
    private final Long2ObjectMap<UserInfo> userInfoMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public C2SSessionManager(NetConfigManager netConfigManager, AcceptorManager acceptorManager,
                             NetTimeManager netTimeManager, TokenManager tokenManager) {
        this.netConfigManager = netConfigManager;
        this.acceptorManager = acceptorManager;
        this.netTimeManager = netTimeManager;
        this.tokenManager = tokenManager;
    }

    /** 解决循环依赖 */
    public void setManagerWrapper(NetManagerWrapper managerWrapper) {
        this.managerWrapper = managerWrapper;
    }

    public void tick(){
        for (UserInfo userInfo : userInfoMap.values()) {
            for (SessionWrapper sessionWrapper: userInfo.sessionWrapperMap.values()){
                // 状态机刷帧
                if (sessionWrapper.getState() != null){
                    sessionWrapper.getState().execute();
                }
                // 检测超时的rpc调用
                FastCollectionsUtils.removeIfAndThen(sessionWrapper.getRpcPromiseMap(),
                        (k, rpcPromiseInfo) -> netTimeManager.getSystemMillTime() >= rpcPromiseInfo.timeoutMs,
                        (k, rpcPromiseInfo) -> rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.TIMEOUT));
            }
        }
    }

    /**
     * 获取session
     * @param localGuid 对应的本地用户标识
     * @param serverGuid 服务器guid
     * @return 如果存在则返回对应的session，否则返回null
     */
    @Nullable
    private SessionWrapper getSessionWrapper(long localGuid, long serverGuid){
        UserInfo userInfo = userInfoMap.get(localGuid);
        if (null == userInfo) {
            return null;
        }
        return userInfo.sessionWrapperMap.get(serverGuid);
    }

    /**
     * 链接到远程。
     * @param netContext 本地信息
     * @param serverGuid 在登录服或别处获得的serverGuid
     * @param serverType 服务器类型
     * @param hostAndPort 服务器地址
     * @param initializerSupplier 初始化器提供者，如果initializer是线程安全的，可以始终返回同一个对象
     * @param lifecycleAware 作为客户端，链接不同的服务器时，可能有不同的生命周期事件处理
     * @param messageHandler 消息处理器
     */
    public void connect(NetContext netContext, long serverGuid, RoleType serverType, HostAndPort hostAndPort,
                        @Nonnull ChannelInitializerSupplier initializerSupplier,
                        @Nonnull SessionLifecycleAware<C2SSession> lifecycleAware,
                        @Nonnull MessageHandler messageHandler) throws IllegalArgumentException{
        // 已注册
        long localGuid = netContext.localGuid();
        if (getSessionWrapper(localGuid, serverGuid) != null){
            throw new IllegalArgumentException("session localGuid " + localGuid + "- serverGuid " + serverGuid+ " registered before.");
        }
        // 保存用户信息，因为是发起连接请求，因此方法参数都是针对单个会话的。
        UserInfo userInfo = userInfoMap.computeIfAbsent(localGuid, k -> new UserInfo(netContext));

        // 创建会话
        C2SSession session = new C2SSession(netContext, managerWrapper, serverGuid, serverType, hostAndPort);
        byte[] encryptedLoginToken = tokenManager.newEncryptedLoginToken(netContext.localGuid(), netContext.localRole(), serverGuid, serverType);
        SessionWrapper sessionWrapper = new SessionWrapper(userInfo, initializerSupplier, lifecycleAware, messageHandler, session, encryptedLoginToken);
        // 保存会话
        userInfo.sessionWrapperMap.put(session.getServerGuid(), sessionWrapper);
        // 初始为连接状态
        changeState(sessionWrapper, new ConnectingState(sessionWrapper));
    }

    /**
     * 如果session可用的话
     * @param localGuid form
     * @param serverGuid to
     * @param then 接下来做什么呢？
     */
    private void ifSessionOk(long localGuid, long serverGuid, Consumer<SessionWrapper> then) {
        SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        if (null == sessionWrapper){
            logger.warn("server {} is removed, but try send message.", serverGuid);
            return;
        }
        MessageQueue messageQueue = sessionWrapper.getMessageQueue();
        if (messageQueue.getCacheMessageNum() >= netConfigManager.clientMaxCacheNum()){
            // 缓存过多，删除会话
            removeSession(localGuid, serverGuid, "cacheMessageNum is too much!");
        }else {
            then.accept(sessionWrapper);
        }
    }

    /**
     * 向服务器发送一个消息,不保证立即发送，因为会话状态不确定，只保证最后一定会按顺序发送出去
     * @param localGuid from
     * @param serverGuid to
     * @param message 消息内容
     */
    @Override
    public void send(long localGuid, long serverGuid, @Nonnull Object message){
        ifSessionOk(localGuid, serverGuid, sessionWrapper -> {
            // 添加到待发送队列
            UnsentOneWayMessage unsentOneWayMessage = new UnsentOneWayMessage(message);
            sessionWrapper.state.addToNeedSendQueue(unsentOneWayMessage);
        });
    }

    /**
     * 发送rpc调用结果
     * @param localGuid form
     * @param serverGuid to
     * @param requestGuid rpc请求号
     * @param response rpc调用结果
     */
    @Override
    public void sendRpcResponse(long localGuid, long serverGuid, boolean sync, long requestGuid, @Nonnull RpcResponse response) {
        ifSessionOk(localGuid, serverGuid, sessionWrapper -> {
            UnsentRpcResponse unsentRpcResponse = new UnsentRpcResponse(requestGuid, response);
            if (sync) {
                sessionWrapper.state.trySendImmediately(unsentRpcResponse);
            } else {
                // 添加到待发送队列
                sessionWrapper.state.addToNeedSendQueue(unsentRpcResponse);
            }
        });
    }

    /**
     * 发送一个rpc调用
     * @param localGuid 我的标识
     * @param serverGuid 远程标识
     * @param request rpc请求内容
     * @param timeoutMs 超时时间
     * @param sync 是否是同步调用，同步调用会立即发送(会插队)，而非同步调用可能会先缓存，不是立即发送。
     * @param rpcPromise 接收结果的promise
     */
    @Override
    public void rpc(long localGuid, long serverGuid, @Nonnull Object request, long timeoutMs, boolean sync, Promise<RpcResponse> rpcPromise){
        SessionWrapper sessionWrapper = getSessionWrapper(localGuid, serverGuid);
        if (null == sessionWrapper){
            logger.warn("server {} is removed, but try send rpcRequest.",serverGuid);
            rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
            return;
        }
        MessageQueue messageQueue = sessionWrapper.getMessageQueue();
        if (messageQueue.getCacheMessageNum() >= netConfigManager.clientMaxCacheNum()){
            // 缓存过多，删除会话
            removeSession(localGuid, serverGuid, "cached message is too much!");
            rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED);
        }else {
            UnsentRpcRequest rpcRequest = new UnsentRpcRequest(sessionWrapper.nextRequestGuid(), sync, request);
            // 在发送前，保存promise信息
            RpcPromiseInfo rpcPromiseInfo = new RpcPromiseInfo(rpcPromise, netTimeManager.getSystemMillTime() + timeoutMs);
            sessionWrapper.getRpcPromiseMap().put(rpcRequest.getRpcRequestGuid(), rpcPromiseInfo);
            if (sync) {
                // 同步调用，尝试立即发送
                sessionWrapper.state.trySendImmediately(rpcRequest);
            } else {
                // 添加到缓存队列，稍后发送
                sessionWrapper.state.addToNeedSendQueue(rpcRequest);
            }
        }
    }

    /**
     * 关闭一个会话，如果注册了的话
     * @param localGuid 本地用户标识
     * @param serverGuid 远程节点标识
     */
    @Override
    public boolean removeSession(long localGuid, long serverGuid, String reason){
        UserInfo userInfo = userInfoMap.get(localGuid);
        // 没有该用户的会话
        if (userInfo == null) {
            return true;
        }
        // 删除会话
        SessionWrapper sessionWrapper = userInfo.sessionWrapperMap.remove(serverGuid);
        if (null == sessionWrapper){
            return true;
        }
        afterRemoved(sessionWrapper, reason);
        return true;
    }

    /** 当会话删除之后 */
    private void afterRemoved(SessionWrapper sessionWrapper, String reason) {
        C2SSession session = sessionWrapper.getSession();
        // 标记为已关闭，这里不能调用close，否则死循环了。
        session.setClosed();
        try{
            // 取消所有的rpcPromise
            FastCollectionsUtils.removeIfAndThen(sessionWrapper.getRpcPromiseMap(),
                    (k, rpcPromiseInfo) -> true,
                    (k, rpcPromiseInfo) -> rpcPromiseInfo.rpcPromise.trySuccess(RpcResponse.SESSION_CLOSED));

            // 验证成功过才执行断开回调操作(调用过onSessionConnected方法)
            if (sessionWrapper.getVerifiedSequencer().get() > 0){
                NetContext netContext = sessionWrapper.userInfo.netContext;
                // 提交到用户线程
                ConcurrentUtils.tryCommit(netContext.localEventLoop(), () -> {
                    sessionWrapper.lifecycleAware.onSessionDisconnected(session);
                });
            }
        } catch (Exception e){
            logger.warn("disconnected callback caught exception.", e);
        }finally {
            // 移除之前进行必要的清理
            if (sessionWrapper.getState()!=null){
                sessionWrapper.getState().closeChannel();
                sessionWrapper.setState(null);
            }
            logger.info("remove session by reason of {}, session info={}.", reason, session);
        }
    }

    /**
     *
     * 删除某个用户的所有会话，(赶脚不必发送通知)
     * @param localGuid 用户id
     * @param reason 移除会话的原因
     */
    @Override
    public void removeUserSession(long localGuid, String reason) {
        UserInfo userInfo = userInfoMap.remove(localGuid);
        if (null == userInfo) {
            return;
        }
        removeUserSession(userInfo, reason);
    }

    /**
     * 删除某个用户的所有会话，(赶脚不必发送通知)
     * @param userInfo 用户信息
     * @param reason 移除会话的原因
     */
    private void removeUserSession(UserInfo userInfo, String reason) {
        FastCollectionsUtils.removeIfAndThen(userInfo.sessionWrapperMap,
                (k, sessionWrapper) -> true,
                (k, sessionWrapper) -> afterRemoved(sessionWrapper, reason));
    }

    /**
     * 当用户所在的EventLoop关闭
     */
    @Override
    public void onUserEventLoopTerminal(EventLoop userEventLoop) {
        FastCollectionsUtils.removeIfAndThen(userInfoMap,
                (k, userInfo) -> userInfo.netContext.localEventLoop() == userEventLoop,
                (k, userInfo) -> removeUserSession(userInfo, "onUserEventLoopTerminal"));
    }

    // region  --------------------------------- 网络事件处理 ---------------------------------

    /**
     * 如果产生事件的channel可用的话，接下来干什么呢？
     * @param then 接下来执行的逻辑
     */
    private <T extends NetEventParam> void ifEventChannelOK(Channel eventChannel, T eventParam, Consumer<C2SSessionState> then){
        SessionWrapper sessionWrapper = getSessionWrapper(eventParam.localGuid(), eventParam.remoteGuid());
        // 非法的channel
        if (sessionWrapper == null){
            NetUtils.closeQuietly(eventChannel);
            return;
        }
        // 校验收到消息的channel是否合法
        C2SSessionState sessionState = sessionWrapper.getState();
        if (!sessionState.isEventChannelOK(eventChannel)){
            NetUtils.closeQuietly(eventChannel);
            return;
        }
        then.accept(sessionState);
    }

    /**
     * 当收到服务器的Token验证结果
     * @param responseParam 连接响应结果
     */
    void onRcvConnectResponse(ConnectResponseEventParam responseParam){
        final Channel eventChannel = responseParam.channel();
        ifEventChannelOK(eventChannel,responseParam, sessionState -> {
            // 无论什么状态，只要当前channel收到token验证失败，都关闭session(移除会话)，它意味着服务器通知关闭。
            if (!responseParam.isSuccess()){
                NetUtils.closeQuietly(eventChannel);
                removeSession(responseParam.localGuid(), responseParam.getServerGuid(),"token check failed.");
                return;
            }
            // token验证成功
            sessionState.onTokenCheckSuccess(eventChannel, responseParam);
        });
    }

    /**
     * 当收到远程的rpc请求时
     * @param rpcRequestEventParam rpc请求
     */
    void onRcvServerRpcRequest(RpcRequestEventParam rpcRequestEventParam) {
        final Channel eventChannel = rpcRequestEventParam.channel();
        ifEventChannelOK(eventChannel, rpcRequestEventParam, c2SSessionState -> {
            c2SSessionState.onRcvServerRpcRequest(eventChannel, rpcRequestEventParam);
        });
    }

    /**
     * 当收到远程返回的rpc调用结果时
     * @param rpcResponseEventParam rpc调用结果
     */
    void onRcvServerRpcResponse(RpcResponseEventParam rpcResponseEventParam) {
        final Channel eventChannel = rpcResponseEventParam.channel();
        ifEventChannelOK(eventChannel, rpcResponseEventParam, c2SSessionState -> {
            c2SSessionState.onRcvServerRpcResponse(eventChannel, rpcResponseEventParam);
        });
    }

    /**
     * 当收到服务器的ping包返回时
     * @param ackPongParam 服务器返回的pong包
     */
    void onRevServerAckPong(AckPingPongEventParam ackPongParam){
        final Channel eventChannel = ackPongParam.channel();
        ifEventChannelOK(eventChannel,ackPongParam, c2SSessionState -> {
            c2SSessionState.onRcvServerAckPong(eventChannel, ackPongParam);
        });
    }

    /**
     * 当收到服务器的单向消息时
     * @param oneWayMessageEventParam 服务器发来的业务逻辑包
     */
    void onRevServerOneWayMsg(OneWayMessageEventParam oneWayMessageEventParam){
        final Channel eventChannel = oneWayMessageEventParam.channel();
        ifEventChannelOK(eventChannel,oneWayMessageEventParam, c2SSessionState -> {
            c2SSessionState.onRcvServerMessage(eventChannel, oneWayMessageEventParam);
        });
    }
    // endregion

    // ------------------------------------------------状态机------------------------------------------------

    /**
     * 切换session的状态
     */
    private void changeState(SessionWrapper sessionWrapper, C2SSessionState newState){
        sessionWrapper.setState(newState);;
        if (sessionWrapper.getState()!=null){
            sessionWrapper.getState().enter();
        }
    }

    /**
     * 客户端只会有三个网络事件(三种类型协议)，
     * 1.token验证结果
     * 2.服务器ack-ping返回协议(ack-pong)
     * 3.服务器发送的正式消息
     *
     * 同时也只有三种状态：
     * 1.尝试连接状态
     * 2.正在验证状态
     * 3.已验证状态
     */
    private abstract class C2SSessionState{

        final SessionWrapper sessionWrapper;

        final C2SSession session;

        C2SSessionState(SessionWrapper sessionWrapper) {
            this.sessionWrapper = sessionWrapper;
            this.session = sessionWrapper.session;
        }

        MessageQueue getMessageQueue(){
            return sessionWrapper.messageQueue;
        }

        IntSequencer getVerifiedSequencer(){
            return sessionWrapper.getVerifiedSequencer();
        }

        IntSequencer getSndTokenSequencer(){
            return sessionWrapper.getSndTokenSequencer();
        }

        protected abstract void enter();

        protected abstract void execute();

        // 为何不要exit 因为根本不保证exit能走到,此外导致退出状态的原因太多，要做的事情并不一致，因此重要逻辑不能依赖exit

        /**
         * 在session关闭之前进行资源的清理，清理该状态自身持有的资源
         * (主要是channel)
         */
        public abstract void closeChannel();

        /**
         * 产生事件的channel是否OK，客户端只有当前持有的channel是合法的，因此很多地方是比较简单的。
         * @param eventChannel 产生事件的channel
         * @return 当产生事件的channel是期望的channel时返回true
         */
        protected abstract boolean isEventChannelOK(Channel eventChannel);

        /**
         * 当收到服务器的token验证成功消息
         * @param eventChannel 产生事件的channel
         * @param resultParam 返回信息
         */
        protected void onTokenCheckSuccess(Channel eventChannel, ConnectResponseEventParam resultParam){
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 当收到服务器的Rpc请求时
         * @param eventChannel 产生事件的channel
         * @param rpcRequestEventParam 服务器发来的rpc请求
         */
        protected void onRcvServerRpcRequest(Channel eventChannel, RpcRequestEventParam rpcRequestEventParam) {
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 当收到服务器的逻辑消息包
         * @param eventChannel 产生事件的channel
         * @param oneWayMessageEventParam 服务器发来的单向消息
         */
        protected void onRcvServerMessage(Channel eventChannel, OneWayMessageEventParam oneWayMessageEventParam){
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        protected void onRcvServerRpcResponse(Channel eventChannel, RpcResponseEventParam responseEventParam) {
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 当收到服务器的ack-pong消息包
         * @param eventChannel 产生事件的channel
         * @param ackPongParam 服务器返回的pong包
         */
        protected void onRcvServerAckPong(Channel eventChannel, AckPingPongEventParam ackPongParam){
            throw new IllegalStateException(this.getClass().getSimpleName());
        }

        /**
         * 尝试立即发送一条消息，默认放在缓存队列中等待发送。
         * @param unsentMessage 未发送的消息
         */
        protected void trySendImmediately(UnsentMessage unsentMessage) {
            getMessageQueue().getNeedSendQueue().add(unsentMessage);
        }

        /**
         * 提阿尼啊到待发送队列
         * @param unsentMessage 未发送的消息
         */
        protected void addToNeedSendQueue(UnsentMessage unsentMessage) {
            getMessageQueue().getNeedSendQueue().add(unsentMessage);
        }

    }

    /**
     * 连接状态
     */
    private class ConnectingState extends C2SSessionState {

        private ChannelFuture channelFuture;
        /**
         * 已尝试连接次数
         */
        private int tryTimes = 0;
        /**
         * 连接开始时间
         */
        private long connectStartTime = 0;

        ConnectingState(SessionWrapper sessionWrapper) {
            super(sessionWrapper);
        }

        @Override
        protected void enter() {
            tryConnect();
        }

        private void tryConnect(){
            tryTimes++;
            connectStartTime = netTimeManager.getSystemMillTime();
            channelFuture = acceptorManager.connectAsyn(session.getHostAndPort(), sessionWrapper.getInitializerSupplier().get());
            logger.debug("tryConnect remote {} ,tryTimes {}.", session.getHostAndPort(),tryTimes);
        }

        @Override
        protected void execute() {
            // 建立连接成功
            if (channelFuture.isSuccess() && channelFuture.channel().isActive()){
                logger.debug("connect remote {} success,tryTimes {}.", session.getHostAndPort(),tryTimes);
                changeState(sessionWrapper, new VerifyingState(sessionWrapper,channelFuture.channel()));
                return;
            }
            // 还未超时
            if (netTimeManager.getSystemMillTime()-connectStartTime< netConfigManager.connectTimeout()){
                return;
            }
            // 本次建立连接超时，关闭当前future,并再次尝试
            closeFuture();

            if (tryTimes < netConfigManager.connectMaxTryTimes()){
                // 还可以继续尝试
                tryConnect();
            }else {
                // 无法连接到服务器，移除会话，结束
                removeSession(sessionWrapper.getLocalGuid(), session.getServerGuid(),"can't connect remote " + session.getHostAndPort());
            }
        }

        private void closeFuture() {
            NetUtils.closeQuietly(channelFuture);
            channelFuture=null;
        }

        @Override
        public void closeChannel() {
            closeFuture();
        }

        @Override
        protected boolean isEventChannelOK(Channel eventChannel) {
            // 永远返回false，当前状态下不会响应其它事件
            return false;
        }
    }

    /**
     * 已建立链接状态，可重连状态
     */
    private abstract class ConnectedState extends C2SSessionState{
        /**
         * 已建立连接的channel
         * (已连接的意思是：socket已连接)
         */
        protected final Channel channel;

        ConnectedState(SessionWrapper sessionWrapper, Channel channel) {
            super(sessionWrapper);
            this.channel=channel;
        }

        @Override
        public final void closeChannel() {
            NetUtils.closeQuietly(channel);
        }

        /**
         * 只会响应当前channel的消息事件
         */
        @Override
        protected final boolean isEventChannelOK(Channel eventChannel) {
            return this.channel == eventChannel;
        }

        /**
         * 重连
         * @param reason 重连的原因
         */
        final void reconnect(String reason){
            NetUtils.closeQuietly(channel);
            changeState(sessionWrapper,new ConnectingState(sessionWrapper));
            logger.info("reconnect by reason of {}",reason);
        }

    }

    /**
     * 正在验证状态。
     *
     * 1.如果限定时间内未收到任何消息，则尝试重新连接。
     * 2.收到其它消息，但未收到token验证结果时：，则会再次进行验证。
     * 3.收到验证结果：
     * <li>任何状态下收到验证失败都会关闭session</li>
     * <li>验证成功且判定服务器的ack正确时，验证完成</li>
     * <li>验证成功但判定服务器的ack错误时，关闭session</li>
     */
    private class VerifyingState extends ConnectedState{
        /**
         * 进入状态机的时间戳，用于计算token响应超时
         */
        private long enterStateMillTime;

        VerifyingState(SessionWrapper sessionWrapper, Channel channel) {
            super(sessionWrapper,channel);
        }

        /**
         * 发送token
         */
        @Override
        protected void enter() {
            enterStateMillTime= netTimeManager.getSystemMillTime();

            int sndTokenTimes = getSndTokenSequencer().incAndGet();
            // 创建验证请求
            ConnectRequestTO connectRequest = new ConnectRequestTO(sessionWrapper.getLocalGuid(),
                    sndTokenTimes, getMessageQueue().getAck(), sessionWrapper.getEncryptedToken());
            channel.writeAndFlush(connectRequest);
            logger.debug("{} times send verify msg to server {}",sndTokenTimes,session);
        }

        @Override
        protected void execute() {
            if (netTimeManager.getSystemMillTime()-enterStateMillTime> netConfigManager.waitTokenResultTimeout()){
                // 获取token结果超时，重连
                reconnect("wait token result timeout.");
            }
        }

        @Override
        protected void onTokenCheckSuccess(Channel eventChannel, ConnectResponseEventParam resultParam) {
            // 不是等待的结果
            if (resultParam.getSndTokenTimes() != getSndTokenSequencer().get()){
                return;
            }
            MessageQueue messageQueue = getMessageQueue();
            // 收到的ack有误(有丢包)，这里重连已没有意义(始终有消息漏掉了，无法恢复)
            if (!messageQueue.isAckOK(resultParam.getAck())){
                removeSession(sessionWrapper.getLocalGuid(), resultParam.getServerGuid(), "server ack is error. ackInfo="+messageQueue.generateAckErrorInfo(resultParam.getAck()));
                return;
            }
            // 更新消息队列
            sessionWrapper.getMessageQueue().updateSentQueue(resultParam.getAck());
            // 保存新的token
            sessionWrapper.setEncryptedToken(resultParam.getEncryptedToken());
            changeState(sessionWrapper,new VerifiedState(sessionWrapper,channel));
        }

        @Override
        protected void onRcvServerRpcRequest(Channel eventChannel, RpcRequestEventParam rpcRequestEventParam) {
            reconnect("onRcvServerRpcRequest,but missing token result");
        }

        @Override
        protected void onRcvServerRpcResponse(Channel eventChannel, RpcResponseEventParam responseEventParam) {
            reconnect("onRcvServerRpcResponse,but missing token result");
        }

        @Override
        protected void onRcvServerAckPong(Channel eventChannel, AckPingPongEventParam ackPongParam) {
            reconnect("onRcvServerAckPong,but missing token result");
        }

        @Override
        protected void onRcvServerMessage(Channel eventChannel, OneWayMessageEventParam oneWayMessageEventParam) {
            reconnect("onRcvServerMessage,but missing token result");
        }

    }

    /**
     * token验证成功状态
     */
    private class VerifiedState extends ConnectedState{
        /**
         * 当前队列是否有ping包，避免遍历
         */
        private boolean hasPingMessage;
        /**
         * 上次向服务器发送消息的时间。
         * 它的重要作用是避免双方缓存队列过大，尤其是降低服务器压力。
         */
        private int lastSendMessageTime;

        VerifiedState(SessionWrapper sessionWrapper, Channel channel) {
            super(sessionWrapper,channel);
        }

        @Override
        protected void enter() {
            hasPingMessage=false;
            lastSendMessageTime= netTimeManager.getSystemSecTime();

            int verifiedTimes = getVerifiedSequencer().incAndGet();
            // 增加验证次数
            if (verifiedTimes==1){
                logger.info("first verified success, sessionInfo={}", session);
                if (session.tryActive()) {
                    NetContext netContext = sessionWrapper.userInfo.netContext;
                    // 提交到用户线程
                    ConcurrentUtils.tryCommit(netContext.localEventLoop(), ()-> {
                        sessionWrapper.getLifecycleAware().onSessionConnected(session);
                    });
                }
                // else 会话已关闭
            }else {
                logger.info("reconnect verified success, verifiedTimes={},sessionInfo={}", verifiedTimes, session);
                // 重发未确认接受到的消息
                resend();
            }
        }

        /**
         * 重发那些已发送，但是未被确认的消息
         */
        private void resend() {
            MessageQueue messageQueue= getMessageQueue();
            if (messageQueue.getSentQueue().size()>0){
                for (NetMessage message:messageQueue.getSentQueue()){
                    channel.write(message.build(messageQueue.getAck()));
                }
                channel.flush();
            }
        }

        @Override
        protected void execute() {
            MessageQueue messageQueue= getMessageQueue();
            // 检查消息超时
            if (messageQueue.getSentQueue().size()>0){
                long firstMessageTimeout=messageQueue.getSentQueue().getFirst().getTimeout();
                // 超时未收到第一条消息的ack
                if (netTimeManager.getSystemMillTime()>=firstMessageTimeout){
                    reconnect("first msg of sentQueue timeout.");
                    return;
                }
            }

            // 是否需要发送ack-ping包，ping包服务器收到一定是会返回的，而普通消息则不一定。
            if (isNeedSendAckPing()){
                messageQueue.getNeedSendQueue().add(new UnsentAckPingPong());
                hasPingMessage=true;
                logger.info("send ack ping");
            }

            // 有待发送的消息则发送
            if (messageQueue.getNeedSendQueue().size() > 0){
                flushAllUnsentMessage();
            }
        }

        /** 发送所有待发送的消息 */
        private void flushAllUnsentMessage() {
            MessageQueue messageQueue = getMessageQueue();
            // 发送消息
            while (messageQueue.getNeedSendQueue().size() > 0){
                UnsentMessage unsentMessage = messageQueue.getNeedSendQueue().removeFirst();
                MessageTO messageTO = transferToSentMessage(unsentMessage, messageQueue);
                channel.write(messageTO);
            }
            channel.flush();
            lastSendMessageTime= netTimeManager.getSystemSecTime();
        }

        /** 将一个消息包转换为已发送状态 */
        private MessageTO transferToSentMessage(UnsentMessage unsentMessage, MessageQueue messageQueue) {
            NetMessage netMessage = unsentMessage.build(messageQueue.nextSequence());
            // 添加到已发送队列
            messageQueue.getSentQueue().addLast(netMessage);
            // 更新ack超时时间
            netMessage.setTimeout(nextAckTimeout());
            return netMessage.build(messageQueue.getAck());
        }

        /**
         * 是否需要发送ack-ping包
         * 什么时候需要发？？？
         * 需要同时满足以下条件：
         * 1.当前无ping消息等待结果
         * 2.当前无待发送消息
         * 3.距离最后一条消息发送过去了超时时长的一半 或 长时间未收到服务器消息
         * @return 满足以上条件时返回true，否则返回false。
         */
        private boolean isNeedSendAckPing(){
            // 有ping包还未返回
            if (hasPingMessage){
                return false;
            }
            MessageQueue messageQueue= getMessageQueue();
            // 有待发送的逻辑包
            if (messageQueue.getNeedSendQueue().size()>0){
                return false;
            }
            // 判断发送的最后一条消息的的等待确认时长是否过去了一半(降低都是无返回的消息时导致的超时概率)
            // 如果每次发的都是无返回的协议也太极限了，我们在游戏中不考虑这种情况,通过重连解决该问题
            if (messageQueue.getSentQueue().size()>0){
                long ackTimeout=messageQueue.getSentQueue().getLast().getTimeout();
                return ackTimeout - netTimeManager.getSystemMillTime() <= netConfigManager.ackTimeout()/2;
            }
            // 已经有一段时间没有向服务器发送消息了(session超时时间过去1/3)，保活和降低服务器内存压力
            return netTimeManager.getSystemSecTime() - lastSendMessageTime >= netConfigManager.sessionTimeout()/3;
        }

        private long nextAckTimeout(){
            return netTimeManager.getSystemMillTime()+ netConfigManager.ackTimeout();
        }

        @Override
        protected void onRcvServerRpcRequest(Channel eventChannel, RpcRequestEventParam rpcRequestEventParam) {
            // 大量的lambda表达式可能影响性能，目前先不优化，先注意可维护性。
            RpcRequestMessageTO requestTO = rpcRequestEventParam.messageTO();
            ifSequenceAndAckOk(requestTO, ()-> {
               ConcurrentUtils.tryCommit(sessionWrapper.getNetContext().localEventLoop(), () -> {
                   try {
                       sessionWrapper.messageHandler.onRpcRequest(session, requestTO.getRequest(),
                               new StandardRpcResponseChannel(session, requestTO.isSync(), requestTO.getRequestGuid()));
                   } catch (Exception e){
                       ConcurrentUtils.rethrow(e);
                   }
               });
            });
        }

        @Override
        public void onRcvServerRpcResponse(Channel eventChannel, RpcResponseEventParam responseEventParam) {
            final RpcResponseMessageTO responseMessageTO = responseEventParam.messageTO();
            ifSequenceAndAckOk(responseMessageTO, () -> {
                RpcPromiseInfo rpcPromiseInfo = sessionWrapper.rpcPromiseMap.remove(responseMessageTO.getRequestGuid());
                if (null != rpcPromiseInfo) {
                    // 为甚要try？因为其它地方可能会取消等
                    rpcPromiseInfo.rpcPromise.trySuccess(responseMessageTO.getRpcResponse());
                }
            });
            // else 超时了
        }

        @Override
        protected void onRcvServerAckPong(Channel eventChannel, AckPingPongEventParam ackPongParam) {
            hasPingMessage = false;
            ifSequenceAndAckOk(ackPongParam.messageTO(), ConcurrentUtils.NO_OP_TASK);
            logger.info("rcv ack pong");
        }

        @Override
        protected void onRcvServerMessage(Channel eventChannel, OneWayMessageEventParam oneWayMessageEventParam) {
            OneWayMessageTO oneWayMessageTO = oneWayMessageEventParam.messageTO();
            ifSequenceAndAckOk(oneWayMessageTO, () -> {
                // 提交到用户线程
                ConcurrentUtils.tryCommit(sessionWrapper.getNetContext().localEventLoop(), () -> {
                    try {
                        sessionWrapper.messageHandler.onMessage(session, oneWayMessageTO.getMessage());
                    } catch (Exception e){
                        ConcurrentUtils.rethrow(e);
                    }
                });
            });
        }

        /**
         * 如果消息的ack和sequence正常的话，接下来做什么呢？
         * 当服务器发来的消息是期望的下一个消息，且ack正确时执行指定逻辑。
         * @param messageTO 服务器发来的消息(pong包或业务逻辑包)
         */
        final void ifSequenceAndAckOk(MessageTO messageTO, Runnable then){
            MessageQueue messageQueue = getMessageQueue();
            // 不是期望的下一个消息,请求重传
            if (messageTO.getSequence() != messageQueue.getAck()+1){
                reconnect("serverSequence != ack()+1, serverSequence=" + messageTO.getSequence() + ", ack="+messageQueue.getAck());
                return;
            }
            // 服务器ack不对，尝试矫正
            if (!messageQueue.isAckOK(messageTO.getAck())){
                reconnect("server ack error,ackInfo="+messageQueue.generateAckErrorInfo(messageTO.getAck()));
                return;
            }
            messageQueue.setAck(messageTO.getSequence());
            messageQueue.updateSentQueue(messageTO.getAck());
            then.run();
        }

        @Override
        protected void trySendImmediately(UnsentMessage unsentMessage) {
            // 当前状态下可发送消息
            MessageTO messageTO = transferToSentMessage(unsentMessage, getMessageQueue());
            // 立即发送
            channel.writeAndFlush(messageTO);
        }

        @Override
        protected void addToNeedSendQueue(UnsentMessage unsentMessage) {
            super.addToNeedSendQueue(unsentMessage);
            // 缓存消息数超过阈值，立即尝试发送
            if (getMessageQueue().getNeedSendQueue().size() >= netConfigManager.flushThreshold()) {
                flushAllUnsentMessage();
            }
        }
    }

    // ------------------------------------------------------ 内部封装 ---------------------------------

    /** 用户的所有会话信息 */
    private static class UserInfo {

        /** 用户信息 */
        private final NetContext netContext;

        /**
         * 客户端发起的所有会话,注册时加入，close时删除
         * serverGuid --> session
         */
        private final Long2ObjectMap<SessionWrapper> sessionWrapperMap = new Long2ObjectOpenHashMap<>();

        UserInfo(NetContext netContext) {
            this.netContext = netContext;
        }
    }

    /**
     * session包装对象
     * 不将额外信息暴露给应用层，同时实现线程安全。
     */
    private static class SessionWrapper {

        /** 建立Session和用户之间的关系 */
        private final UserInfo userInfo;

        /**
         * 该会话使用的initializer提供者。
         * （如果容易用错的话，可以改成{@link ChannelInitializerFactory}）
         */
        private final ChannelInitializerSupplier initializerSupplier;

        /**
         * 该会话使用的生命周期回调接口
         */
        private final SessionLifecycleAware<C2SSession> lifecycleAware;
        /**
         * 消息处理器
         */
        private final MessageHandler messageHandler;

        /**
         * 客户端与服务器之间的会话信息
         */
        private final C2SSession session;
        /**
         * 客户端是消息队列
         */
        private final MessageQueue messageQueue = new MessageQueue();
        /**
         * 发送token次数
         */
        private final IntSequencer sndTokenSequencer = new IntSequencer(0);
        /**
         * 验证成功的次数
         * (也等于收到token结果的次数，因为验证失败，就会删除session)
         */
        private final IntSequencer verifiedSequencer = new IntSequencer(0);
        /**
         * 被加密的Token，客户端并不关心具体内容，只是保存用于建立链接
         */
        private byte[] encryptedToken;
        /**
         * 会话当前状态
         */
        private C2SSessionState state;

        /**
         * 当前会话上的rpc计数
         */
        private final LongSequencer rpcRequestGuidSequencer = new LongSequencer(0);
        /**
         * 当前会话上的rpc请求
         */
        private final Long2ObjectMap<RpcPromiseInfo> rpcPromiseMap = new Long2ObjectOpenHashMap<>();

        SessionWrapper(UserInfo userInfo, ChannelInitializerSupplier initializerSupplier,
                       SessionLifecycleAware<C2SSession> lifecycleAware, MessageHandler messageHandler,
                       C2SSession session, byte[] encryptedToken) {
            this.userInfo = userInfo;
            this.initializerSupplier = initializerSupplier;
            this.lifecycleAware = lifecycleAware;
            this.messageHandler = messageHandler;
            this.session = session;
            this.encryptedToken = encryptedToken;
        }

        public C2SSession getSession() {
            return session;
        }

        MessageQueue getMessageQueue() {
            return messageQueue;
        }

        C2SSessionState getState() {
            return state;
        }

        void setEncryptedToken(byte[] encryptedToken) {
            this.encryptedToken = encryptedToken;
        }

        void setState(C2SSessionState state) {
            this.state = state;
        }

        byte[] getEncryptedToken() {
            return encryptedToken;
        }

        IntSequencer getSndTokenSequencer() {
            return sndTokenSequencer;
        }

        IntSequencer getVerifiedSequencer() {
            return verifiedSequencer;
        }

        long getLocalGuid() {
            return userInfo.netContext.localGuid();
        }

        SessionLifecycleAware<C2SSession> getLifecycleAware() {
            return lifecycleAware;
        }

        ChannelInitializerSupplier getInitializerSupplier() {
            return initializerSupplier;
        }

        Long2ObjectMap<RpcPromiseInfo> getRpcPromiseMap() {
            return rpcPromiseMap;
        }

        long nextRequestGuid() {
            return rpcRequestGuidSequencer.incAndGet();
        }

        NetContext getNetContext() {
            return userInfo.netContext;
        }
    }

}
