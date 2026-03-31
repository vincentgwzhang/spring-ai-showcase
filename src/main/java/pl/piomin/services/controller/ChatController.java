package pl.piomin.services.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.piomin.services.dto.ChatMessageDto;
import pl.piomin.services.service.RagService;

@RestController
@RequestMapping("/api")
class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public String ask(@RequestBody ChatMessageDto dto) {
        return ragService.ask(dto.getMessage());
    }
}