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

import com.google.protobuf.AbstractMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * 将protoBuf中的builder转换为message对象，因为消息映射用的是它对应的message。
 * 因为人总是容易忘记，容易老发送builder。
 *
 * 发送builder的好处：可以将一部分操作转移到io线程。
 * 发送builder的坏处：
 *      1.builder是可变对象，如果发送builder后，再次修改builder，是很危险的！！！
 *      2.反复构建多次
 * @author wjybxx
 * @version 1.0
 * date - 2019/6/23 13:39
 * github - https://github.com/hl845740757
 */
public class BuilderToMessageEncoder extends MessageToMessageEncoder<AbstractMessage.Builder> {

    @Override
    protected void encode(ChannelHandlerContext ctx, AbstractMessage.Builder msg, List<Object> out) throws Exception {
        out.add(msg.build());
    }
}
