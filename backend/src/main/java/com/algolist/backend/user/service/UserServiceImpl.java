package com.algolist.backend.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.algolist.backend.user.dao.UserDao;
import com.algolist.backend.user.dto.request.CreateRequestDto;
import com.algolist.backend.user.dto.request.UpdateRequestDto;
import com.algolist.backend.user.dto.response.UserDetailDto;
import com.algolist.backend.user.dto.response.UserSuspensionInfoDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder;
	private final ProfileImageService profileImageService;

	@Override
	public UserDetailDto selectUser(String username) {
		return userDao.selectUser(username);
	}

	@Override
	public boolean insertUser(CreateRequestDto request) {
		String username = StringUtils.hasText(request.getUsername()) ? request.getUsername().trim() : null;
		String password = request.getPassword();
		String nickname = StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : null;

		// username이나 password는 필수 값이므로 하나라도 없을 시 예외 반환
		if (username == null || !StringUtils.hasText(password)) {
			throw new IllegalArgumentException("아이디와 비밀번호를 모두 입력해주세요.");
		}

		password = password.trim();
		validatePassword(password);
		password = passwordEncoder.encode(password);

		UserDetailDto user = userDao.selectUser(username);
		
		// 중복된 ID를 사용하고 있는지 확인, 중복이라면 예외 반환
		if(user != null) {
			throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
		}

		String profileImageUrl = profileImageService.saveProfileImage(request.getProfileImage());
		
		// 유저 생성 시 1개의 행이 생성되므로 result가 1이여야만 정상 동작으로 간주
		int result = userDao.insertUser(username, password, nickname, profileImageUrl);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	// 유저 정보 업데이트(프로필 이미지, 닉네임, 비밀번호, 자기소개)
	public boolean updateUser(String username, UpdateRequestDto request) {
		String profileImageUrl = profileImageService.saveProfileImage(request.getProfileImage());
		String nickname = StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : null;
		String password = request.getPassword();
		String bio = request.getBio() == null ? null : request.getBio().trim();

		if (StringUtils.hasText(password)) {
			password = password.trim();
			validatePassword(password);
			password = passwordEncoder.encode(password);
		} else {
			password = null;
		}

		if (nickname == null && password == null && bio == null && profileImageUrl == null) {
			return userDao.selectUser(username) != null;
		}

		int result = userDao.updateUser(username, nickname, password, bio, profileImageUrl);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean deleteUser(String username) {
		int result = userDao.deleteUser(username);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public UserSuspensionInfoDto selectActiveSuspension(Long userId) {
		return userDao.selectActiveSuspension(userId);
	}

	// 비밀번호 로직이 적합한지 확인하는 메서드
	private void validatePassword(String password) {
		if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
			throw new IllegalArgumentException("비밀번호는 8자 이상이며 영어와 숫자를 모두 포함해야 합니다.");
		}
	}
}
