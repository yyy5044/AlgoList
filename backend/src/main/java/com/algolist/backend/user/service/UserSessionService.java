package com.algolist.backend.user.service;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import com.algolist.backend.auth.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSessionService {

	private final SessionRegistry sessionRegistry;

	// 특정 유저의 기존 로그인 세션을 만료 처리하는 메서드
	public void expireUserSessions(Long userId) {
		for (Object principal : sessionRegistry.getAllPrincipals()) {
			if (!(principal instanceof CustomUserDetails userDetails)) {
				continue;
			}

			if (!userId.equals(userDetails.getUser().getUserId())) {
				continue;
			}

			for (SessionInformation session : sessionRegistry.getAllSessions(userDetails, false)) {
				session.expireNow();
			}
		}
	}
}
