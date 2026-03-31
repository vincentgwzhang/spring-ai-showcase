package pl.piomin.services.service;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromptService {

    public String buildPrompt(String query, List<String> docs) {
        String context = String.join("\n", docs);

        return "Answer ONLY using context.\n\nContext:\n" + context +
            "\n\nQuestion:\n" + query;
    }
}