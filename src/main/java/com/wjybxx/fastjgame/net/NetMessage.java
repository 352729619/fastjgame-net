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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * 它是网络层已发送但是对方还未确认收到的消息包，它是非线程安全的（因为它不能有可写的ack）。
 * 它由网络层构建，网络层使用，且不会共享，因此字段不必是final(final有一定消耗)。
 *
 * 一个包的{@link #sequence}不会改变，但是ack会在每次发送的时候改变。
 *
 * 2019年7月30日进行了重命名，避免和protoBuf的message搞混淆。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:42
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public abstract class NetMessage {
    /**
     * 当前包id。一个网络包一旦被构建，则不再改变！
     */
    protected long sequence;
    /**
     * 消息确认超时时间
     * 发送的时候设置超时时间
     */
    private long timeout;

    public NetMessage(long sequence) {
        this.sequence = sequence;
    }

    public long getSequence() {
        return sequence;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * 构建传输对象
     * @param ack 捎带确认
     * @return transferObj
     */
    public abstract MessageTO build(long ack);

}
