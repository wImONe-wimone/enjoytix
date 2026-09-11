package com.wimone.enjoytix.agent.service;
import com.wimone.enjoytix.agent.model.AgentConversation;
import com.wimone.enjoytix.agent.model.AgentMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Repository
@Profile("!mysql")
public class InMemoryConversationRepository implements ConversationRepository {
    private final Map<Long, AgentConversation> conversations = new ConcurrentHashMap<>();

    @Override
    public AgentConversation save(AgentConversation conversation) {
        conversations.put(conversation.conversationId(), conversation);
        return conversation;
    }

    @Override
    public AgentConversation find(Long conversationId) {
        return conversations.get(conversationId);
    }

    @Override
    public AgentConversation append(Long conversationId, AgentMessage message) {
        return conversations.computeIfPresent(conversationId, (id, current) -> {
            List<AgentMessage> messages = new ArrayList<>(current.messages());
            messages.add(message);
            return new AgentConversation(current.conversationId(), current.userId(), current.createdAt(),
                    List.copyOf(messages));
        });
    }
}
