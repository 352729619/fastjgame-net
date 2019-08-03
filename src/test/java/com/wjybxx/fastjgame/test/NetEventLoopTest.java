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

package com.wjybxx.fastjgame.test;

import com.wjybxx.fastjgame.concurrent.DefaultEventLoop;
import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.TCPClientChannelInitializer;
import com.wjybxx.fastjgame.net.initializer.TCPServerChannelInitializer;
import com.wjybxx.fastjgame.protobuffer.p_center_scene;
import com.wjybxx.fastjgame.protobuffer.p_common;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class NetEventLoopTest {

    public static void main(String[] args) {
        final CodecHelper codecHelper = CodecHelper.newInstance(new HashMappingStrategy(p_common.class,
                        p_center_scene.class),
                new ProtoBufMessageSerializer());

        NetEventLoopGroup netGroup = new NetEventLoopGroupImp(2, new DefaultThreadFactory("NET-EVENT-LOOP"));
        EventLoop userEventLoop1 = new DefaultEventLoop(null, new DefaultThreadFactory("USER1"));
        EventLoop userEventLoop2 = new DefaultEventLoop(null, new DefaultThreadFactory("USER2"));

        final int serverGuid = 1;
        final int clientGuid = 2;

        ListenableFuture<NetContext> future1 = netGroup.createContext(serverGuid, RoleType.TEST_SERVER, userEventLoop1);
        ListenableFuture<NetContext> future2 = netGroup.createContext(clientGuid, RoleType.TEST_CLIENT, userEventLoop2);

        future1.awaitUninterruptibly();
        future2.awaitUninterruptibly();;

        NetContext context1 = future1.tryGet();
        NetContext context2 = future2.tryGet();

        TCPServerChannelInitializer tcpServerChannelInitializer = new TCPServerChannelInitializer(context1.localGuid(), 8192, codecHelper,
                context1.netEventManager());
        ListenableFuture<HostAndPort> bindFuture = context1.bindRange(false, new PortRange(10000, 10050), () -> tcpServerChannelInitializer,
                new SeverLifeAware(), new ServerMessageHandler());
        bindFuture.awaitUninterruptibly();
        HostAndPort bindAddress = bindFuture.tryGet();
        System.out.println(bindAddress);
        // ----
        TCPClientChannelInitializer tcpClientChannelInitializer = new TCPClientChannelInitializer(context2.localGuid(), serverGuid, 8192,
                codecHelper, context2.netEventManager());
        ListenableFuture<?> connect = context2.connect(serverGuid, RoleType.TEST_SERVER, bindAddress, () -> tcpClientChannelInitializer,
                new ClientLifeAware(), new ClientMessageHandler());

        connect.awaitUninterruptibly();

        netGroup.terminationFuture().awaitUninterruptibly();
    }

    private static class SeverLifeAware implements SessionLifecycleAware<S2CSession> {
        @Override
        public void onSessionConnected(S2CSession session) {
            System.out.println("onSessionConnected " + session);
        }

        @Override
        public void onSessionDisconnected(S2CSession session) {
            System.out.println("onSessionDisconnected " + session);
        }
    }

    private static class ServerMessageHandler implements MessageHandler {

        @Override
        public void onMessage(Session session, Object message) throws Exception {
            if (message instanceof p_center_scene.p_center_cross_scene_hello) {
                p_center_scene.p_center_cross_scene_hello hello = (p_center_scene.p_center_cross_scene_hello) message;
                System.out.println("Server onMessage: " + hello);
                // 发回去
                session.sendMessage(message);
            }
        }

        @Override
        public void onRpcRequest(Session session, Object request, RpcResponseChannel responseChannel) throws Exception {
            if (request instanceof p_center_scene.p_center_cross_scene_hello) {
                p_center_scene.p_center_cross_scene_hello hello = (p_center_scene.p_center_cross_scene_hello) request;
                System.out.println("onRpcRequest: " + request);
                // 再发回去
                responseChannel.write(new RpcResponse(RpcResultCode.SUCCESS, hello));
            }
        }
    }

    private static class ClientLifeAware implements SessionLifecycleAware<C2SSession> {

        @Override
        public void onSessionConnected(C2SSession session) {
            p_center_scene.p_center_cross_scene_hello.Builder builder = p_center_scene.p_center_cross_scene_hello.newBuilder();
            builder.setPlatformNumber(1);
            builder.setServerId(2);
            session.sendMessage(builder.build());
        }

        @Override
        public void onSessionDisconnected(C2SSession session) {
            System.out.println("onSessionDisconnected " + session);
        }
    }

    private static class ClientMessageHandler implements MessageHandler {

        @Override
        public void onMessage(Session session, Object message) throws Exception {
            System.out.println("client onMessage:" + message);

            // 再发回去
            session.rpc(message).addCallback((f) -> {
                System.out.println("rpc callback " + f.getBody());
            });
        }

        @Override
        public void onRpcRequest(Session session, Object request, RpcResponseChannel responseChannel) throws Exception {

        }
    }

}
