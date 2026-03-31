package pl.piomin.services.service;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final VectorService vectorService;
    private final PromptService promptService;
    private final ChatClient chatClient;
    private final CacheService cacheService;

    public RagService(VectorService vectorService,
                      PromptService promptService,
                      ChatClient.Builder builder,
                      CacheService cacheService) {
        this.vectorService = vectorService;
        this.promptService = promptService;
        this.chatClient = builder.build();
        this.cacheService = cacheService;
    }

    public String ask(String query) {
        long start = System.currentTimeMillis();

        // 1. retrieve docs
        List<String> docs = vectorService.search(query);

        // 2. cache key
        String cacheKey = cacheService.buildKey(query, docs);

        // 3. cache hit
        if (cacheService.contains(cacheKey)) {
            return cacheService.get(cacheKey);
        }

        // 4. build prompt
        String prompt = promptService.buildPrompt(query, docs);

        // 5. call LLM
        String response = chatClient.prompt()
            .user(prompt)
            .call()
            .content();

        // 6. cache
        cacheService.put(cacheKey, response);

        long end = System.currentTimeMillis();

        // logging
        System.out.println("[QUERY] " + query);
        System.out.println("[DOCS] " + docs);
        System.out.println("[PROMPT] " + prompt);
        System.out.println("[RESPONSE] " + response);
        System.out.println("[LATENCY] " + (end - start) + "ms");

        return response;
    }
}