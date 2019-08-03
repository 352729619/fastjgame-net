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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.EventLoopChooserFactory;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.MultiThreadEventLoopGroup;
import com.wjybxx.fastjgame.misc.NetContext;
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

    public NetEventLoopGroupImp(int nThreads, @Nonnull ThreadFactory threadFactory) {
        super(nThreads, threadFactory, null);
    }

    public NetEventLoopGroupImp(int nThreads, @Nonnull ThreadFactory threadFactory, @Nullable EventLoopChooserFactory chooserFactory) {
        super(nThreads, threadFactory, chooserFactory, null);
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

    @Nonnull
    @Override
    protected NetEventLoop newChild(ThreadFactory threadFactory, Object context) {
        return new NetEventLoopImp(this, threadFactory);
    }
}
