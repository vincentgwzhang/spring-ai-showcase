package pl.piomin.services.functions.stock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import pl.piomin.services.functions.stock.api.DailyStockData;
import pl.piomin.services.functions.stock.api.StockData;

import java.util.function.Function;

@Slf4j
public class StockService implements Function<StockRequest, StockResponse> {

    private final RestTemplate restTemplate;

    public StockService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${STOCK_API_KEY}")
    String apiKey;

    @Override
    public StockResponse apply(StockRequest stockRequest) {
        StockData data = restTemplate.getForObject("https://api.twelvedata.com/time_series?symbol={0}&interval=1min&outputsize=1&apikey={1}",
                StockData.class,
                stockRequest.company(),
                apiKey);
        DailyStockData latestData = data.getValues().get(0);
        log.info("Get stock prices: {} -> {}", stockRequest.company(), latestData.getClose());
        return new StockResponse(Float.parseFloat(latestData.getClose()));
    }
}
