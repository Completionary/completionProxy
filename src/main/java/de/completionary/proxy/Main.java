package de.completionary.proxy;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.ElasticsearchException;

import de.completionary.proxy.elasticsearch.SuggestionIndex;

//import static org.elasticsearch.common.xcontent.XContentFactory.*;

public class Main {

	public static void main(String[] args) {
		SuggestionIndex client = new SuggestionIndex("index");

		Random r = new Random();

		int numberOfQueries = 1000;
		for (int i = 0; i < numberOfQueries; i++) { // heat up cpu
			String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
		}

		long startTime = System.currentTimeMillis();
		for (int i = 0; i < numberOfQueries; i++) {
			String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
		}
		long randomTime = (System.currentTimeMillis() - startTime);

		startTime = System.currentTimeMillis();
		for (int i = 0; i < numberOfQueries; i++) {
			String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
			client.findSuggestionsFor(query, 15);
		}
		float time = (System.currentTimeMillis() - startTime - randomTime)
				/ (float) numberOfQueries;

		System.out.println(time + " ms per query");
	}
}
