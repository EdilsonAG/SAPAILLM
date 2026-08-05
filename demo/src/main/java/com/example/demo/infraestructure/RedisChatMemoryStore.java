package com.example.demo.infraestructure;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

@Component
public class RedisChatMemoryStore implements ChatMemoryStore{
    private static final String PREFIX = "chat:memory:";
    private static final Duration TTL = Duration.ofHours(24); // expira sessão inativa

    private final StringRedisTemplate redisTemplate;

    public RedisChatMemoryStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redisTemplate.opsForValue().get(PREFIX + memoryId);
        if (json == null) {
            return Collections.emptyList();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        redisTemplate.opsForValue().set(PREFIX + memoryId, json, TTL);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(PREFIX + memoryId);
    }
}
