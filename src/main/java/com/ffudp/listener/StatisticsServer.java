package com.ffudp.listener;

import com.ffudp.msg.PublishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
@Component
@Slf4j
public class StatisticsServer implements CommandLineRunner {


	@Autowired
	private PublishService publishService;
	//每次发送接收的数据包大小
	private final int MAX_BUFF_SIZE = 1024 * 10;
	//服务端监听端口，客户端也通过该端口发送数据
	@Value("${server.ffport}")
	private int port;
	private DatagramChannel channel;
	private Selector selector;
	private ScheduledExecutorService es = Executors.newScheduledThreadPool(1);

	@Override
	public void run(String... args) throws Exception {
		init();
	}

	public void init() throws IOException {
		//创建通道和选择器
		selector = Selector.open();
		channel = DatagramChannel.open();
		//设置为非阻塞模式
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(port));
		//将通道注册至selector，监听只读消息（此时服务端只能读数据，无法写数据）
		channel.register(selector, SelectionKey.OP_READ);
		//使用线程的方式，保证服务端持续等待接收客户端数据
		es.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					while(selector.select() > 0) {
						Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
						while(iterator.hasNext()) {
							SelectionKey key = iterator.next();
							try {
								iterator.remove();
								if(key.isReadable()) {
									doReceive(key);
								}
							} catch (Exception e) {
								log.error("SelectionKey receive exception", e);
								try {
									if (key != null) {
										key.cancel();
										key.channel().close();
									}
								} catch (ClosedChannelException cex) {
									log.error("Close channel exception", cex);
								}
							}
						}
					}
				} catch (IOException e) {
					log.error("selector.select exception", e);
				}
			}
		}, 0L, 2L, TimeUnit.MINUTES);
	}

	//处理接收到的数据
	private void doReceive(SelectionKey key) throws Exception {
		String content = "";
		DatagramChannel sc = (DatagramChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFF_SIZE);
		buffer.clear();
		sc.receive(buffer);
		buffer.flip();
		byte[] buf = new byte[buffer.limit()];
		buffer.get(buf);
		String inf = bytesToHexString(buf);
//		byte[] b2 =hexStr2Byte(inf);
//		String key0 = udpService.service(b2,0,b2.length);
		publishService.publish("Parsing",inf);

		buffer.clear();
	}

	public static final String bytesToHexString(byte[] bArray) {
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = 0; i < bArray.length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2)
				sb.append(0);
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}
	public static byte[] hexStr2Byte(String hex) {
		ByteBuffer bf = ByteBuffer.allocate(hex.length() / 2);
		for (int i = 0; i < hex.length(); i++) {
			String hexStr = hex.charAt(i) + "";
			i++;
			hexStr += hex.charAt(i);
			byte b = (byte) Integer.parseInt(hexStr, 16);
			bf.put(b);
		}
		return bf.array();
	}
}
