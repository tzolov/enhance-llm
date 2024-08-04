package com.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLinePropertySource(VectorStore store, ObjectMapper mapper,
			ChatClient.Builder builder) {

		return args -> {

			// disable the Loading after the first run.
			load(store, mapper);
			System.out.println("Loaded %d documents");

			Thread.sleep(10000);

			var response = builder.build()
				.prompt()
				.advisors(new QuestionAnswerAdvisor(store, SearchRequest.defaults()))
				.user("When was Tomoaki Komorida born?")
				.call()
				.content();

			System.out.println(response);
		};
	}

	private void load(VectorStore store, ObjectMapper mapper) throws IOException {
		ObjectReader reader = mapper.readerFor(Map.class);

		TokenTextSplitter splitter = new TokenTextSplitter(200, 50, 5, 400, true);

		List<Document> list = new ArrayList<>();
		reader
			.readValues(
					new File("/Users/christiantzolov/Dev/projects/demo/enhance-llm/data/databricks-dolly-15k.jsonl"))
			.forEachRemaining(line -> {
				Map map = (Map) line;
				if (!"closed_qa".equals(map.get("category"))) {
					return;
				}
				// Document doc = new Document("%s. %s".formatted(map.get("instruction"),
				// map.get("context")));
				// String context = ("DATA: " + map.get("context")).trim();
				String context = ("" + map.get("context")).trim();
				if (StringUtils.hasText(context)) {
					Document doc = new Document(context);
					List<Document> splitDocs = splitter.split(doc);
					list.addAll(splitDocs);
				}
			});

		store.add(list);

		// subBatchSplit(list, 50).stream().forEach(store::add);
	}

	// public static List<List<Document>> subBatchSplit(List<Document> docs, int batchSize) {
	// 	int totalCount = docs.size();
	// 	List<List<Document>> results = new ArrayList<>();
	// 	int start = 0;
	// 	while (start < totalCount) {
	// 		results.add(docs.subList(start, Math.min(docs.size(), start + batchSize)));
	// 		start += batchSize;
	// 	}
	// 	return results;
	// }

}