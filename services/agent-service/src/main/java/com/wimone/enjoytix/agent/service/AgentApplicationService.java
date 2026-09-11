package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.model.AgentConversation;
import com.wimone.enjoytix.agent.model.AgentMessage;
import com.wimone.enjoytix.agent.model.AgentStreamEvent;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class AgentApplicationService {
    private final ConversationRepository repository;
    private final AgentResponseGenerator generator;
    private final IdGeneratorManager ids;
    private final MeterRegistry meters;

    public AgentApplicationService(ConversationRepository repository,
                                   AgentResponseGenerator generator,
                                   IdGeneratorManager ids,
                                   MeterRegistry meters) {
        this.repository = repository;
        this.generator = generator;
        this.ids = ids;
        this.meters = meters;
    }

    public AgentConversation createConversation(Long userId) {
        AgentConversation conversation = new AgentConversation(ids.nextId(), userId, Instant.now(), List.of());
        return repository.save(conversation);
    }

    public AgentConversation getConversation(Long id, Long userId) {
        AgentConversation conversation = requireConversation(id);
        checkOwner(conversation, userId);
        return conversation;
    }

    public Stream<AgentStreamEvent> sendMessage(Long id, Long userId, String content) {
        List<AgentStreamEvent> events = new ArrayList<>();
        sendMessage(id, userId, content, events::add);
        return events.stream();
    }

    public void sendMessage(Long id, Long userId, String content, AgentStreamListener listener) {
        AgentConversation conversation = requireConversation(id);
        checkOwner(conversation, userId);
        String question = content == null ? "" : content.trim();
        if (question.isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }
        repository.append(id, new AgentMessage("user", question, Instant.now()));
        long started = System.nanoTime();
        listener.onEvent(new AgentStreamEvent("conversation.started", id.toString()));
        StringBuilder answer = new StringBuilder();
        try (Stream<String> chunks = generator.generateStream(id, userId, question)) {
            var iterator = chunks.iterator();
            while (!listener.isCancelled() && iterator.hasNext()) {
                String chunk = iterator.next();
                if (chunk != null && !chunk.isEmpty() && !listener.isCancelled()) {
                    answer.append(chunk);
                    listener.onEvent(new AgentStreamEvent("token.delta", chunk));
                }
            }
            if (listener.isCancelled()) {
                return;
            }
            if (answer.isEmpty()) {
                throw new AgentModelException("Model returned an empty response");
            }
            String result = answer.toString();
            repository.append(id, new AgentMessage("assistant", result, Instant.now()));
            meters.counter("enjoytix.agent.model.calls", "status", "success").increment();
            meters.counter("enjoytix.agent.model.tokens", "type", "output").increment(estimateTokens(result));
            listener.onEvent(new AgentStreamEvent("answer.completed", result));
        } catch (RuntimeException ex) {
            meters.counter("enjoytix.agent.model.calls", "status", "failure").increment();
            if (!listener.isCancelled()) {
                listener.onEvent(new AgentStreamEvent("error", errorMessage(ex)));
            }
        } finally {
            meters.timer("enjoytix.agent.model.duration")
                    .record(System.nanoTime() - started, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    private long estimateTokens(String text) {
        return Math.max(1, (text.length() + 3) / 4);
    }

    private String errorMessage(RuntimeException ex) {
        return ex.getMessage() == null ? "Model generation failed" : ex.getMessage();
    }

    private AgentConversation requireConversation(Long id) {
        AgentConversation conversation = repository.find(id);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation does not exist");
        }
        return conversation;
    }

    private void checkOwner(AgentConversation conversation, Long userId) {
        if (!conversation.userId().equals(userId)) {
            throw new AgentAccessDeniedException();
        }
    }
}
