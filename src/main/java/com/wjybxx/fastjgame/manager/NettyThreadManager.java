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
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.misc.AbstractThreadLifeCycleHelper;
import com.wjybxx.fastjgame.manager.NetConfigManager;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Netty线程管理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/29 20:02
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class NettyThreadManager extends AbstractThreadLifeCycleHelper {

    private final NetConfigManager netConfigManager;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Inject
    public NettyThreadManager(NetConfigManager netConfigManager) {
        this.netConfigManager = netConfigManager;
    }

    /**
     * 启动netty线程
     */
    @Override
    protected void startImp() {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("ACCEPTOR_THREAD"));
        workerGroup = new NioEventLoopGroup(netConfigManager.maxIoThreadNum(), new DefaultThreadFactory("IO_THREAD"));
    }

    /**
     * 关闭Netty的线程
     */
    @Override
    protected void shutdownImp() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

}
