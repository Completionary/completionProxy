package de.completionary.proxy.thrift.server;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;

import de.completionary.proxy.helper.ProxyOptions;
import de.completionary.proxy.thrift.handler.AdminHandler;
import de.completionary.proxy.thrift.services.admin.AdminService;

public class AdminServer extends Thread {

	@Override
	public void run() {
		try {
			TNonblockingServerTransport trans = new TNonblockingServerSocket(
					ProxyOptions.ADMIN_SERVER_PORT);
			TNonblockingServer.Args args = new TNonblockingServer.Args(trans);
			args.transportFactory(new TFramedTransport.Factory());
			args.protocolFactory(new TBinaryProtocol.Factory());
			args.processor(new AdminService.AsyncProcessor<AdminService.AsyncIface>(
					new AdminHandler()));
			TServer server = new TNonblockingServer(args);
			server.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
