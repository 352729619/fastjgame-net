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
 * 消息事件参数。
 * 它对应于{@link NetMessage}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/9 9:09
 * github - https://github.com/hl845740757
 */
public abstract class MessageEventParam implements NetEventParam{

    /** 该事件对应的channel */
    private final Channel channel;

    /** 该事件关联的本地角色guid */
    private final long localGuid;

    protected MessageEventParam(Channel channel, long localGuid) {
        this.localGuid = localGuid;
        this.channel = channel;
    }

    @Override
    public final Channel channel() {
        return channel;
    }

    @Override
    public final long localGuid() {
        return localGuid;
    }

    public final long getAck() {
        return messageTO().getAck();
    }

    public final long getSequence() {
        return messageTO().getSequence();
    }

    /**
     * 获取该消息事件上的捎带确认信息
     * @return 捎带确认信息
     */
    public abstract MessageTO messageTO();
}
