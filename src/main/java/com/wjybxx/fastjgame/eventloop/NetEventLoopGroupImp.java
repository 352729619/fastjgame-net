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

package com.wjybxx.fastjgame.eventloop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.module.NetEventLoopGroupModule;
import com.wjybxx.fastjgame.net.RoleType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * 网络事件循环组
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class NetEventLoopGroupImp extends MultiThreadEventLoopGroup implements NetEventLoopGroup{

    /**
     * @see #NetEventLoopGroupImp(int, ThreadFactory, RejectedExecutionHandler, EventLoopChooserFactory)
     */
    public NetEventLoopGroupImp(int nThreads,
                                @Nonnull ThreadFactory threadFactory,
                                @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
        this(nThreads, threadFactory, rejectedExecutionHandler, null);
    }

    /**
     *
     * @param nThreads 线程组内的线程数量
     * @param threadFactory 线程工厂
     * @param rejectedExecutionHandler 任务拒绝策略
     * @param chooserFactory 负载均衡算法
     */
    public NetEventLoopGroupImp(int nThreads,
                                @Nonnull ThreadFactory threadFactory,
                                @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                                @Nullable EventLoopChooserFactory chooserFactory) {
        super(nThreads, threadFactory, rejectedExecutionHandler, chooserFactory, Guice.createInjector(new NetEventLoopGroupModule()));
    }

    @Nonnull
    @Override
    public NetEventLoop next() {
        return (NetEventLoop) super.next();
    }

    @Override
    public ListenableFuture<NetContext> createContext(long localGuid, RoleType localRole, EventLoop localEventLoop) {
        return next().createContext(localGuid, localRole, localEventLoop);
    }

    /**
     * 自己把自己坑了一把，这里是超类构建的时候调用的，此时子类属性都是null，因此newChild需要的属性必须在context中
     */
    @Nonnull
    @Override
    protected NetEventLoop newChild(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler, Object context) {
        return new NetEventLoopImp(this, threadFactory, rejectedExecutionHandler, (Injector) context);
    }

}
