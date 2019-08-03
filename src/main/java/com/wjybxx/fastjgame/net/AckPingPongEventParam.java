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

import io.netty.channel.Channel;

import javax.annotation.concurrent.Immutable;

/**
 * ack心跳包事件参数
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:59
 * github - https://github.com/hl845740757
 */
@Immutable
public class AckPingPongEventParam extends MessageEventParam {

    /**
     * 会话唯一id，对方唯一id。
     */
    private final long remoteGuid;
    /**
     * ping包
     */
    private final AckPingPongMessageTO pingPongMessageTO;

    public AckPingPongEventParam(Channel channel, long localGuid, long remoteGuid, AckPingPongMessageTO pingPongMessageTO) {
        super(channel, localGuid);
        this.remoteGuid = remoteGuid;
        this.pingPongMessageTO = pingPongMessageTO;
    }

    public AckPingPongMessageTO messageTO() {
        return pingPongMessageTO;
    }

    @Override
    public long remoteGuid() {
        return remoteGuid;
    }
}
