package com.algolist.backend.problem.translation;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class TranslationServiceImpl implements TranslationService {
	private final ChatClient chatClient;
	
	public TranslationServiceImpl(ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}
}
