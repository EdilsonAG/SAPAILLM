package com.example.demo.domain.service;

import com.example.demo.domain.dto.ResponseIA;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
    wiringMode = AiServiceWiringMode.EXPLICIT,
    chatModel = "chatModel",
    chatMemoryProvider = "chatMemoryProvider"
    // contentRetriever REMOVIDO
)
public interface TriagemAgentService {

    @SystemMessage(fromResource = "/prompts/triagem.txt")
    ResponseIA chat(@MemoryId String sessionId,
                    @V("playbooks") String playbooks,
                    @UserMessage String message);
}