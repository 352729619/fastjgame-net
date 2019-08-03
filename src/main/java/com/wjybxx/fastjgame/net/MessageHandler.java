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

package com.wjybxx.fastjgame.net;

/**
 * 业务逻辑消息处理器，包括单向消息，rpc请求
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:05
 * github - https://github.com/hl845740757
 */
public interface MessageHandler {

    /**
     * 处理该会话发来单向的消息
     * @param session 会话信息
     * @param message 业务逻辑消息
     * @throws Exception error
     */
    void onMessage(Session session, Object message) throws Exception;

    /**
     * 处理该会话发来的Rpc请求
     * @param session 会话信息
     * @param request rpc请求
     * @param responseChannel 返回结果的通道
     * @throws Exception error
     */
    void onRpcRequest(Session session, Object request, RpcResponseChannel responseChannel) throws Exception;

}
