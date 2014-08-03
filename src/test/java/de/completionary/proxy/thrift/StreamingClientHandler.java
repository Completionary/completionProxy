package de.completionary.proxy.thrift;

import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.streaming.StreamingClientService;

public class StreamingClientHandler implements
		StreamingClientService.AsyncIface {

	@Override
	public void updateStatistics(Map<String, StreamedStatisticsField> stream,
			AsyncMethodCallback resultHandler) throws TException {
		System.out.println("Received stream!!!!!!!!!!!!!:");
		for (Map.Entry<String, StreamedStatisticsField> entry : stream
				.entrySet()) {
			System.out.println(entry.getKey() + "\t"
					+ entry.getValue().numberOfQueries);
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		resultHandler.onComplete(null);
	}
}
