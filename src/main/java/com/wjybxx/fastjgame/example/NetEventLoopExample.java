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

package com.wjybxx.fastjgame.example;

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.HttpResponseHelper;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.HttpServerInitializer;
import com.wjybxx.fastjgame.net.initializer.TCPClientChannelInitializer;
import com.wjybxx.fastjgame.net.initializer.TCPServerChannelInitializer;
import com.wjybxx.fastjgame.utils.NetUtils;
import okhttp3.Call;
import okhttp3.Response;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;

/**
 * 网络事件循环测试用例。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public class NetEventLoopExample {

    public static void main(String[] args) {
        final CodecHelper codecHelper = CodecHelper.newInstance(new ExampleHashMappingStrategy("com.wjybxx.fastjgame.example"),
                new ProtoBufMessageSerializer());

        // 线程数可以动态调整
        int threadNum = 2;
        NetEventLoopGroup netGroup = new NetEventLoopGroupImp(threadNum, new DefaultThreadFactory("NET-EVENT-LOOP"), RejectedExecutionHandlers.reject());
        EventLoop userEventLoop1 = new DefaultEventLoop(null, new DefaultThreadFactory("SERVER"), RejectedExecutionHandlers.reject());
        EventLoop userEventLoop2 = new DefaultEventLoop(null, new DefaultThreadFactory("CLIENT"), RejectedExecutionHandlers.reject());

        final int serverGuid = 1;
        final int clientGuid = 2;

        ListenableFuture<NetContext> future1 = netGroup.createContext(serverGuid, RoleType.TEST_SERVER, userEventLoop1);
        ListenableFuture<NetContext> future2 = netGroup.createContext(clientGuid, RoleType.TEST_CLIENT, userEventLoop2);

        future1.awaitUninterruptibly();
        future2.awaitUninterruptibly();;

        NetContext context1 = future1.tryGet();
        NetContext context2 = future2.tryGet();

        ListenableFuture<HostAndPort> bindFuture = startTcpServer(codecHelper, context1);
        HostAndPort bindAddress = bindFuture.tryGet();
        System.out.println(bindAddress);
        // ----
        ListenableFuture<?> connect = startTcpClient(codecHelper, serverGuid, context2, bindAddress);

        connect.awaitUninterruptibly();

        syncGetTest(context1);

        asynGetTest(context1);

        startHttpService(context2);

        netGroup.terminationFuture().awaitUninterruptibly();
    }

    private static ListenableFuture<?> startTcpClient(CodecHelper codecHelper, int serverGuid, NetContext context2, HostAndPort bindAddress) {
        TCPClientChannelInitializer tcpClientChannelInitializer = context2.newTcpClientInitializer(serverGuid, codecHelper);
        return context2.connect(serverGuid, RoleType.TEST_SERVER, bindAddress, () -> tcpClientChannelInitializer,
                new ClientLifeAware(), new ClientMessageHandler());
    }

    private static ListenableFuture<HostAndPort> startTcpServer(CodecHelper codecHelper, NetContext context1) {
        TCPServerChannelInitializer tcpServerChannelInitializer = context1.newTcpServerInitializer(codecHelper);
        ListenableFuture<HostAndPort> bindFuture = context1.bindRange(NetUtils.getLocalIp(), new PortRange(10000, 10050), tcpServerChannelInitializer,
                new SeverLifeAware(), new ServerMessageHandler());
        bindFuture.awaitUninterruptibly();
        return bindFuture;
    }

    private static void syncGetTest(NetContext context1) {
        try {
            Response response = context1.syncGet("www.baidu.com", new HashMap<>());
            System.out.println("syncGetCode: " + response.code());
            if (response.body() != null) {
                System.out.println(response.body().string());
            }
            NetUtils.closeQuietly(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void asynGetTest(NetContext context1) {
        context1.asyncGet("www.baidu.com", new HashMap<>(), new OkHttpCallback() {
            @Override
            public void onFailure(@Nonnull Call call, @Nonnull IOException cause) {
                printThreadInfo();
                System.out.println("asyncGet onFailure.");
            }

            @Override
            public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
                printThreadInfo();
                System.out.println("[asyncGetCode]: " + response.code());
                if (response.body() != null) {
                    System.out.println(response.body().string());
                }
                NetUtils.closeQuietly(response);
            }
        });
    }

    private static void startHttpService(NetContext context2) {
        HttpServerInitializer httpServerInitializer = context2.newHttpServerInitializer();
        ListenableFuture<HostAndPort> httpPortFuture = context2.bindRange(NetUtils.getLocalIp(), new PortRange(20001, 200050), httpServerInitializer, (httpSession, path, requestParams) -> {
            System.out.println("onHttpRequest, path = " + path + ", param = " + requestParams.toString());
            httpSession.writeAndFlush(HttpResponseHelper.newJsonResponse("Hello World"));
        });
        httpPortFuture.awaitUninterruptibly();
        System.out.println("httpPort = " + httpPortFuture.tryGet());
    }

    private static class SeverLifeAware implements SessionLifecycleAware<S2CSession> {
        @Override
        public void onSessionConnected(S2CSession session) {
            printThreadInfo();
            System.out.println("Server onSessionConnected " + session);
        }

        @Override
        public void onSessionDisconnected(S2CSession session) {
            printThreadInfo();
            System.out.println("Server onSessionDisconnected " + session);
        }
    }

    private static class ServerMessageHandler implements MessageHandler {

        @Override
        public void onMessage(Session session, Object message) throws Exception {
            printThreadInfo();
            if (message instanceof p_center_scene.p_center_cross_scene_hello) {
                p_center_scene.p_center_cross_scene_hello hello = (p_center_scene.p_center_cross_scene_hello) message;
                System.out.println("Server onMessage: " + hello);
                // 发回去
                session.sendMessage(message);
            }
        }

        @Override
        public void onRpcRequest(Session session, Object request, RpcResponseChannel responseChannel) throws Exception {
            printThreadInfo();

            if (request instanceof p_center_scene.p_center_cross_scene_hello) {
                p_center_scene.p_center_cross_scene_hello hello = (p_center_scene.p_center_cross_scene_hello) request;
                System.out.println("Server onRpcRequest: " + request);
                // 再发回去
                responseChannel.write(new RpcResponse(RpcResultCode.SUCCESS, hello));
            }
        }
    }

    private static class ClientLifeAware implements SessionLifecycleAware<C2SSession> {

        @Override
        public void onSessionConnected(C2SSession session) {
            printThreadInfo();
            p_center_scene.p_center_cross_scene_hello.Builder builder = p_center_scene.p_center_cross_scene_hello.newBuilder();
            builder.setPlatformNumber(1);
            builder.setServerId(2);
            session.sendMessage(builder.build());
        }

        @Override
        public void onSessionDisconnected(C2SSession session) {
            printThreadInfo();
            System.out.println("Client onSessionDisconnected " + session);
        }
    }

    private static class ClientMessageHandler implements MessageHandler {

        @Override
        public void onMessage(Session session, Object message) throws Exception {
            printThreadInfo();
            System.out.println("Client client onMessage:" + message);

            // 再发回去
            session.rpc(message).addCallback((f) -> {
                printThreadInfo();
                System.out.println("rpc callback " + f.getBody());
            });

            p_center_scene.p_center_cross_scene_hello.Builder builder = p_center_scene.p_center_cross_scene_hello.newBuilder();
            builder.setPlatformNumber(2);
            builder.setServerId(55);
            RpcResponse rpcResponse = session.syncRpc(builder.build(), 100);
            printThreadInfo();
            System.out.println("syncRpc response: " + rpcResponse);
        }

        @Override
        public void onRpcRequest(Session session, Object request, RpcResponseChannel responseChannel) throws Exception {

        }
    }

    private static void printThreadInfo() {
        System.out.println("\nThread - " + Thread.currentThread());
    }
}
