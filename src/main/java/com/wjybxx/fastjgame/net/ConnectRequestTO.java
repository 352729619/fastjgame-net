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

import javax.annotation.concurrent.Immutable;

/**
 * 客户端发起连接请求的传输对象.
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:54
 * github - https://github.com/hl845740757
 */
@Immutable
@TransferObject
public class ConnectRequestTO {

    /**
     * 我所在的netWorld标识(我是谁)
     * （对于玩家来讲，该标识就是玩家guid）
     */
    private final long clientGuid;
    /**
     * 这是第几次发送token
     * (用于识别同一个token下的请求先后顺序)
     */
    private final int sndTokenTimes;
    /**
     * 客户端已收到的最大协议号
     * (与tcp的ack有细微区别，tcp的ack表示期望的下一个包)
     */
    private final long ack;
    /**
     * 客户端保存的token
     * (服务器发送给客户端的，客户端只保存)
     */
    private final byte[] tokenBytes;

    public ConnectRequestTO(long clientGuid, int sndTokenTimes, long ack, byte[] tokenBytes) {
        this.clientGuid = clientGuid;
        this.sndTokenTimes = sndTokenTimes;
        this.ack = ack;
        this.tokenBytes = tokenBytes;
    }

    public long getClientGuid() {
        return clientGuid;
    }

    public int getSndTokenTimes() {
        return sndTokenTimes;
    }

    public long getAck() {
        return ack;
    }

    public byte[] getTokenBytes() {
        return tokenBytes;
    }
}
