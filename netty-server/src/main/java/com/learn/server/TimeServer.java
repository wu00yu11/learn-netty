package com.learn.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: netty
 * @description:
 * @author: zjj
 * @create: 2019-12-03 11:41
 **/
public class TimeServer {
    private static final Logger logger = LoggerFactory.getLogger(TimeServer.class);
    private static final String host = "127.0.0.1";
    private static final int port = 9898;

    public static void main(String[] args) {
        Thread thread = new Thread(()->{
            new TimeServer().bind(host,port);
        });

        thread.setDaemon(true);
        thread.start();
        for (;;) {
        }
    }

    public void bind(String host,int port) {
        logger.info("server running");
        /**
         * interface EventLoopGroup extends EventExecutorGroup extends ScheduledExecutorService extends ExecutorService
         * 配置服务端的 NIO 线程池,用于网络事件处理，实质上他们就是 Reactor 线程组
         * bossGroup 用于服务端接受客户端连接，workerGroup 用于进行 SocketChannel 网络读写*/
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            /** ServerBootstrap 是 Netty 用于启动 NIO 服务端的辅助启动类，用于降低开发难度
             * */
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChildChannelHandler());

            /**服务器启动辅助类配置完成后，调用 bind 方法绑定监听端口，调用 sync 方法同步等待绑定操作完成*/
            ChannelFuture f = b.bind(host,port).sync();

            logger.info(Thread.currentThread().getName() + ",服务器开始监听端口，等待客户端连接.........");
            /**下面会进行阻塞，等待服务器连接关闭之后 main 方法退出，程序结束
             *
             * */
            f.channel().closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    /**优雅退出，释放线程池资源*/
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                    logger.info(channelFuture.channel().toString()+ "链路关闭");
                }
            }).sync();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        } finally {
            /**优雅退出，释放线程池资源*/
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            logger.info("server end");
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel arg0) throws Exception {
            arg0.pipeline().addLast(new TimeServerHandler());
        }
    }

}
