package com.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.ObjectMapper;

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

			Thread.sleep(5000);

			System.out.println("Ready to chat");

			for (int i = 0; i < 10; i++) {
				var response = builder.build()
					.prompt()
					.advisors(new QuestionAnswerAdvisor(store, SearchRequest.defaults().withTopK(1)))
					.user("When was Tomoaki Komorida born?")
					.call()
					.content();

				System.out.println("Response# " + i +  " -> " + response);
			}
		};
	}

	private void load(VectorStore store, ObjectMapper mapper) throws IOException {

		List<Document> list = new ArrayList<>();

		mapper.readerFor(Map.class)
			.readValues(
					new File("/Users/christiantzolov/Dev/projects/demo/enhance-llm/data/databricks-dolly-15k.jsonl"))
			.forEachRemaining(line -> {
				Map map = (Map) line;
				if ("closed_qa".equals(map.get("category"))) {
					list.add(new Document("Content: %s. %s".formatted(map.get("instruction"), map.get("context"))));
				}
			});

		store.add(list);
	}
}