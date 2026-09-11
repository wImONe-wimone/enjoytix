package com.wimone.enjoytix.agent.service;
import com.wimone.enjoytix.agent.model.AgentConversation;
import com.wimone.enjoytix.agent.model.AgentMessage;
public interface ConversationRepository { AgentConversation save(AgentConversation conversation); AgentConversation find(Long conversationId); AgentConversation append(Long conversationId, AgentMessage message); }
