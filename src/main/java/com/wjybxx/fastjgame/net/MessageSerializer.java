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

import java.io.IOException;

/**
 * 消息序列化器，完成业务逻辑消息对象的序列化与反序列化工作。
 * {@link #init(MessageMapper)} happens-before {@link #serialize(Object)} and {@link #deserialize(Class, byte[])}
 * （由线程启动规则提供happens-before规则）
 *
 * 此外符合文档约束的实现将是<b>事实不可变对象</b>，它需要安全发布，才能保证线程安全；
 *
 * @apiNote
 * 如果serializer是有状态的，那么初始化完成之后，序列化和反序列化方法不可以修改它的状态，以实现高性能的并发。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:20
 * github - https://github.com/hl845740757
 */
public interface MessageSerializer {

    /**
     * 可能需要根据消息映射某些初始化操作，以优化某些处理等等。
     * @apiNote
     * 1. 该方法不要手动调用，否则可能导致线程安全问题！
     * 2. 该方法必须完成所有可能使用到的信息的初始化，不要做延迟初始化等等！
     * 满足以上两点规则的话，该对象可以安全的共享。
     *
     * happens-before {@link #serialize(Object)} and {@link #deserialize(Class, byte[])}
     */
    void init(MessageMapper messageMapper) throws Exception;

    /**
     * 序列化对象
     * @param message 一个具体的消息对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object message) throws IOException;

    /**
     * 反序列化对象
     * @param messageClazz 消息对应的class
     * @param messageBytes 消息对应的字节数组
     * @return 反序列化之后的消息对象
     */
    <T> T deserialize(Class<T> messageClazz, byte[] messageBytes) throws IOException;
}
