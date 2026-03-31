package pl.piomin.services.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public MessageWindowChatMemory messageWindowChatMemory() {
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, MessageWindowChatMemory messageWindowChatMemory) {
        return chatClientBuilder.defaultAdvisors(
            PromptChatMemoryAdvisor.builder(messageWindowChatMemory).build(),
            new SimpleLoggerAdvisor()
        ).build();
    }

}
