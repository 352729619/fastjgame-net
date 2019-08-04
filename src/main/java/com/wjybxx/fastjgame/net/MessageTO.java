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

import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * 消息传输对象，将要发送的数据安全的传输到IO线程 (网络层线程 -> netty线程)。
 * 子类全部实现为线程不可变对象，有助于保证线程安全性。
 * (虽然不必如此，因此该对象是安全发布到netty线程，并且不会重复使用，
 * 但是实现为不可变对象意图更明确，避免有人想着重用该对象)
 *
 * 通信采用捎带确认机制：一个消息包必须有ack和sequence字段。
 * 注意：一个包的{@link #sequence}不会改变，但是ack会在每次发送的时候改变，因此
 * {@link MessageTO}是不可重用的，每次都会新创建。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 9:26
 * github - https://github.com/hl845740757
 */
@Immutable
@TransferObject
public abstract class MessageTO {

    /**
     * 捎带确认的ack
     */
    private final long ack;

    /**
     * 当前包id
     */
    private final long sequence;

    public MessageTO(long ack, long sequence) {
        this.ack = ack;
        this.sequence = sequence;
    }

    public final long getAck() {
        return ack;
    }

    public final long getSequence() {
        return sequence;
    }

}
