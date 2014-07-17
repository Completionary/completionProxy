package de.completionary.proxy;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.ElasticsearchException;

import de.completionary.proxy.elasticsearch.SuggestionIndex;

//import static org.elasticsearch.common.xcontent.XContentFactory.*;

public class Main {

	public static void main(String[] args) {
		SuggestionIndex client = new SuggestionIndex("index");
		
		try {
			client.test();
		} catch (ElasticsearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
