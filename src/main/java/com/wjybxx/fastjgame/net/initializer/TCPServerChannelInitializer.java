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

package com.wjybxx.fastjgame.net.initializer;

import com.wjybxx.fastjgame.manager.networld.NetEventManager;
import com.wjybxx.fastjgame.net.codec.ServerCodec;
import com.wjybxx.fastjgame.net.CodecHelper;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 服务器channel初始化器示例
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 22:17
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class TCPServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    /** 本地发起监听的角色guid */
    private final long localGuid;
    private final int maxFrameLength;
    private final CodecHelper codecHelper;
    private final NetEventManager netEventManager;

    public TCPServerChannelInitializer(long localGuid, int maxFrameLength, CodecHelper codecHelper, NetEventManager netEventManager) {
        this.localGuid = localGuid;
        this.maxFrameLength = maxFrameLength;
        this.netEventManager = netEventManager;
        this.codecHelper = codecHelper;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline=ch.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(maxFrameLength, 0, 4, 0, 4));
        pipeline.addLast(new ServerCodec(codecHelper, localGuid, netEventManager));
    }
}
