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
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.Token;
import com.wjybxx.fastjgame.net.TokenEncryptStrategy;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;

/**
 * token控制器，用于分配token和token校验。
 * 它可以为别的服务器分配token，不只是服务于当前服务器(当前world)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:12
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class TokenManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);
    /**
     * 初始验证/登录次数
     */
    private static final int INIT_VERIFIED_TIMES = 0;

    private final NetConfigManager netConfigManager;
    private final NetTimeManager netTimeManager;
    /**
     * token加解密策略，允许自定义的复杂策略。
     * 未来可能会放在IO线程中加解密
     */
    private TokenEncryptStrategy tokenEncryptStrategy = new XOREncryptStrategy();

    @Inject
    public TokenManager(NetConfigManager netConfigManager, NetTimeManager netTimeManager) {
        this.netConfigManager = netConfigManager;
        this.netTimeManager = netTimeManager;
    }

    /**
     * world启动前期可以赋值
     * @param tokenEncryptStrategy 自定义的token加密策略
     */
    public void setTokenEncryptStrategy(TokenEncryptStrategy tokenEncryptStrategy) {
        this.tokenEncryptStrategy =Objects.requireNonNull(tokenEncryptStrategy);
    }

    /**
     * 为指定双方创建一个再也无法验证成功的token
     * @param clientGuid 客户端guid
     * @param serverGuid 服务器guid
     * @return
     */
    public Token newFailToken(long clientGuid, long serverGuid){
        return new Token(clientGuid, RoleType.INVALID, serverGuid, RoleType.INVALID, -1, -1);
    }

    /**
     * 是否是用于标记失败的token
     * @param token
     * @return
     */
    public boolean isFailToken(Token token){
        return token.getClientRoleType() == RoleType.INVALID
                || token.getServerRoleType()== RoleType.INVALID
                || token.getVerifiedTimes() < 0
                || token.getCreateSecTime() < 0;
    }

    /**
     * 分配一个在指定服务器登录用的token
     * @param clientGuid 客户端的guid，为哪个客户端创建的
     * @param clientRole 客户端的角色类型
     * @param serverGuid 服务器的guid，要登录的服务器guid
     * @param serverRole 服务器的角色类型
     * @return Token
     */
    public Token newLoginToken(long clientGuid, RoleType clientRole, long serverGuid, RoleType serverRole){
        return new Token(clientGuid, clientRole, serverGuid, serverRole,
                INIT_VERIFIED_TIMES, netTimeManager.getSystemSecTime());
    }

    /**
     * @see #newLoginToken(long, RoleType, long, RoleType)
     * @return tokenBytes
     */
    public byte[] newEncryptedLoginToken(long clientGuid, RoleType clientRole, long serverGuid, RoleType serverRole) {
        return encryptToken(newLoginToken(clientGuid, clientRole, serverGuid, serverRole));
    }

    /**
     * 是否是登录token
     */
    public boolean isLoginToken(Token token) {
        return token.getVerifiedTimes() == INIT_VERIFIED_TIMES;
    }

    /**
     * 登录token是否超时了
     */
    public boolean isLoginTokenTimeout(Token token){
        return netTimeManager.getSystemSecTime() > token.getCreateSecTime() + netConfigManager.loginTokenTimeout();
    }

    /**
     * 创建一个登录成功Token
     */
    public Token newLoginSuccessToken(Token token){
        // 默认的验证次数不一定是0，不能简单的+1
        return new Token(token.getClientGuid(), token.getClientRoleType(), token.getServerGuid(), token.getServerRoleType(),
                1, netTimeManager.getSystemSecTime());
    }

    /**
     * 是否是相同的token,客户端的token是否和服务器token匹配
     */
    public boolean isSameToken(Token existToken, Token token){
        // 不轻易重写equals方法
        return existToken.getClientGuid() == token.getClientGuid() &&
                existToken.getClientRoleType() == token.getClientRoleType() &&
                existToken.getServerGuid() == token.getServerGuid() &&
                existToken.getServerRoleType() == token.getServerRoleType() &&
                existToken.getVerifiedTimes() == token.getVerifiedTimes() &&
                existToken.getCreateSecTime() == token.getCreateSecTime();
    }

    /**
     * 断线重连之后，分配下一个token，还可以加入更多参数，额外的信息需要传参
     */
    public Token nextToken(Token token){
        return new Token(token.getClientGuid(), token.getClientRoleType(), token.getServerGuid(), token.getServerRoleType(),
                token.getVerifiedTimes()+1, netTimeManager.getSystemSecTime());
    }

    /**
     * 加密token，这里最好有自己的实现。
     * @return bytes
     */
    public byte[] encryptToken(Token token){
        return tokenEncryptStrategy.encryptToken(token);
    }

    /**
     * 解密失败则返回null
     * @param encryptedTokenBytes 加密后的token字节数组
     * @return token
     */
    public Token decryptToken(byte[] encryptedTokenBytes){
        try {
            return tokenEncryptStrategy.decryptToken(encryptedTokenBytes);
        }catch (Exception e){
            logger.warn("decryptToken caught exception",e);
            return null;
        }
    }

    /**
     * 默认的异或方式的加解密策略
     */
    private class XOREncryptStrategy implements TokenEncryptStrategy{

        @Override
        public byte[] encryptToken(Token token) {
            byte[] tokenBytes = encodeToken(token);
            return xorByteArray(tokenBytes, netConfigManager.getTokenKeyBytes());
        }

        @Override
        public Token decryptToken(byte[] encryptedTokenBytes) throws Exception {
            byte[] tokenBytes = xorByteArray(encryptedTokenBytes, netConfigManager.getTokenKeyBytes());
            return decodeToken(tokenBytes);
        }
    }

    /**
     * 异或两个字节数组，并返回一个新的字节数组，其长度为msgBytes的长度
     * @param msgBytes 消息对应的字节数组
     * @param keyBytes 用于加密的字节数组
     * @return 加密后的字节数组
     */
    private static byte[] xorByteArray(byte[] msgBytes,byte[] keyBytes){
        byte[] resultBytes = new byte[msgBytes.length];
        for (int index=0;index<msgBytes.length;index++){
            resultBytes[index]= (byte)(msgBytes[index] ^ keyBytes[index%keyBytes.length]);
        }
        return resultBytes;
    }
    /** 编码token */
    private static byte[] encodeToken(Token token) {
        final int contentLength = 8 + 4 + 8 + 4 + 4 + 4;
        ByteBuf byteBuf = Unpooled.buffer(contentLength);
        byteBuf.writeLong(token.getClientGuid());
        byteBuf.writeInt(token.getClientRoleType().getNumber());

        byteBuf.writeLong(token.getServerGuid());
        byteBuf.writeInt(token.getServerRoleType().getNumber());

        byteBuf.writeInt(token.getVerifiedTimes());
        byteBuf.writeInt(token.getCreateSecTime());

        return NetUtils.readRemainBytes(byteBuf);
    }
    /** 解码token */
    private static Token decodeToken(byte[] tokenBytes) {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(tokenBytes);
        long clientGuid = byteBuf.readLong();
        RoleType clientRolType = RoleType.forNumber(byteBuf.readInt());

        long serverGuid = byteBuf.readLong();
        RoleType serverRole = RoleType.forNumber(byteBuf.readInt());

        int verifiedTimes = byteBuf.readInt();
        int createSecTime = byteBuf.readInt();
        return new Token(clientGuid, clientRolType, serverGuid, serverRole, verifiedTimes, createSecTime);
    }
}
