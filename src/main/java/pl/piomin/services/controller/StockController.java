package pl.piomin.services.controller;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import pl.piomin.services.functions.stock.api.DailyStockData;
import pl.piomin.services.functions.stock.api.Stock;
import pl.piomin.services.functions.stock.api.StockData;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/stocks")
@Slf4j
public class StockController {

    private final JsonMapper mapper = new JsonMapper();
    private final ChatClient chatClient;
    private final RewriteQueryTransformer.Builder rqtBuilder;
    private final RestTemplate restTemplate;
    private final VectorStore store;

    @Value("${STOCK_API_KEY}")
    private String apiKey;

    public StockController(ChatClient.Builder chatClientBuilder,
                           @Autowired(required = false) VectorStore store,
                           RestTemplate restTemplate) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.rqtBuilder = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder);
        this.store = store;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/load-data")
    void load() {
        final List<String> companies = List.of("AAPL", "MSFT", "GOOG", "AMZN", "META", "NVDA");
        for (String company : companies) {
            StockData data = restTemplate.getForObject("https://api.twelvedata.com/time_series?symbol={0}&interval=1day&outputsize=10&apikey={1}",
                    StockData.class,
                    company,
                    apiKey);
            if (data != null && data.getValues() != null) {
                var list = data.getValues().stream().map(DailyStockData::getClose).toList();
                var doc = Document.builder()
                        .id(company)
                        .text(mapper.writeValueAsString(new Stock(company, list)))
                        .build();
                store.add(List.of(doc));
                log.info("Document added: {}", company);
            }
        }
    }

    @GetMapping("/docs")
    List<Document> query() {
        SearchRequest searchRequest = SearchRequest.builder()
                .query("Find the most growth trends")
                .topK(2)
                .build();
        return store.similaritySearch(searchRequest);
    }

    @RequestMapping("/v1/most-growth-trend")
    String getBestTrend() {
        PromptTemplate pt = new PromptTemplate("""
                {query}.
                Which {target} is the most % growth?
                The 0 element in the prices table is the latest price, while the last element is the oldest price.
                """);

        Prompt p = pt.create(
                Map.of("query", "Find the most growth trends",
                       "target", "share")
        );

        return this.chatClient.prompt(p)
                .advisors(QuestionAnswerAdvisor.builder(store).build())
                .call()
                .content();
    }

    @RequestMapping("/v1-1/most-growth-trend")
    String getBestTrendV11() {
        PromptTemplate pt = new PromptTemplate("""
                Which share is the most % growth?
                The 0 element in the prices table is the latest price, while the last element is the oldest price.
                Return a full name of company instead of a market shortcut.
                """);

        SearchRequest searchRequest = SearchRequest.builder()
                .query("""
                Find the most growth trends.
                The 0 element in the prices table is the latest price, while the last element is the oldest price.
                """)
                .topK(3)
                .similarityThreshold(0.7)
                .build();

        return this.chatClient.prompt(pt.create())
                .advisors(QuestionAnswerAdvisor.builder(store).searchRequest(searchRequest).build())
                .call()
                .content();
    }

    @RequestMapping("/v2/most-growth-trend")
    String getBestTrendV2() {
        PromptTemplate pt = new PromptTemplate("""
                {query}.
                Which {target} is the most % growth?
                The 0 element in the prices table is the latest price, while the last element is the oldest price.
                """);

        Prompt p = pt.create(Map.of("query", "Find the most growth trends", "target", "share"));

        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(
                    VectorStoreDocumentRetriever
                        .builder()
                        .similarityThreshold(0.7)
                        .topK(3)
                        .vectorStore(store)
                        .build()
                )
                .queryTransformers(rqtBuilder.promptTemplate(pt).build())
                .build();

        return this.chatClient.prompt(p)
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .content();
    }

}
