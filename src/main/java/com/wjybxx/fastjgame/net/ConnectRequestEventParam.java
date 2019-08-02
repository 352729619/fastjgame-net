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

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/2
 * github - https://github.com/hl845740757
 */
public class ConnectRequestEventParam implements NetEventParam{

    private final long logicWorldGuid;
    private final Channel channel;
    private final ConnectRequestTO connectRequestTO;

    public ConnectRequestEventParam(long logicWorldGuid, Channel channel, ConnectRequestTO connectRequestTO) {
        this.logicWorldGuid = logicWorldGuid;
        this.channel = channel;
        this.connectRequestTO = connectRequestTO;
    }

    public long getClientGuid() {
        return connectRequestTO.getClientGuid();
    }

    public long getServerGuid() {
        return logicWorldGuid;
    }

    public long getAck() {
        return connectRequestTO.getAck();
    }

    public byte[] getTokenBytes() {
        return connectRequestTO.getTokenBytes();
    }

    public int getSndTokenTimes() {
        return connectRequestTO.getSndTokenTimes();
    }

    public ConnectRequestTO getConnectRequestTO() {
        return connectRequestTO;
    }

    @Override
    public long logicWorldGuid() {
        return logicWorldGuid;
    }

    @Override
    public long remoteLogicWorldGuid() {
        return connectRequestTO.getClientGuid();
    }

    @Override
    public Channel channel() {
        return channel;
    }
}
