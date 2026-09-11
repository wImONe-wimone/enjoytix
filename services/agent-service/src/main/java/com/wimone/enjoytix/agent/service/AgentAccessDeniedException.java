package com.wimone.enjoytix.agent.service;
public class AgentAccessDeniedException extends RuntimeException { public AgentAccessDeniedException() { super("Conversation does not belong to current user"); } }
