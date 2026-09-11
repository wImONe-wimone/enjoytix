package com.wimone.enjoytix.agent.service;

import com.wimone.enjoytix.agent.model.AgentConversation;
import com.wimone.enjoytix.agent.model.AgentMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.wimone.enjoytix.framework.distributedid.core.IdGeneratorManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AgentApplicationServiceTest {

    @Test
    void createsConversationAndStreamsAssistantResponse() {
        FakeRepository repository = new FakeRepository();
        AgentApplicationService service = new AgentApplicationService(repository,
                (conversationId, userId, content) -> "已收到：" + content,
                new IdGeneratorManager(new com.wimone.enjoytix.framework.distributedid.core.SnowflakeIdGenerator(10)),
                new SimpleMeterRegistry());

        AgentConversation conversation = service.createConversation(7L);
        var events = service.sendMessage(conversation.conversationId(), 7L,
                " 帮我查一下周末演出 ").toList();

        assertThat(events).extracting("type")
                .containsExactly("conversation.started", "token.delta", "answer.completed");
        assertThat(events.get(1).data()).isEqualTo("已收到：帮我查一下周末演出");
        assertThat(service.getConversation(conversation.conversationId(), 7L).messages())
                .extracting(AgentMessage::content)
                .containsExactly("帮我查一下周末演出", "已收到：帮我查一下周末演出");
    }

    @Test
    void rejectsConversationAccessForAnotherUser() {
        AgentApplicationService service = new AgentApplicationService(new FakeRepository(),
                (conversationId, userId, content) -> content,
                new IdGeneratorManager(new com.wimone.enjoytix.framework.distributedid.core.SnowflakeIdGenerator(10)),
                new SimpleMeterRegistry());
        AgentConversation conversation = service.createConversation(7L);

        assertThatThrownBy(() -> service.getConversation(conversation.conversationId(), 8L))
                .isInstanceOf(AgentAccessDeniedException.class);
    }

    @Test
    void stopsStreamingAndDoesNotSavePartialAnswerWhenListenerCancels() {
        FakeRepository repository = new FakeRepository();
        List<String> generatedChunks = new ArrayList<>();
        AgentResponseGenerator generator = new StreamingAgentResponseGenerator() {
            @Override
            public String generate(Long conversationId, Long userId, String content) {
                return "";
            }

            @Override
            public java.util.stream.Stream<String> generateStream(Long conversationId, Long userId, String content) {
                return java.util.stream.Stream.of("first", "second", "third").peek(generatedChunks::add);
            }
        };
        AgentApplicationService service = new AgentApplicationService(repository, generator,
                new IdGeneratorManager(new com.wimone.enjoytix.framework.distributedid.core.SnowflakeIdGenerator(10)),
                new SimpleMeterRegistry());
        AgentConversation conversation = service.createConversation(7L);
        List<String> eventTypes = new ArrayList<>();
        AtomicBoolean cancelled = new AtomicBoolean();

        service.sendMessage(conversation.conversationId(), 7L, "question", new AgentStreamListener() {
            @Override
            public void onEvent(com.wimone.enjoytix.agent.model.AgentStreamEvent event) {
                eventTypes.add(event.type());
                if ("token.delta".equals(event.type())) {
                    cancelled.set(true);
                }
            }

            @Override
            public boolean isCancelled() {
                return cancelled.get();
            }
        });

        assertThat(eventTypes).containsExactly("conversation.started", "token.delta");
        assertThat(generatedChunks).containsExactly("first");
        assertThat(service.getConversation(conversation.conversationId(), 7L).messages())
                .extracting(AgentMessage::content)
                .containsExactly("question");
    }
    private static final class FakeRepository implements ConversationRepository {
        private final Map<Long, AgentConversation> data = new ConcurrentHashMap<>();

        @Override
        public AgentConversation save(AgentConversation conversation) {
            data.put(conversation.conversationId(), conversation);
            return conversation;
        }

        @Override
        public AgentConversation find(Long conversationId) {
            return data.get(conversationId);
        }

        @Override
        public AgentConversation append(Long conversationId, AgentMessage message) {
            AgentConversation current = data.get(conversationId);
            List<AgentMessage> messages = new ArrayList<>(current.messages());
            messages.add(message);
            AgentConversation updated = new AgentConversation(current.conversationId(), current.userId(),
                    Instant.now(), List.copyOf(messages));
            data.put(conversationId, updated);
            return updated;
        }
    }
}
