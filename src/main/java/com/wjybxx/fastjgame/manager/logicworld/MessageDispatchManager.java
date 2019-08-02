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

package com.wjybxx.fastjgame.manager.logicworld;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.manager.GameEventLoopManager;
import com.wjybxx.fastjgame.manager.networld.RpcManager;
import com.wjybxx.fastjgame.net.MessageHandler;
import com.wjybxx.fastjgame.net.MessageMapper;
import com.wjybxx.fastjgame.net.RpcRequestEventParam;
import com.wjybxx.fastjgame.net.Session;
import com.wjybxx.fastjgame.net.rpc.StandardRpcChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 消息分发器，用于业务逻辑线程。
 * 非线程安全，每一个LogicWorld一个。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:04
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class MessageDispatchManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageDispatchManager.class);
    private final GameEventLoopManager gameEventLoopManager;
    private final RpcManager rpcManager;
    /**
     * 单向消息处理器
     */
    private final Map<Class<?>, MessageHandler<?>> messageHandlerMap = new IdentityHashMap<>();
    /**
     * rpc请求处理器
     */
    private final Map<Class<?>, MessageHandler<?>> rpcRequestHandlerMap = new IdentityHashMap<>();

    @Inject
    public MessageDispatchManager(GameEventLoopManager gameEventLoopManager, RpcManager rpcManager) {
        this.gameEventLoopManager = gameEventLoopManager;
        this.rpcManager = rpcManager;
    }

    /**
     * 注册一个客户端消息处理器
     * @param messageClazz 消息类必须存在于{@link MessageMapper}
     * @param messageHandler 消息处理器
     * @param <T> 消息的类型
     */
    public <T> void registerMessageHandler(Class<T> messageClazz, MessageHandler<? super T> messageHandler){
        if (messageHandlerMap.containsKey(messageClazz)){
            throw new IllegalArgumentException(messageClazz.getSimpleName() + " already registered.");
        }
        messageHandlerMap.put(messageClazz,messageHandler);
    }

    /**
     * 处理另一方发来的单向消息。
     *
     * @param session 会话信息
     * @param message 单向消息
     * @param <T> 消息的类型
     */
    public <T> void handleMessage(Session session, @Nullable T message) {
        assert gameEventLoopManager.inEventLoop();

        // 未成功解码的消息，做个记录并丢弃(不影响其它请求)
        if (null == message){
            logger.warn("roleType={} remoteLogicWorldGuid={} send null message",
                    session.remoteRole(), session.remoteGuid());
            return;
        }
        // 未注册的消息，做个记录并丢弃(不影响其它请求)
        @SuppressWarnings("unchecked")
        MessageHandler<T> messageHandler = (MessageHandler<T>) messageHandlerMap.get(message.getClass());
        if (null==messageHandler){
            logger.warn("roleType={} remoteLogicWorldGuid={} send unregistered message {}",
                    session.remoteRole(), session.remoteGuid(), message.getClass().getSimpleName());
            return;
        }
        try {
            messageHandler.handle(session, message);
        } catch (Exception e) {
            logger.warn("handle message caught exception,remoteLogicWorldGuid={},roleType={},message clazz={}",
                    session.remoteRole(), session.remoteGuid(), message.getClass().getSimpleName(), e);
        }
    }

    /**
     * 因为什么时候返回结果是不确定的，因此该方法本身不声明返回值。
     * 分类型分派
     * @param session 关联的会话信息
     * @param rpcRequestEventParam rpc请求参数
     */
    public void handleRpcRequest(Session session, RpcRequestEventParam rpcRequestEventParam) {
        final long logicWorldGuid = rpcRequestEventParam.logicWorldGuid();
        final long remoteLogicWorldGuid = rpcRequestEventParam.remoteLogicWorldGuid();
        final long requestGuid = rpcRequestEventParam.messageTO().getRequestGuid();
        StandardRpcChannel standardRpcChannel = new StandardRpcChannel(logicWorldGuid, remoteLogicWorldGuid, requestGuid, rpcManager);
        // TODO 分类型分派

        try {

        } catch (Exception e) {

        } finally {

        }
    }

}
