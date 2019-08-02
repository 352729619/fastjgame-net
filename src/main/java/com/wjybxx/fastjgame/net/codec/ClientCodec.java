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

package com.wjybxx.fastjgame.net.codec;

import com.wjybxx.fastjgame.manager.networld.NetEventManager;
import com.wjybxx.fastjgame.net.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 客户端用编解码器。
 * 并非真正的非线程安全，而是唯一关联一个会话，没法共享。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 12:10
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class ClientCodec extends BaseCodec {

    /** 该channel关联哪一个logicWorld */
    private final long logicWorldGuid;
    /** channel所属的serverGuid */
    private final long serverGuid;
    /** 是否已建立链接 */
    private boolean connect = false;

    private final NetEventManager netEventManager;

    public ClientCodec(CodecHelper codecHelper, long logicWorldGuid, long serverGuid, NetEventManager netEventManager) {
        super(codecHelper);
        this.logicWorldGuid = logicWorldGuid;
        this.serverGuid = serverGuid;
        this.netEventManager = netEventManager;
    }

    // region 编码消息
    @Override
    public void write(ChannelHandlerContext ctx, Object msgTO, ChannelPromise promise) throws Exception {
        // 按出现的几率判断
        if (msgTO instanceof RpcRequestMessageTO){
            // 客户端发起的rpc请求
            writeRpcRequestMessage(ctx, (RpcRequestMessageTO) msgTO, promise);
        } else if (msgTO instanceof RpcResponseMessageTO) {
            // rpc返回结果
            writeRpcResponseMessage(ctx, (RpcResponseMessageTO) msgTO, promise);
        } else if (msgTO instanceof OneWayMessageTO) {
            // 单向消息，向另一个服务器发送单向消息
            writeOneWayMessage(ctx, (OneWayMessageTO) msgTO, promise);
        } else if (msgTO instanceof AckPingPongMessageTO){
            // 客户端ack-ping包
            writeAckPingPongMessage(ctx,(AckPingPongMessageTO) msgTO, promise, NetPackageType.ACK_PING);
        } else if (msgTO instanceof ConnectRequestTO){
            // 连接请求包(token验证包)
            writeConnectRequest(ctx, (ConnectRequestTO) msgTO, promise);
        } else {
            super.write(ctx, msgTO, promise);
        }
    }
    // endregion

    // region 读取消息
    @Override
    protected void readMsg(ChannelHandlerContext ctx, NetPackageType netPackageType, ByteBuf msg) {
        switch (netPackageType){
            case CONNECT_RESPONSE:
                tryReadConnectResponse(ctx, msg);
                break;
            case RPC_REQUEST:
                tryReadRpcRequestMessage(ctx, msg);
                break;
            case RPC_RESPONSE:
                tryReadRpcResponseMessage(ctx, msg);
                break;
            case ONE_WAY_MESSAGE:
                tryReadOneWayMessage(ctx, msg);
                break;
            case ACK_PONG:
                tryReadAckPongMessage(ctx, msg);
                break;
            default:
                closeCtx(ctx,"unexpected netEventType " + netPackageType);
                break;
        }
    }

    /**
     * 服务器返回的Token验证结果
     */
    private void tryReadConnectResponse(ChannelHandlerContext ctx, ByteBuf msg) {
        ConnectResponseTO responseTO = readConnectResponse(msg);
        ConnectResponseEventParam connectResponseParam = new ConnectResponseEventParam(logicWorldGuid, ctx.channel(), serverGuid, responseTO);
        netEventManager.publishEvent(NetEventType.CONNECT_RESPONSE, connectResponseParam);

        // 标记为已连接
        if (connectResponseParam.isSuccess()) {
            connect = true;
        }
    }

    /**
     * 服务器返回的ack-pong包
     */
    private void tryReadAckPongMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();

        AckPingPongMessageTO ackPingPongMessage = readAckPingPongMessage(msg);
        AckPingPongEventParam ackPongParam = new AckPingPongEventParam(ctx.channel(), logicWorldGuid, serverGuid, ackPingPongMessage);
        netEventManager.publishEvent(NetEventType.ACK_PONG,ackPongParam);
    }

    /**
     * 尝试读取远程的rpc请求消息
     */
    private void tryReadRpcRequestMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();

        RpcRequestMessageTO rpcRequestMessageTO = readRpcRequestMessage(msg);
        RpcRequestEventParam rpcRequestEventParam = new RpcRequestEventParam(ctx.channel(), logicWorldGuid, serverGuid, rpcRequestMessageTO);
        netEventManager.publishEvent(NetEventType.S2C_RPC_REQUEST, rpcRequestEventParam);
    }

    /**
     * 读取我发起的Rpc的响应消息
     */
    private void tryReadRpcResponseMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();

        RpcResponseMessageTO rpcResponseMessageTO = readRpcResponseMessage(msg);
        RpcResponseEventParam rpcResponseEventParam = new RpcResponseEventParam(ctx.channel(), logicWorldGuid, serverGuid, rpcResponseMessageTO);
        netEventManager.publishEvent(NetEventType.C2S_RPC_RESPONSE, rpcResponseEventParam);
    }

    /**
     * 读取连接的服务器方发来的单向消息
     */
    private void tryReadOneWayMessage(ChannelHandlerContext ctx, ByteBuf msg) {
        ensureConnected();

        OneWayMessageTO oneWayMessageTO = readOneWayMessage(msg);
        OneWayMessageEventParam oneWayMessageEventParam = new OneWayMessageEventParam(ctx.channel(), logicWorldGuid, serverGuid, oneWayMessageTO);
        netEventManager.publishEvent(NetEventType.S2C_ONE_WAY_MESSAGE, oneWayMessageEventParam);
    }
    // endregion

    private void ensureConnected() {
        if (!connect) {
            throw new IllegalStateException();
        }
    }
}