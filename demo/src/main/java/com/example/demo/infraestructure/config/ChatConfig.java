package com.example.demo.infraestructure.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.exec.CommandLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.example.demo.domain.service.TriagemAgentService;
import com.example.demo.infraestructure.RedisChatMemoryStore;
import com.example.demo.infraestructure.leader.PlaybookLoader;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.embedding.EmbeddingModel;
//import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
//import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

@Configuration
public class ChatConfig {

        private PlaybookLoader playbookLoader;

        @Value("${openai.api-key}")
        private String apiKey;

        @Value("${gemini.api-key}")
        private String GEMINIKEY;

        public ChatConfig(PlaybookLoader playbookLoader) {
                this.playbookLoader = playbookLoader;
        }

        @Bean
        ChatModel chatModel() {
                JsonObjectSchema schema = JsonObjectSchema.builder()
                                .addProperty("resposta", JsonStringSchema.builder()
                                                .description("OBRIGATORIO E NUNCA VAZIO. Texto em portugues que sera exibido ao usuario. Quando status=COLETANDO, contem as perguntas dos campos faltantes.")
                                                .build())
                                .addEnumProperty("status", List.of("COLETANDO", "PRONTO", "CANCELADO"))
                                .addProperty("description", JsonStringSchema.builder()
                                                .description("Titulo do ticket. String vazia enquanto status=COLETANDO.")
                                                .build())
                                .addProperty("longText", JsonStringSchema.builder()
                                                .description("Campos coletados. String vazia enquanto status=COLETANDO.")
                                                .build())
                                .addProperty("priority", JsonStringSchema.builder()
                                                .description("BAIXA, MEDIA ou ALTA. String vazia enquanto status=COLETANDO.")
                                                .build())
                                .addProperty("type", JsonStringSchema.builder()
                                                .description("Categoria do playbook. String vazia enquanto status=COLETANDO.")
                                                .build())
                                .required("resposta", "status")
                                .build();

                // return OllamaChatModel.builder()
                // .baseUrl("http://localhost:11434")
                // .modelName("qwen2.5:7b")
                // .temperature(0.2)
                // .numCtx(4096)
                // .numPredict(1024)
                // .responseFormat(ResponseFormat.builder()
                // .type(ResponseFormatType.JSON)
                // .jsonSchema(JsonSchema.builder()
                // .name("ResponseIA")
                // .rootElement(schema)
                // .build())
                // .build())
                // .supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
                // .timeout(Duration.ofMinutes(5))
                // .logRequests(true)
                // .logResponses(true)
                // .build();

                // return OpenAiChatModel.builder()
                // .apiKey(apiKey)
                // .modelName("gpt-5.6-luna")
                // .temperature(1.0)
                // .responseFormat(ResponseFormat.builder()
                // .type(ResponseFormatType.JSON)
                // .jsonSchema(JsonSchema.builder()
                // .name("ResponseIA")
                // .rootElement(schema) // mesmo schema que você já tem
                // .build())
                // .build())
                // .timeout(Duration.ofMinutes(2))
                // .logRequests(true)
                // .logResponses(true)
                // .build();

                return GoogleAiGeminiChatModel.builder()
                                .apiKey(GEMINIKEY) // vem de @Value("${gemini.api-key}")
                                .modelName("gemini-3.5-flash")
                                .temperature(1.0)
                                .timeout(Duration.ofMinutes(2))
                                .logRequestsAndResponses(true)
                                .build();

        }

        @Bean
        public EmbeddingStoreIngestor embeddingStoreIngestor(EmbeddingStore<TextSegment> embeddingStore,
                        EmbeddingModel embeddingModel) {
                return EmbeddingStoreIngestor.builder()
                                .embeddingStore(embeddingStore)
                                .embeddingModel(embeddingModel)
                                .documentSplitter(DocumentSplitters.recursive(1000, 0))
                                .build();
        }

        @Bean
        CommandLineRunner ingestPlaybooks(EmbeddingStoreIngestor embeddingStoreIngestor,
                        EmbeddingStore<TextSegment> embeddingStore,
                        EmbeddingModel embeddingModel) throws IOException {
                return args -> {

                        // só ingere se a tabela estiver vazia
                        var probe = embeddingStore
                                        .search(dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                                                        .queryEmbedding(embeddingModel.embed("probe").content())
                                                        .maxResults(1).build());
                        if (!probe.matches().isEmpty()) {
                                System.out.println("Playbooks já ingeridos, pulando.");
                                return;
                        }
                        List<Document> docs = new ArrayList<>();
                        var resources = new PathMatchingResourcePatternResolver()
                                        .getResources("classpath:playbooks/*.md");

                        for (Resource r : resources) {
                                String text = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                                docs.add(Document.from(text, Metadata.from("file", r.getFilename())));
                        }

                        embeddingStoreIngestor.ingest(docs);
                        System.out.println("Playbooks ingeridos: " + docs.size());
                };
        }

        @Bean
        public EmbeddingStore<TextSegment> embeddingStore() {
                return PgVectorEmbeddingStore.builder()
                                .host("localhost")
                                .port(5432)
                                .database("vectordb")
                                .user("postgres")
                                .password("postgres")
                                .table("playbooks_embeddings")
                                .dimension(1024) // OBRIGATÓRIO bater com o modelo — AllMiniLmL6V2 gera vetor de 384
                                                // dimensões
                                .build();
        }

        @Bean
        public ChatMemoryProvider chatMemoryProvider(RedisChatMemoryStore store) {
                return memoryId -> MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(20) // janela de mensagens, ajuste conforme necessidade
                                .chatMemoryStore(store)
                                .build();
        }

        // @Bean
        // EmbeddingModel embeddingModel() {
        // return new AllMiniLmL6V2EmbeddingModel();
        // }
        // MiniLM-L6-v2 → leu quase só inglês.
        // bge-m3 → leu ~100 idiomas, incluindo português.
        @Bean
        EmbeddingModel embeddingModel() {
                return OllamaEmbeddingModel.builder()
                                .baseUrl("http://localhost:11434")
                                .modelName("bge-m3")
                                .timeout(Duration.ofMinutes(3))
                                .build();
        }

        @Bean
        ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
                return EmbeddingStoreContentRetriever.builder()
                                .embeddingStore(embeddingStore)
                                .embeddingModel(embeddingModel)
                                .maxResults(2) // pega os 2 pedaços mais parecidos com a pergunta
                                .minScore(0.4) // ignora resultado se a similaridade for muito baixa
                                .build();
        }

        @Bean
        RetrievalAugmentor retrievalAugmentor(ChatModel chatModel, ContentRetriever contentRetriever) {
                return DefaultRetrievalAugmentor.builder()
                                .queryTransformer(new CompressingQueryTransformer(chatModel))
                                .contentRetriever(contentRetriever)
                                .build();
        }

        // @Bean
        // ContentRetriever contentRetriever() throws IOException {
        // EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        // EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        // List<Document> docs = new ArrayList<>();
        // var resources = new PathMatchingResourcePatternResolver()
        // .getResources("classpath:playbooks/*.md");
        // for (Resource r : resources) {
        // String text = new String(r.getInputStream().readAllBytes(),
        // StandardCharsets.UTF_8);
        // docs.add(Document.from(text, Metadata.from("file", r.getFilename())));
        // }

        // EmbeddingStoreIngestor.builder()
        // .embeddingStore(store)
        // .embeddingModel(embeddingModel)
        // // 1 playbook = 1 segmento (não quebrar no meio dos campos obrigatórios)
        // .documentSplitter(DocumentSplitters.recursive(2000, 0))
        // .build()
        // .ingest(docs);

        // return EmbeddingStoreContentRetriever.builder()
        // .embeddingStore(store)
        // .embeddingModel(embeddingModel)
        // .maxResults(2)
        // .minScore(0.4)
        // .build();
        // }

        // @Bean
        // TriagemAgentService triagemAgent(ChatModel chatModel,
        // ChatMemoryProvider chatMemoryProvider,
        // ContentRetriever contentRetriever) {
        // return AiServices.builder(TriagemAgentService.class)
        // .chatModel(chatModel)
        // .chatMemoryProvider(chatMemoryProvider)
        // .contentRetriever(contentRetriever)
        // .build();
        // }
}
