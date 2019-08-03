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

import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * 最开始时为分离的Encoder和Decoder。
 * 那样的问题是不太容易标记channel双方的guid。
 * (会导致协议冗余字段，或使用不必要的同步{@link io.netty.util.AttributeMap})
 *
 * 使用codec会使得协议更加精炼，性能也更好，此外也方便阅读。
 * 它不是线程安全的，也不可共享。
 *
 * baseCodec作为解码过程的最后一步和编码过程的第一步
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/7 12:26
 * github - https://github.com/hl845740757
 */
public abstract class BaseCodec extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(BaseCodec.class);

    final MessageMapper messageMapper;
    final MessageSerializer messageSerializer;

    protected BaseCodec(CodecHelper codecHelper) {
        this.messageMapper = codecHelper.getMessageMapper();
        this.messageSerializer = codecHelper.getMessageSerializer();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NetUtils.setChannelPerformancePreferences(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object byteBuf) throws Exception {
        ByteBuf msg= (ByteBuf) byteBuf;
        try {
            long realSum = msg.readLong();
            long logicSum = NetUtils.calChecksum(msg,msg.readerIndex(),msg.readableBytes());
            if (realSum != logicSum){
                // 校验和不一致
                closeCtx(ctx,"realSum="+realSum + ", logicSum="+logicSum);
                return;
            }
            // 任何编解码出现问题都会在上层消息判断哪里出现问题，这里并不处理channel数据是否异常
            byte pkgTypeNumber = msg.readByte();
            NetPackageType netPackageType = NetPackageType.forNumber(pkgTypeNumber);
            if (null == netPackageType){
                // 约定之外的包类型
                closeCtx(ctx,"null==netEventType " + pkgTypeNumber);
                return;
            }
            readMsg(ctx, netPackageType, msg);
        }finally {
            // 解码结束，释放资源
            msg.release();
        }
    }

    /**
     * 子类真正的读取数据
     * @param ctx ctx
     * @param netPackageType 事件类型
     * @param msg 收到的网络包
     */
    protected abstract void readMsg(ChannelHandlerContext ctx, NetPackageType netPackageType, ByteBuf msg) throws Exception;

    // ---------------------------------------------- 协议1、2  ---------------------------------------
    /**
     * 编码协议1 - 连接请求
     * @param ctx ctx
     * @param msgTO 发送的消息
     */
    final void writeConnectRequest(ChannelHandlerContext ctx, ConnectRequestTO msgTO, ChannelPromise promise) {
        byte[] encryptedToken=msgTO.getTokenBytes();
        int contentLength = 8 + 8 + 4 + 8 + encryptedToken.length;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetPackageType.CONNECT_REQUEST);

        byteBuf.writeLong(msgTO.getClientGuid());
        byteBuf.writeInt(msgTO.getSndTokenTimes());
        byteBuf.writeLong(msgTO.getAck());
        byteBuf.writeBytes(encryptedToken);
        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议1 - 连接请求
     */
    final ConnectRequestTO readConnectRequest(Channel channel, ByteBuf msg) {
        long clientGuid=msg.readLong();
        int sndTokenTimes=msg.readInt();
        long ack=msg.readLong();
        byte[] encryptedToken= NetUtils.readRemainBytes(msg);

        return new ConnectRequestTO(clientGuid, sndTokenTimes, ack, encryptedToken);
    }

    /**
     * 编码协议2 - 连接响应
     */
    final void writeConnectResponse(ChannelHandlerContext ctx, ConnectResponseTO msgTO, ChannelPromise promise) {
        byte[] encryptedToken=msgTO.getEncryptedToken();

        int contentLength = 4 + 1 + 8 + encryptedToken.length;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetPackageType.CONNECT_RESPONSE);

        byteBuf.writeInt(msgTO.getSndTokenTimes());
        byteBuf.writeByte(msgTO.isSuccess()?1:0);
        byteBuf.writeLong(msgTO.getAck());
        byteBuf.writeBytes(msgTO.getEncryptedToken());

        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 解码协议2 - 连接响应
     */
    final ConnectResponseTO readConnectResponse(ByteBuf msg) {
        int sndTokenTimes=msg.readInt();
        boolean success=msg.readByte()==1;
        long ack=msg.readLong();
        byte[] encryptedToken= NetUtils.readRemainBytes(msg);

        return new ConnectResponseTO(sndTokenTimes, success, ack, encryptedToken);
    }

    // ---------------------------------------------- 协议3、4 ---------------------------------------
    /**
     * 3. 编码rpc请求包
     */
    final void writeRpcRequestMessage(ChannelHandlerContext ctx, RpcRequestMessageTO messageTO, ChannelPromise promise) throws Exception{
        // 发送的时候不可能为null
        Object body = messageTO.getRequest();
        int messageId = messageMapper.getMessageId(body.getClass());
        byte[] messageBytes= messageSerializer.serialize(body);

        int contentLength = 8 + 8 + 8 + 4 + messageBytes.length;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetPackageType.RPC_REQUEST);
        // 捎带确认消息
        byteBuf.writeLong(messageTO.getAck());
        byteBuf.writeLong(messageTO.getSequence());
        // rpc请求内容
        byteBuf.writeByte(messageTO.isSync() ? 1 : 0);
        byteBuf.writeLong(messageTO.getRequestGuid());
        byteBuf.writeInt(messageId);
        byteBuf.writeBytes(messageBytes);
        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 3. 解码rpc请求包
     */
    final RpcRequestMessageTO readRpcRequestMessage(ByteBuf msg) {
        // 捎带确认消息
        long ack= msg.readLong();
        long sequence = msg.readLong();
        // rpc内容
        boolean sync = msg.readByte() == 1;
        long requestGuid = msg.readLong();
        int messageId = msg.readInt();
        byte[] messageBytes = NetUtils.readRemainBytes(msg);

        Object request = null;
        try {
            Class<?> messageClazz = messageMapper.getMessageClazz(messageId);
            assert null != messageClazz:"messageId " + messageId + " clazz not found";
            request = messageSerializer.deserialize(messageClazz, messageBytes);
        }catch (Exception e){
            // 为了不影响该连接上的其它消息，需要捕获异常
            logger.warn("deserialize messageId {} caught exception", messageId, e);
        }
        return new RpcRequestMessageTO(ack, sequence, sync, requestGuid, request);
    }

    /**
     * 4. 编码rpc 响应包
     */
    final void writeRpcResponseMessage(ChannelHandlerContext ctx, RpcResponseMessageTO messageTO, ChannelPromise promise) throws Exception{
        final int baseLength = 8 + 8 + 8 + 4;
        if (RpcResultCode.hasBody(messageTO.getResultCode())) {
            Object body = messageTO.getBody();
            int messageId = messageMapper.getMessageId(body.getClass());
            byte[] messageBytes= messageSerializer.serialize(body);

            int contentLength = baseLength + 4 + messageBytes.length;
            ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetPackageType.RPC_RESPONSE);

            writeResponseCommon(messageTO, byteBuf);

            byteBuf.writeInt(messageId);
            byteBuf.writeBytes(messageBytes);

            appendSumAndWrite(ctx, byteBuf, promise);
        } else {
            ByteBuf byteBuf = newInitializedByteBuf(ctx, baseLength, NetPackageType.RPC_RESPONSE);

            writeResponseCommon(messageTO, byteBuf);

            appendSumAndWrite(ctx, byteBuf, promise);
        }
    }

    private void writeResponseCommon(RpcResponseMessageTO messageTO, ByteBuf byteBuf) {
        // 捎带确认信息
        byteBuf.writeLong(messageTO.getAck());
        byteBuf.writeLong(messageTO.getSequence());
        // 响应内容
        byteBuf.writeLong(messageTO.getRequestGuid());
        byteBuf.writeInt(messageTO.getResultCode().getNumber());
    }

    /**
     * 4. 解码rpc 响应包
     */
    final RpcResponseMessageTO readRpcResponseMessage(ByteBuf msg) {
        // 捎带确认信息
        long ack= msg.readLong();
        long sequence = msg.readLong();
        // 响应内容
        long requestGuid = msg.readLong();
        RpcResultCode resultCode = RpcResultCode.forNumber(msg.readInt());
        Object body = null;
        if (RpcResultCode.hasBody(resultCode)) {
            int messageId = msg.readInt();
            byte[] messageBytes = NetUtils.readRemainBytes(msg);
            body = tryReadMessage(messageId, messageBytes);
        }
        return new RpcResponseMessageTO(ack, sequence, requestGuid, new RpcResponse(resultCode, body));
    }

    // ------------------------------------------ 协议5 --------------------------------------------

    /**
     * 7.编码单向协议包
     */
    final void writeOneWayMessage(ChannelHandlerContext ctx, OneWayMessageTO msgTO, ChannelPromise promise) throws IOException {
        Object message = msgTO.getMessage();
        int messageId = messageMapper.getMessageId(message.getClass());
        byte[] messageBytes= messageSerializer.serialize(message);

        int contentLength = 8 + 8 + 4 + messageBytes.length;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, NetPackageType.RPC_RESPONSE);
        // 捎带确认
        byteBuf.writeLong(msgTO.getAck());
        byteBuf.writeLong(msgTO.getSequence());
        // 消息内容
        byteBuf.writeInt(messageId);
        byteBuf.writeBytes(messageBytes);

        appendSumAndWrite(ctx, byteBuf, promise);
    }

    /**
     * 7.解码单向协议
     */
    final OneWayMessageTO readOneWayMessage(ByteBuf msg) {
        // 捎带确认
        long ack = msg.readLong();
        long sequence = msg.readLong();
        // 消息内容
        int messageId = msg.readInt();
        byte[] messageBytes = NetUtils.readRemainBytes(msg);
        Object message = tryReadMessage(messageId, messageBytes);
        return new OneWayMessageTO(ack, sequence, message);
    }

    /**
     * 尝试解码消息
     * @param messageId 协议id
     * @param messageBytes 协议内容
     * @return 为了不引用该连接上的其它消息，如果解码失败返回null。
     */
    @Nullable
    private Object tryReadMessage(int messageId, byte[] messageBytes) {
        Object message = null;
        try {
            Class<?> messageClazz = messageMapper.getMessageClazz(messageId);
            message = messageSerializer.deserialize(messageClazz, messageBytes);
        }catch (Exception e){
            // 为了不影响该连接上的其它消息，需要捕获异常
            logger.warn("deserialize messageId {} caught exception", messageId, e);
        }
        return message;
    }
    // ---------------------------------------------- 协议6/7  ---------------------------------------
    /**
     * 编码协议6/7 - ack心跳包
     */
    final void writeAckPingPongMessage(ChannelHandlerContext ctx, AckPingPongMessageTO msgTO,
                                                 ChannelPromise promise, NetPackageType netPackageType) {
        int contentLength = 8 + 8;
        ByteBuf byteBuf = newInitializedByteBuf(ctx, contentLength, netPackageType);

        byteBuf.writeLong(msgTO.getAck());
        byteBuf.writeLong(msgTO.getSequence());
        appendSumAndWrite(ctx,byteBuf,promise);
    }

    /**
     * 解码协议6/7 - ack心跳包
     */
    final AckPingPongMessageTO readAckPingPongMessage(ByteBuf msg) {
        long ack = msg.readLong();
        long sequence = msg.readLong();
        return new AckPingPongMessageTO(ack, sequence);
    }
    // ------------------------------------------ 分割线 --------------------------------------------
    /**
     * 关闭channel
     * @param ctx 待关闭的context
     * @param reason 关闭context的原因
     */
    final void closeCtx(ChannelHandlerContext ctx, String reason){
        logger.warn("close channel by reason of {}", reason);
        NetUtils.closeQuietly(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        closeCtx(ctx,"decode exceptionCaught.");
        logger.info("", cause);
    }

    /**
     * 创建一个初始化好的byteBuf
     * 设置包总长度 和 校验和
     * @param ctx handlerContext，用于获取allocator
     * @param contentLength 有效内容的长度
     * @return 足够空间的byteBuf可以直接写入内容部分
     */
    private ByteBuf newInitializedByteBuf(ChannelHandlerContext ctx, int contentLength, NetPackageType netNetPackageType){
        return NetUtils.newInitializedByteBuf(ctx, contentLength, netNetPackageType.pkgType);
    }

    /**
     * 添加校验和并发送
     * @param ctx handlerContext，用于将数据发送出去
     * @param byteBuf 待发送的数据包
     * @param promise 操作回执
     */
    private void appendSumAndWrite(ChannelHandlerContext ctx, ByteBuf byteBuf, ChannelPromise promise) {
        NetUtils.appendCheckSum(byteBuf);
        ctx.write(byteBuf, promise);
    }
}
