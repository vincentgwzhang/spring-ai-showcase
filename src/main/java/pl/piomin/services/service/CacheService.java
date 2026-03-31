package pl.piomin.services.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final Cache<String, String> cache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();

    public String buildKey(String query, List<String> docs) {
        return Integer.toHexString((query + docs.toString()).hashCode());
    }

    public boolean contains(String key) {
        return cache.getIfPresent(key) != null;
    }

    public String get(String key) {
        return cache.getIfPresent(key);
    }

    public void put(String key, String value) {
        cache.put(key, value);
    }
}