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

package com.wjybxx.fastjgame.manager.networld;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.manager.*;
import com.wjybxx.fastjgame.manager.logicworld.MessageDispatchManager;
import com.wjybxx.fastjgame.misc.*;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerFactory;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;
import com.wjybxx.fastjgame.trigger.Timer;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import com.wjybxx.fastjgame.utils.FastCollectionsUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.BindException;
import java.util.function.Consumer;

/**
 * 服务器到客户端会话管理器。
 * (我接收到的连接)
 *
 * token相关说明请查看文档 关于token.txt
 *
 * 注意：请求登录、重连时，验证失败不能对当前session做任何操作，因为不能证明表示当前session有异常，
 * 只有连接成功时才能操作session。
 * 同理，也不能更新{@link #forbiddenTokenHelper}。
 *
 * 换句话说：有新的channel请求建立连接，不能代表旧的channel和会话有异常，有可能是新的channel是非法的。
 *
 * 什么时候应该删除session？
 * 1.主动调用{@link #removeSession(long, long, String)}
 * 2.会话超时
 * 3.缓存过多
 * 4.客户端重新登录
 *
 * 什么时候会关闭channel？
 *  {@link #removeSession(long, long, String)} 或者说 {@link #notifyClientExit(Channel, SessionWrapper)}
 * {@link #notifyTokenCheckFailed(Channel, ConnectRequestEventParam, FailReason)}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:14
 * github - https://github.com/hl845740757
 */
public class S2CSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(S2CSessionManager.class);

    private final NetTimeManager netTimeManager;
    private final NetConfigManager netConfigManager;
    private final TokenManager tokenManager;
    private final AcceptManager acceptManager;
    private final ForbiddenTokenHelper forbiddenTokenHelper;
    /** NetWorld的EventLoop持有者 */
    private final GameEventLoopManager gameEventLoopManager;
    private final LogicWorldManager logicWorldManager;
    /** logicWorld的会话信息 */
    private final Long2ObjectMap<LogicWorldSessionInfo> logicWorldSessionInfoMap = new Long2ObjectOpenHashMap<>();

    @Inject
    public S2CSessionManager(NetTimeManager netTimeManager, NetConfigManager netConfigManager, NetTriggerManager netTriggerManager,
                             TokenManager tokenManager, AcceptManager acceptManager, GameEventLoopManager gameEventLoopManager,
                             LogicWorldManager logicWorldManager) {
        this.netTimeManager = netTimeManager;
        this.netConfigManager = netConfigManager;
        this.tokenManager = tokenManager;
        this.acceptManager = acceptManager;
        this.gameEventLoopManager = gameEventLoopManager;
        this.logicWorldManager = logicWorldManager;
        this.forbiddenTokenHelper=new ForbiddenTokenHelper(netTimeManager, netTriggerManager, netConfigManager.tokenForbiddenTimeout());

        // 定时检查会话超时的timer(1/3个周期检测一次)
        Timer checkTimeOutTimer = Timer.newInfiniteTimer(netConfigManager.sessionTimeout()/3 * 1000,this::checkSessionTimeout);
        netTriggerManager.addTimer(checkTimeOutTimer);
    }

    /**
     * 定时检查会话超时时间
     */
    private void checkSessionTimeout(Timer timer){
        for(LogicWorldSessionInfo logicWorldSessionInfo:logicWorldSessionInfoMap.values()) {
            FastCollectionsUtils.removeIfAndThen(logicWorldSessionInfo.sessionWrapperMap,
                    (k, sessionWrapper) -> netTimeManager.getSystemSecTime() > sessionWrapper.getSessionTimeout(),
                    (k, sessionWrapper) -> afterRemoved(sessionWrapper, "session time out!"));
        }
    }

    /**
     * @see AcceptManager#bind(boolean, int, ChannelInitializer)
     */
    public HostAndPort bind(long worldGuid, boolean outer, int port,
                            ChannelInitializerSupplier initializerSupplier,
                            SessionLifecycleAware<S2CSession> lifecycleAware) throws BindException {
        createLogicWorldSessionInfo(worldGuid, initializerSupplier, lifecycleAware);
        return acceptManager.bind(outer, port, initializerSupplier.get());
    }

    /**
     * @see AcceptManager#bindRange(boolean, PortRange, ChannelInitializer)
     */
    public HostAndPort bindRange(long worldGuid, boolean outer, PortRange portRange,
                                 ChannelInitializerSupplier initializerSupplier,
                                 SessionLifecycleAware<S2CSession> lifecycleAware) throws BindException {
        createLogicWorldSessionInfo(worldGuid, initializerSupplier, lifecycleAware);

        return acceptManager.bindRange(outer, portRange, initializerSupplier.get());
    }

    /** 创建必要的信息 */
    private void createLogicWorldSessionInfo(long worldGuid,
                                             ChannelInitializerSupplier initializerSupplier,
                                             SessionLifecycleAware<S2CSession> lifecycleAware) {
        logicWorldManager.ensureRegistered(worldGuid);
        LogicWorldInNetWorldInfo logicWorldInNetWorldInfo = logicWorldManager.getLogicWorldInfo(worldGuid);
        assert null != logicWorldInNetWorldInfo;
        RoleType worldType = logicWorldInNetWorldInfo.getWorldType();

        logicWorldSessionInfoMap.computeIfAbsent(worldGuid, k -> {
            return new LogicWorldSessionInfo(worldGuid, worldType, lifecycleAware, initializerSupplier);
        });
    }

    /**
     * 是否存在指定session
     * @param logicWorldGuid 我的标识
     * @param remoteLogicWorldGuid 远程标识
     * @return 如果存在则返回true
     */
    public boolean containsSession(long logicWorldGuid, long remoteLogicWorldGuid) {
        return null != getSessionWrapper(logicWorldGuid, remoteLogicWorldGuid);
    }

    /**
     * 获取session
     * @param logicWorldGuid 对应的logicWorld
     * @param clientGuid 连接的客户端的guid
     * @return 如果存在则返回对应的session，否则返回null
     */
    private SessionWrapper getSessionWrapper(long logicWorldGuid, long clientGuid) {
        assert gameEventLoopManager.inEventLoop();
        LogicWorldSessionInfo logicWorldSessionInfo = logicWorldSessionInfoMap.get(logicWorldGuid);
        if (null == logicWorldSessionInfo) {
            return null;
        }
        return logicWorldSessionInfo.sessionWrapperMap.get(clientGuid);
    }

    /**
     * 发送一条单向消息
     * @param logicWorldGuid from
     * @param clientGuid to
     * @param message 消息内容
     */
    public void send(long logicWorldGuid, long clientGuid, @Nonnull Object message){
        ifSessionOk(logicWorldGuid, clientGuid, sessionWrapper -> {
            OneWayMessage logicMessage = new OneWayMessage(sessionWrapper.getMessageQueue().nextSequence(), message);
            sessionWrapper.writeAndFlush(logicMessage);
        });
    }

    /**
     * 如果session可用的话
     * @param then 接下来做什么呢？
     */
    private void ifSessionOk(long logicWorldGuid, long clientGuid, Consumer<SessionWrapper> then) {
        SessionWrapper sessionWrapper = getSessionWrapper(logicWorldGuid, clientGuid);
        if (null== sessionWrapper){
            logger.warn("client {} is removed, but try send message.",clientGuid);
            return;
        }
        if (sessionWrapper.getCacheMessageNum() >= netConfigManager.serverMaxCacheNum()){
            removeSession(logicWorldGuid, clientGuid,"cacheMessageNum is too much! cacheMessageNum="+sessionWrapper.getCacheMessageNum());
        }else {
            then.accept(sessionWrapper);
        }
    }

    /**
     * 发送rpc响应
     * @param logicWorldGuid 我的id
     * @param clientGuid 远程节点id
     * @param requestGuid 请求对应的编号
     * @param response 响应结果
     */
    public void sendRpcResponse(long logicWorldGuid, long clientGuid, long requestGuid, RpcResponse response) {
        ifSessionOk(logicWorldGuid, clientGuid, sessionWrapper -> {
            RpcResponseMessage responseMessage = new RpcResponseMessage(sessionWrapper.getMessageQueue().nextSequence(), requestGuid, response);
            sessionWrapper.writeAndFlush(responseMessage);
        });
    }

    /**
     * 向远程发送一个rpc请求
     * @param logicWorldGuid 我的标识
     * @param clientGuid 远程节点标识
     * @param request rpc请求内容
     * @param responsePromise 用于监听结果
     */
    public void rpc(long logicWorldGuid, long clientGuid, @Nonnull Object request, Promise<RpcResponse> responsePromise){
        SessionWrapper sessionWrapper = getSessionWrapper(logicWorldGuid, clientGuid);
        if (null== sessionWrapper){
            logger.warn("client {} is removed, but try send message.",clientGuid);
            responsePromise.trySuccess(new RpcResponse(RpcResultCode.SESSION_NOT_EXIST, null));
            return;
        }
        if (sessionWrapper.getCacheMessageNum() >= netConfigManager.serverMaxCacheNum()){
            removeSession(logicWorldGuid, clientGuid,"cached message is too much! cacheMessageNum="+sessionWrapper.getCacheMessageNum());
            responsePromise.trySuccess(new RpcResponse(RpcResultCode.SESSION_NOT_EXIST, null));
        }else {
            RpcRequestMessage rpcRequestMessage = new RpcRequestMessage(sessionWrapper.getMessageQueue().nextSequence(), sessionWrapper.nextRequestGuid(), request);
            // 必须先保存promise，再发送消息，严格的时序保证
            sessionWrapper.getRpcListenerMap().put(rpcRequestMessage.getRequestGuid(), responsePromise);
            // 注意：这行代码两个控制器不一样，一个是放入了缓存，一个是立即发送
            sessionWrapper.writeAndFlush(rpcRequestMessage);
        }
    }

    /**
     * 请求移除一个会话
     * @param clientGuid remoteLogicWorldGuid
     * @param reason 要是可扩展的，好像只有字符串最合适
     */
    public S2CSession removeSession(long logicWorldGuid, long clientGuid, String reason){
        SessionWrapper sessionWrapper = getSessionWrapper(logicWorldGuid, clientGuid);
        if (null == sessionWrapper){
            return null;
        }
        afterRemoved(sessionWrapper, reason);
        return sessionWrapper.getSession();
    }

    /**
     * 会话删除之后
     */
    private void afterRemoved(SessionWrapper sessionWrapper, String reason) {
        // 禁用该token及之前的token
        forbiddenTokenHelper.forbiddenCurToken(sessionWrapper.getToken());

        S2CSession session = sessionWrapper.getSession();
        notifyClientExit(sessionWrapper.getChannel(),sessionWrapper);
        logger.info("remove session by reason of {}, session info={}.",reason, session);

        LogicWorldInNetWorldInfo logicWorldInfo = logicWorldManager.getLogicWorldInfo(sessionWrapper.getLogicWorldGuid());
        if (null == logicWorldInfo) {
            logger.error("Session state error. Connect success, but logicWorld info may already onShutdown.");
        } else {
            // 断开连接通知
            EventLoopUtils.executeOrRun(logicWorldInfo.getEventLoop(), () -> {
                sessionWrapper.getLifecycleAware().onSessionDisconnected(session);
            }, e -> {
                logger.warn("disconnect callback caught exception",e);
            });
        }
    }

    /**
     * 收到客户端的链接请求(请求验证token)
     * @param requestParam 请求参数
     */
    void onRcvConnectRequest(ConnectRequestEventParam requestParam){
        final Channel channel = requestParam.channel();

        Token clientToken= tokenManager.decryptToken(requestParam.getTokenBytes());
        // 客户端token不合法(解析错误)
        if (null==clientToken){
            notifyTokenCheckFailed(channel, requestParam, FailReason.NULL);
            return;
        }
        // 服务器会话已经不存这里了(服务器没有监听或已经关闭)
        if (!logicWorldSessionInfoMap.containsKey(requestParam.logicWorldGuid())) {
            notifyTokenCheckFailed(channel, requestParam, FailReason.SERVER_NOT_EXIST);
            return;
        }
        // 无效token
        if (tokenManager.isFailToken(clientToken)){
            notifyTokenCheckFailed(channel, requestParam, FailReason.INVALID);
            return;
        }
        // token与请求不匹配
        if (!isRequestMatchToken(requestParam,clientToken)){
            notifyTokenCheckFailed(channel, requestParam, FailReason.TOKEN_NOT_MATCH_REQUEST);
            return;
        }
        // 被禁用的旧token
        if (forbiddenTokenHelper.isForbiddenToken(clientToken)){
            notifyTokenCheckFailed(channel, requestParam, FailReason.OLD_REQUEST);
            return;
        }
        // 为什么不能在这里更新forbiddenToken? 因为走到这里还不能证明是一个合法的客户端，不能影响合法客户端的数据。
        SessionWrapper sessionWrapper = getSessionWrapper(requestParam.logicWorldGuid(), requestParam.getClientGuid());
        // 不论如何必须是新的channel
        if (isSameChannel(sessionWrapper, channel)){
            notifyTokenCheckFailed(channel, requestParam, FailReason.SAME_CHANNEL);
            return;
        }
        // 新的token
        if (isNewerToken(sessionWrapper,clientToken)){
            login(channel,requestParam,clientToken);
            return;
        }
        // 当前使用的token
        if (isUsingToken(sessionWrapper,clientToken)){
            reconnect(channel,requestParam,clientToken);
            return;
        }
        // 其它(介于使用的两个token之间的token)
        notifyTokenCheckFailed(channel,requestParam,FailReason.OLD_REQUEST);
    }

    /**
     * 是否是同一个channel
     */
    private boolean isSameChannel(SessionWrapper sessionWrapper, Channel channel) {
        return null!=sessionWrapper && sessionWrapper.getChannel()==channel;
    }

    /**
     * 是否是更新的token
     */
    private boolean isNewerToken(@Nullable SessionWrapper sessionWrapper, Token clientToken) {
        return null == sessionWrapper || clientToken.getCreateSecTime() > sessionWrapper.getToken().getCreateSecTime();
    }

    /**
     * 是否是当前会话使用的token
     */
    private boolean isUsingToken(@Nullable SessionWrapper sessionWrapper, Token clientToken) {
        if (null == sessionWrapper){
            return false;
        }
        // 前一个token为null ==> 客户端一定收到了新的token
        if (sessionWrapper.getPreToken()==null){
            return tokenManager.isSameToken(sessionWrapper.getToken(),clientToken);
        }
        // 可能收到了，也可能没收到最新的token，但只能是两者之一
        return tokenManager.isSameToken(sessionWrapper.getToken(),clientToken)
                || tokenManager.isSameToken(sessionWrapper.getPreToken(),clientToken);
    }

    /**
     * 请求与是token否匹配。校验token基本信息,校验token是否被修改过
     * @param requestParam 客户端请求参数
     * @param token 客户端携带的token信息
     */
    private boolean isRequestMatchToken(ConnectRequestEventParam requestParam, @Nonnull Token token){
        // token不是用于该客户端的
        if (requestParam.getClientGuid() != token.getClientGuid()
                || requestParam.logicWorldGuid() != token.getServerGuid()){
            return false;
        }
        // token不是用于该服务器的
        LogicWorldInNetWorldInfo logicWorldInNetWorldInfo = logicWorldManager.getLogicWorldInfo(token.getServerGuid());
        if (null == logicWorldInNetWorldInfo || token.getServerRoleType() != logicWorldInNetWorldInfo.getWorldType()) {
            return false;
        }
        return true;
    }

    /**
     * 使用更加新的token请求登录
     * @param channel 产生事件的channel
     * @param requestParam 客户端发来的请求参数
     * @param clientToken 客户端携带的token信息 更加新的token，newerToken
     */
    private boolean login(Channel channel, ConnectRequestEventParam requestParam, @Nonnull Token clientToken) {
        // 不是登录token
        if (!tokenManager.isLoginToken(clientToken)){
            notifyTokenCheckFailed(channel, requestParam, FailReason.NOT_LOGIN_TOKEN);
            return false;
        }
        // 登录token超时了
        if (tokenManager.isLoginTokenTimeout(clientToken)){
            notifyTokenCheckFailed(channel, requestParam, FailReason.TOKEN_TIMEOUT);
            return false;
        }
        // 客户端已收到消息数必须为0
        if (requestParam.getAck() != MessageQueue.INIT_ACK){
            notifyTokenCheckFailed(channel, requestParam, FailReason.ACK);
            return false;
        }

        // 为何要删除旧会话？(前两个问题都是可以解决的，但是第三个问题不能不管)
        // 1.会话是有状态的，无法基于旧状态与新状态的客户端通信(ack,sequence)
        // 2.旧的token需要被禁用
        // 3.必须进行通知，需要让逻辑层知道旧连接彻底断开了，可能有额外逻辑
        removeSession(requestParam.logicWorldGuid(), requestParam.getClientGuid(),"reLogin");

        // 禁用验证成功的token之前的token(不能放到removeSession之前，会导致覆盖)
        forbiddenTokenHelper.forbiddenPreToken(clientToken);

        // 登录成功
        LogicWorldSessionInfo logicWorldSessionInfo = logicWorldSessionInfoMap.get(requestParam.logicWorldGuid());
        S2CSession session = new S2CSession(requestParam.getClientGuid(), clientToken.getClientRoleType());
        SessionWrapper sessionWrapper = new SessionWrapper(logicWorldSessionInfo, session);
        logicWorldSessionInfo.sessionWrapperMap.put(requestParam.getClientGuid(),sessionWrapper);

        // 分配新的token并进入等待状态
        Token nextToken= tokenManager.newLoginSuccessToken(clientToken);
        sessionWrapper.changeToWaitState(channel, requestParam.getSndTokenTimes(), clientToken, nextToken, nextSessionTimeout());

        notifyTokenCheckSuccess(channel, requestParam, MessageQueue.INIT_ACK,nextToken);
        logger.info("client login success, sessionInfo={}",session);

        // 连接建立回调(通知)
        LogicWorldInNetWorldInfo logicWorldInfo = logicWorldManager.getLogicWorldInfo(logicWorldSessionInfo.logicWorldGuid);
        if (null == logicWorldInfo) {
            logger.error("Session state error. Connect success, but logicWorld info may already onShutdown.");
        } else {
            EventLoopUtils.executeOrRun(logicWorldInfo.getEventLoop(), () -> {
                logicWorldSessionInfo.lifecycleAware.onSessionConnected(session);
            }, e -> {
                logger.warn("sessionConnected callback caught exception",e);
            });
        }
        return true;
    }

    private int nextSessionTimeout() {
        return netTimeManager.getSystemSecTime()+ netConfigManager.sessionTimeout();
    }

    /**
     * 客户端尝试断线重连，token是服务器保存的两个token之一。
     * @param channel 产生事件的channel
     * @param requestParam 客户端发来的请求参数
     * @param clientToken 客户端携带的token信息，等于服务器使用的token (usingToken)
     */
    private boolean reconnect(Channel channel, ConnectRequestEventParam requestParam, @Nonnull Token clientToken) {
        SessionWrapper sessionWrapper = getSessionWrapper(requestParam.logicWorldGuid(), requestParam.getClientGuid());
        assert null != sessionWrapper;
        // 这是一个旧请求
        if (requestParam.getSndTokenTimes() <= sessionWrapper.getSndTokenTimes()){
            notifyTokenCheckFailed(channel, requestParam, FailReason.OLD_REQUEST);
            return false;
        }
        // 判断客户端ack合法性
        MessageQueue messageQueue = sessionWrapper.getMessageQueue();
        if (!messageQueue.isAckOK(requestParam.getAck())){
            notifyTokenCheckFailed(channel, requestParam, FailReason.ACK);
            return false;
        }
        // ---- 这里验证成功 ack 和 token都验证通过
        // 禁用验证成功的token之前的token
        forbiddenTokenHelper.forbiddenPreToken(clientToken);

        // 关闭旧channel
        if (sessionWrapper.getChannel()!=channel){
            NetUtils.closeQuietly(sessionWrapper.getChannel());
        }

        // 更新消息队列
        messageQueue.updateSentQueue(requestParam.getAck());

        // 分配新的token并进入等待状态
        Token nextToken= tokenManager.nextToken(clientToken);
        sessionWrapper.changeToWaitState(channel, requestParam.getSndTokenTimes(), clientToken, nextToken, nextSessionTimeout());

        notifyTokenCheckSuccess(channel, requestParam, messageQueue.getAck(), nextToken);
        logger.info("client reconnect success, sessionInfo={}",sessionWrapper.getSession());

        // 重发已发送未确认的消息
        if (messageQueue.getSentQueue().size()>0){
            for (NetMessage message:messageQueue.getSentQueue()){
                sessionWrapper.getChannel().write(message.build(messageQueue.getAck()));
            }
            sessionWrapper.getChannel().flush();
        }
        return true;
    }

    /**
     * 通知客户端退出
     * @param channel 会话对应的的channel
     * @param sessionWrapper 会话信息
     */
    private void notifyClientExit(Channel channel, SessionWrapper sessionWrapper){
        long clientGuid = sessionWrapper.getSession().getClientGuid();
        Token failToken = tokenManager.newFailToken(clientGuid, sessionWrapper.getLogicWorldGuid());
        notifyTokenCheckResult(channel,sessionWrapper.getSndTokenTimes(),false, -1, failToken);
    }

    /**
     * 通知客户端token验证失败
     * 注意token校验失败，不能认定当前会话失效，可能是错误或非法的连接，因此不能对会话下手
     * @param requestEventParam 客户端的请求信息
     * @param failReason 失败原因，用于记录日志
     */
    private void notifyTokenCheckFailed(Channel channel, ConnectRequestEventParam requestEventParam, FailReason failReason){
        Token failToken = tokenManager.newFailToken(requestEventParam.getClientGuid(), requestEventParam.logicWorldGuid());
        notifyTokenCheckResult(channel,requestEventParam.getConnectRequestTO().getSndTokenTimes(),false, -1, failToken);
        logger.warn("client {} checkTokenResult failed by reason of {}",requestEventParam.getClientGuid(),failReason);
    }

    /**
     * 通知客户端token验证成功
     * @param requestEventParam 客户端的请求信息
     * @param ack 服务器的捎带确认ack
     * @param nextToken 连接成功时新分配的token
     */
    private void notifyTokenCheckSuccess(Channel channel,ConnectRequestEventParam requestEventParam, long ack, Token nextToken){
        notifyTokenCheckResult(channel,requestEventParam.getSndTokenTimes(),true, ack,nextToken);
    }

    /**
     * 通知客户端token验证结果
     * @param channel 发起请求验证token的channel
     * @param sndTokenTimes 这是客户端的第几次请求
     * @param success 是否成功
     * @param ack 服务器的ack
     * @param token 新的token
     */
    private void notifyTokenCheckResult(Channel channel, int sndTokenTimes, boolean success, long ack, Token token){
        byte[] encryptToken = tokenManager.encryptToken(token);
        ConnectResponseTO connectResponse=new ConnectResponseTO(sndTokenTimes,success, ack,encryptToken);
        ChannelFuture future = channel.writeAndFlush(connectResponse);
        // token验证失败情况下，发送之后，关闭channel
        if (!success){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 收到客户端的定时Ack-ping包
     * @param ackPingParam 心跳包参数
     */
    void onRcvClientAckPing(AckPingPongEventParam ackPingParam){
        final Channel eventChannel = ackPingParam.channel();
        tryUpdateMessageQueue(eventChannel,ackPingParam,sessionWrapper -> {
            MessageQueue messageQueue = sessionWrapper.getMessageQueue();
            sessionWrapper.writeAndFlush(new AckPingPongMessage(messageQueue.nextSequence()));
        });
    }

    /**
     * 尝试用message更新消息队列
     * @param eventChannel 产生事件的channel
     * @param eventParam 消息参数
     * @param then 当且仅当message是当前channel上期望的下一个消息，且ack合法时执行。
     */
    private <T extends MessageEventParam> void tryUpdateMessageQueue(Channel eventChannel, T eventParam, Consumer<SessionWrapper> then){
        SessionWrapper sessionWrapper = getSessionWrapper(eventParam.logicWorldGuid(), eventParam.remoteLogicWorldGuid());
        if (null == sessionWrapper){
            NetUtils.closeQuietly(eventChannel);
            return;
        }
        // 必须是相同的channel (isEventChannelOk)
        if (eventChannel!=sessionWrapper.getChannel()){
            NetUtils.closeQuietly(eventChannel);
            return;
        }
        // 在新channel收到客户端的消息时 => 客户端一定收到了token验证结果
        // 确定客户端已收到了新的token,更新channel为已激活状态，并添加禁用的token
        if (sessionWrapper.getPreToken() != null){
            sessionWrapper.changeToActiveState();
            forbiddenTokenHelper.forbiddenPreToken(sessionWrapper.getToken());
        }
        // 更新session超时时间
        sessionWrapper.setSessionTimeout(nextSessionTimeout());

        MessageTO message=eventParam.messageTO();
        MessageQueue messageQueue=sessionWrapper.getMessageQueue();
        // 不是期望的下一个消息
        if (message.getSequence()!=messageQueue.getAck()+1){
            return;
        }
        // 客户端发来的ack错误
        if (!messageQueue.isAckOK(message.getAck())){
            return;
        }
        // 更新消息队列
        messageQueue.setAck(message.getSequence());
        messageQueue.updateSentQueue(message.getAck());

        // 然后执行自己的逻辑
        then.accept(sessionWrapper);
    }

    /**
     * 当接收到客户端发来的rpc请求时
     * @param rpcRequestEventParam rpc请求参数
     */
    void onRcvClientRpcRequest(RpcRequestEventParam rpcRequestEventParam) {
        final Channel eventChannel = rpcRequestEventParam.channel();
        tryUpdateMessageQueue(eventChannel, rpcRequestEventParam, sessionWrapper -> {
            LogicWorldInNetWorldInfo logicWorldInfo = logicWorldManager.getLogicWorldInfo(sessionWrapper.getLogicWorldGuid());
            if (null == logicWorldInfo) {
                // 此时不应该能走到这里来，该logicWorld关联的session都应该被删除了。
                logger.error("session state error!");
                return;
            }
            MessageDispatchManager dispatchManager = logicWorldInfo.getMessageDispatchManager();
            dispatchManager.handleRpcRequest(sessionWrapper.session, rpcRequestEventParam);
        });
    }

    /**
     * 当收到发送给客户端的rpc的响应时
     * @param rpcResponseEventParam rpc响应
     */
    void onRcvClientRpcResponce(RpcResponseEventParam rpcResponseEventParam) {
        final Channel eventChannel = rpcResponseEventParam.channel();
        tryUpdateMessageQueue(eventChannel, rpcResponseEventParam, sessionWrapper -> {
            Promise<RpcResponse> responseListenableFuture = sessionWrapper.getRpcListenerMap().remove(rpcResponseEventParam.messageTO().getRequestGuid());
            if (null == responseListenableFuture) {
                // 可能超时了
                logger.warn("rpc may timeout");
                return;
            }
            responseListenableFuture.trySuccess(rpcResponseEventParam.messageTO().getRpcResponse());
        });
    }

    /**
     * 当接收到客户端发送的单向消息时
     */
    void onRcvClientOneWayMsg(OneWayMessageEventParam oneWayMessageEventParam){
        final Channel eventChannel = oneWayMessageEventParam.channel();
        tryUpdateMessageQueue(eventChannel, oneWayMessageEventParam, sessionWrapper -> {
            LogicWorldInNetWorldInfo logicWorldInfo = logicWorldManager.getLogicWorldInfo(sessionWrapper.getLogicWorldGuid());
            if (null == logicWorldInfo) {
                // 此时不应该能走到这里来，该logicWorld关联的session都应该被删除了。
                logger.error("session state error!");
                return;
            }
            MessageDispatchManager dispatchManager = logicWorldInfo.getMessageDispatchManager();
            dispatchManager.handleMessage(sessionWrapper.getSession(), oneWayMessageEventParam.messageTO().getMessage());
        });
    }

    /**
     * 当logicWorld关闭，关闭该logicWorld对应的所有channel
     */
    public void onLogicWorldShutdown(long logicWorldGuid) {

    }

    // -------------------------------------------------- 内部封装 -------------------------------------------

    private static final class LogicWorldSessionInfo {

        // ------------- 会话关联的本地对象 -----------------
        /** 监听端口的logicWorld的信息 */
        private final long logicWorldGuid;
        private final RoleType logicWorldRoleType;
        /** 会话生命周期handler */
        private final SessionLifecycleAware<S2CSession> lifecycleAware;
        /** （如果容易用错的话，可以改成{@link ChannelInitializerFactory}） */
        private final ChannelInitializerSupplier initializerSupplier;
        /** 该logicWorld上是所有会话信息 */
        private final Long2ObjectMap<SessionWrapper> sessionWrapperMap = new Long2ObjectOpenHashMap<>();
        /** 本地监听rpc结果的future */
        private final Long2ObjectMap<Promise<RpcResponse>> rpcListenerMap = new Long2ObjectOpenHashMap<>();

        private LogicWorldSessionInfo(long logicWorldGuid, RoleType logicWorldRoleType,
                                      @Nonnull SessionLifecycleAware<S2CSession> lifecycleAware,
                                      @Nonnull ChannelInitializerSupplier initializerSupplier) {
            this.logicWorldGuid = logicWorldGuid;
            this.logicWorldRoleType = logicWorldRoleType;
            this.lifecycleAware = lifecycleAware;
            this.initializerSupplier = initializerSupplier;
        }
    }

    /**
     * S2CSession的包装类，不对外暴露细节
     */
    private static final class SessionWrapper {

        private final LogicWorldSessionInfo logicWorldSessionInfo;

        /**
         * 注册的会话信息
         */
        private final S2CSession session;
        /**
         * 会话的消息队列
         */
        private final MessageQueue messageQueue=new MessageQueue();
        /**
         * 会话channel一定不为null
         */
        private Channel channel;
        /**
         * 会话当前token，一定不为null。
         * (如果preToken不为null,则客户端可能还未收到该token)
         */
        private Token token;
        /**
         * 会话过期时间(秒)(时间到则需要移除)
         */
        private int sessionTimeout;
        /**
         * 这是客户端第几次发送token验证
         */
        private int sndTokenTimes;
        /**
         * 在确认客户端收到新的token之前会保留
         */
        private Token preToken=null;

        /**
         * 当前会话上的rpc计数
         */
        private final LongSequencer rpcRequestGuidSequencer = new LongSequencer(0);
        /**
         * 当前会话上的rpc请求
         */
        private final Long2ObjectMap<Promise<RpcResponse>> rpcListenerMap = new Long2ObjectOpenHashMap<>();

        SessionWrapper(LogicWorldSessionInfo logicWorldSessionInfo, S2CSession session) {
            this.logicWorldSessionInfo = logicWorldSessionInfo;
            this.session = session;
        }

        S2CSession getSession() {
            return session;
        }

        Channel getChannel() {
            return channel;
        }

        Token getToken() {
            return token;
        }

        int getSessionTimeout() {
            return sessionTimeout;
        }

        int getSndTokenTimes() {
            return sndTokenTimes;
        }

        MessageQueue getMessageQueue() {
            return messageQueue;
        }

        Token getPreToken() {
            return preToken;
        }

        /**
         * 切换到等待状态，即确认客户端收到新的token之前，新的token还不能生效
         * (等待客户端真正的产生消息,也就是收到了新的token)
         * @param channel 新的channel
         * @param sndTokenTimes 这是对客户端第几次发送token验证
         * @param preToken 上一个token
         * @param nextToken 新的token
         * @param sessionTimeout 会话超时时间
         */
        void changeToWaitState(Channel channel, int sndTokenTimes, Token preToken, Token nextToken, int sessionTimeout){
            this.channel=channel;
            this.token=nextToken;
            this.sessionTimeout =sessionTimeout;
            this.preToken=preToken;
            this.sndTokenTimes=sndTokenTimes;
        }

        /**
         * 切换到激活状态，确定客户端已收到了新的token
         * (收到客户端用当前channel发过来的消息)
         */
        void changeToActiveState(){
            this.preToken=null;
        }

        void setSessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }

        /**
         * 立即发送一个消息
         */
        void writeAndFlush(NetMessage message){
            // 服务器不需要设置它的超时时间，只需要设置捎带确认的ack
            messageQueue.getSentQueue().addLast(message);
            // 发送
            channel.writeAndFlush(message.build(messageQueue.getAck()));
        }

        /**
         * 获取当前缓存的消息数
         * 缓存过多可能需要关闭会话
         */
        int getCacheMessageNum(){
            return messageQueue.getCacheMessageNum();
        }

        long getLogicWorldGuid() {
            return logicWorldSessionInfo.logicWorldGuid;
        }

        SessionLifecycleAware<S2CSession> getLifecycleAware() {
            return logicWorldSessionInfo.lifecycleAware;
        }

        ChannelInitializerSupplier getInitializerSupplier() {
            return logicWorldSessionInfo.initializerSupplier;
        }

        Long2ObjectMap<Promise<RpcResponse>> getRpcListenerMap() {
            return rpcListenerMap;
        }
        long nextRequestGuid() {
            return rpcRequestGuidSequencer.incAndGet();
        }
    }

}