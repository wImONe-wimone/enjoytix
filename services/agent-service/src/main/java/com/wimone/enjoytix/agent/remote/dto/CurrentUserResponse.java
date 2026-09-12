package com.wimone.enjoytix.agent.remote.dto;
public record CurrentUserResponse(Long userId, String username, String mobile, String realName, Integer status) {}
