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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Netty线程管理器。
 * 最终决定还是每一个NetEventLoop一个，因为资源分配各有缺陷。
 * why?
 * 如果以NetEventLoopGroup进行分配，在用户不清楚的情况下，用户可能认为调整NetEventLoopGroup的线程数就可以提高网络性能，但实际上不行。
 * 如果没有调整Netty的线程数，它不能做到随着NetEventLoop线程的增加使性能也增加的目的。
 *
 * 以NetEventLoop为单位分配资源也有坏处，最明显的坏处就是线程数很多。
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
        workerGroup = new NioEventLoopGroup(netConfigManager.maxIOThreadNumPerEventLoop(), new DefaultThreadFactory("IO_THREAD"));
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
