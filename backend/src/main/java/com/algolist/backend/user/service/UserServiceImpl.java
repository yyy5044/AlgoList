package com.algolist.backend.user.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algolist.backend.global.exception.DuplicateUsernameException;
import com.algolist.backend.solution.SolutionActivityService;
import com.algolist.backend.user.dao.UserDao;
import com.algolist.backend.user.dto.UserDto;
import com.algolist.backend.user.dto.request.CreateRequestDto;
import com.algolist.backend.user.dto.request.UpdateRequestDto;
import com.algolist.backend.user.dto.response.SolutionActivityResponseDto;
import com.algolist.backend.user.dto.response.UserDetailDto;
import com.algolist.backend.user.dto.response.UserSuspensionInfoDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private static final int MAX_USERNAME_LENGTH = 50;
	private static final int MAX_NICKNAME_LENGTH = 50;
	private static final int MAX_BIO_LENGTH = 500;
	private static final int MAX_PASSWORD_LENGTH = 72;

	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder;
	private final ProfileImageService profileImageService;
	private final EmailVerificationService emailVerificationService;
	private final SolutionActivityService solutionActivityService;
	private final UserSessionService userSessionService;

	@Override
	public UserDetailDto selectUser(String username) {
		return userDao.selectUser(username);
	}

	@Override
	// 회원가입
	@Transactional
	public boolean insertUser(CreateRequestDto request) {
		String username = StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : null;
		String password = request.getPassword();
		String nickname = StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : null;
		String email = request.getEmail();

		// username이나 password는 필수 값이므로 하나라도 없을 시 예외 반환
		if (username == null || !StringUtils.hasText(password)) {
			throw new IllegalArgumentException("아이디와 비밀번호를 모두 입력해주세요.");
		}

		validateTextLength(username, MAX_USERNAME_LENGTH, "아이디");
		validateTextLength(nickname, MAX_NICKNAME_LENGTH, "닉네임");

		password = password.trim();
		validatePassword(password);
		password = passwordEncoder.encode(password);

		UserDetailDto user = userDao.selectUser(username);
		
		// 중복된 ID를 사용하고 있는지 확인, 중복이라면 예외 반환
		if(user != null) {
			throw new DuplicateUsernameException("이미 사용 중인 아이디입니다.");
		}

		String normalizedEmail = emailVerificationService.normalizeEmail(email);
		// 이메일 인증 여부 확인(컬럼에 넣을 값)
		LocalDateTime emailVerifiedAt = emailVerificationService.validateSignupEmail(normalizedEmail);
		String profileImageUrl = profileImageService.saveProfileImage(request.getProfileImage());
		
		// 유저 생성 시 1개의 행이 생성되므로 result가 1이여야만 정상 동작으로 간주
		try {
			int result = userDao.insertUser(username, normalizedEmail, emailVerifiedAt, password, nickname, profileImageUrl);
			if (result != 1) {
				profileImageService.deleteProfileImage(profileImageUrl);
				return false;
			}
			// 인증한 이메일 데이터는 consumed_at 컬럼 설정
			emailVerificationService.consumeVerifiedEmail(normalizedEmail);
			return true;
		} catch (RuntimeException e) {
			profileImageService.deleteProfileImage(profileImageUrl);
			throw e;
		}
	}

	@Override
	// 유저 정보 업데이트(프로필 이미지, 닉네임, 비밀번호, 자기소개)
	public boolean updateUser(String username, UpdateRequestDto request) {
		String nickname = StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : null;
		String password = request.getPassword();
		String bio = request.getBio() == null ? null : request.getBio().trim();
		boolean hasProfileImage = request.getProfileImage() != null && !request.getProfileImage().isEmpty();

		validateTextLength(nickname, MAX_NICKNAME_LENGTH, "닉네임");
		validateTextLength(bio, MAX_BIO_LENGTH, "자기소개");

		if (StringUtils.hasText(password)) {
			password = password.trim();
			validatePassword(password);
			password = passwordEncoder.encode(password);
		} else {
			password = null;
		}

		if (nickname == null && password == null && bio == null && !hasProfileImage) {
			return userDao.selectUser(username) != null;
		}

		UserDetailDto currentUser = null;
		// 이미지 파일이 존재할 경우 기존 유저의 이미지 파일 경로를 가져오기 위해 currentUser 객체 가져오기
		if (hasProfileImage) {
			currentUser = userDao.selectUser(username);
			if (currentUser == null) {
				return false;
			}
		}

		String profileImageUrl = profileImageService.saveProfileImage(request.getProfileImage());
		try {
			int result = userDao.updateUser(username, nickname, password, bio, profileImageUrl);

			// 정상적인 유저 UPDATE에 실패하면 새로 저장한 프로필 이미지 지우기
			if (result != 1) {
				profileImageService.deleteProfileImage(profileImageUrl);
				return false;
			}

			// 업데이트에 성공했다면 기존 유저의 프로필 이미지 지우기
			if (currentUser != null) {
				profileImageService.deleteProfileImage(currentUser.getProfileImageUrl());
			}

			return true;
			
		// 예외 발생 시에도 추가한 프로필 이미지 삭제
		} catch (RuntimeException e) {
			profileImageService.deleteProfileImage(profileImageUrl);
			throw e;
		}
	}

	@Override
	// 유저 삭제(Soft Delete)
	public boolean deleteUser(String username) {
		UserDto user = userDao.selectUserForAuth(username);
		if (user == null) {
			return false;
		}

		int result = userDao.deleteUser(username);

		if (result != 1) {
			return false;
		} else {
			userSessionService.expireUserSessions(user.getUserId());
			return true;
		}
	}

	@Override
	public UserSuspensionInfoDto selectActiveSuspension(Long userId) {
		return userDao.selectActiveSuspension(userId);
	}

	@Override
	public SolutionActivityResponseDto selectSolutionActivity(String username) {
		UserDto user = userDao.selectUserForAuth(username);
		if (user == null) {
			return null;
		}

		return solutionActivityService.selectActivity(user.getUserId());
	}
	
	// 아이디, 닉네임, 비밀번호, 자기소개 등이 허용된 길이 이내인지 검증하는 메서드
	private void validateTextLength(String value, int maxLength, String fieldName) {
		if (value != null && value.codePointCount(0, value.length()) > maxLength) {
			throw new IllegalArgumentException(fieldName + "은(는) 최대 " + maxLength + "자까지 입력할 수 있습니다.");
		}
	}

	// 비밀번호 로직이 적합한지 확인하는 메서드
	private void validatePassword(String password) {
		int passwordLength = password.codePointCount(0, password.length());
		if (passwordLength < 8 || passwordLength > MAX_PASSWORD_LENGTH || !password.matches(".*[A-Za-z].*")
				|| !password.matches(".*\\d.*")) {
			throw new IllegalArgumentException("비밀번호는 8자 이상 72자 이하이며 영어와 숫자를 모두 포함해야 합니다.");
		}
	}
}
