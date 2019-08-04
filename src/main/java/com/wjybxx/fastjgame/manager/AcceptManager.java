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
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.net.BindException;

/**
 * 建立链接的帮助类。
 * 起名真的太难了，我之前项目就是这名，感觉不是很准确，但是又找不到更准确的。
 *
 * 它本身是线程安全的，但是除了NetWorld，其它world不应该直接使用它。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/16 11:00
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class AcceptManager {

    private static final Logger logger = LoggerFactory.getLogger(AcceptManager.class);

    private final NetConfigManager netConfigManager;
    private final NettyThreadManager nettyThreadManager;

    @Inject
    public AcceptManager(NetConfigManager netConfigManager, NettyThreadManager nettyThreadManager) {
        this.netConfigManager = netConfigManager;
        this.nettyThreadManager = nettyThreadManager;
    }

    /**
     * 监听某个端口,阻塞直到成功或失败。
     * 参数意义可参考{@link java.net.StandardSocketOptions}
     * 或 - https://www.cnblogs.com/googlemeoften/p/6082785.html
     * @param host 地址
     * @param port 需要绑定的端口
     * @param initializer channel初始化类，根据使用的协议(eg:tcp,ws) 和 序列化方式(eg:json,protoBuf)确定
     * @return 监听成功成功则返回绑定的地址，失败则返回null
     */
    public HostAndPort bind(String host, int port, ChannelInitializer<SocketChannel> initializer) throws BindException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(nettyThreadManager.getBossGroup(), nettyThreadManager.getWorkerGroup());

        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.childHandler(initializer);

        // parentGroup参数
        serverBootstrap.option(ChannelOption.SO_BACKLOG,400);
        serverBootstrap.option(ChannelOption.SO_REUSEADDR,true);

        // childGroup参数
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE,false);
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY,true);
        serverBootstrap.childOption(ChannelOption.SO_SNDBUF, netConfigManager.sndBufferAsServer());
        serverBootstrap.childOption(ChannelOption.SO_RCVBUF, netConfigManager.revBufferAsServer());
        serverBootstrap.childOption(ChannelOption.SO_LINGER,0);
        serverBootstrap.childOption(ChannelOption.SO_REUSEADDR,true);

        ChannelFuture channelFuture = serverBootstrap.bind(host, port);
        try {
            channelFuture.sync();
            logger.info("bind {}:{} success.",host,port);
            return new HostAndPort(host, port);
        } catch (InterruptedException e) {
            // ignore e
            NetUtils.closeQuietly(channelFuture);
            Thread.currentThread().interrupt();
        } catch (Exception e){
            // ignore, may another process bind this port
            NetUtils.closeQuietly(channelFuture);
        }
        throw new BindException("can't bind " + host + ":" + port);
    }

    /**
     * 在某个端口范围内选择一个端口监听.
     * @param host 地址
     * @param portRange 端口范围
     * @param initializer channel初始化类
     * @return 监听成功的端口号，失败返回null
     */
    public HostAndPort bindRange(String host, PortRange portRange, ChannelInitializer<SocketChannel> initializer) throws BindException {
        if (portRange.startPort <=0){
            throw new IllegalArgumentException("fromPort " + portRange.startPort);
        }
        if (portRange.startPort > portRange.endPort){
            throw new IllegalArgumentException("fromPort " + portRange.startPort + " toPort " + portRange.endPort);
        }
        for (int port = portRange.startPort; port<= portRange.endPort; port++){
            try {
                return bind(host, port, initializer);
            }catch (BindException e){
                // ignore
            }
        }
        throw new BindException("can't bind port from " + portRange.startPort + " to " + portRange.endPort);
    }

    /**
     * 异步建立连接localHost
     * @param hostAndPort 服务器地址
     * @param initializer channel初始化类，根据使用的协议(eg:tcp,ws) 和 序列化方式(eg:json,protoBuf)确定
     * @return channelFuture 注意使用{@link ChannelFuture#sync()} 会抛出异常。
     * 使用{@link ChannelFuture#await()} 和{@link ChannelFuture#isSuccess()} 安全处理。
     * 此外，使用channel 需要调用 {@link Channel#isActive()}检查是否成功和远程建立连接
     */
    public ChannelFuture connectAsyn(HostAndPort hostAndPort, ChannelInitializer<SocketChannel> initializer){
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.group(nettyThreadManager.getWorkerGroup());

        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(initializer);

        bootstrap.option(ChannelOption.SO_KEEPALIVE,false);
        bootstrap.option(ChannelOption.TCP_NODELAY,true);
        bootstrap.option(ChannelOption.SO_SNDBUF, netConfigManager.sndBufferAsClient());
        bootstrap.option(ChannelOption.SO_RCVBUF, netConfigManager.revBufferAsClient());
        bootstrap.option(ChannelOption.SO_LINGER,0);
        bootstrap.option(ChannelOption.SO_REUSEADDR,true);
        return bootstrap.connect(hostAndPort.getHost(),hostAndPort.getPort());
    }

    /**
     * 同步建立连接
     * @param hostAndPort 服务器地址
     * @param initializer channel初始化类，根据使用的协议(eg:tcp,ws) 和 序列化方式(eg:json,protoBuf)确定
     * @return 注意！使用channel 需要调用 {@link Channel#isActive()}检查是否成功和远程建立连接
     */
    public Channel connectSyn(HostAndPort hostAndPort, ChannelInitializer<SocketChannel> initializer) {
        ChannelFuture channelFuture= connectAsyn(hostAndPort,initializer);
        channelFuture.awaitUninterruptibly();
        return channelFuture.channel();
    }
}
