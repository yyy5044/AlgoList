package com.algolist.backend.user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private static final Path PROFILE_IMAGE_DIR = Path.of("uploads", "profile-images");
	private static final String PROFILE_IMAGE_URL_PREFIX = "/uploads/profile-images/";

	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder;

	@Override
	public List<UserDto> selectAllUsers() {
		return userDao.selectAllUsers();
	}

	@Override
	public UserDetailDto selectUser(String username) {
		return userDao.selectUser(username);
	}

	@Override
	public boolean insertUser(String username, String password) {
		UserDetailDto user = userDao.selectUser(username);
		
		// 중복된 ID를 사용하고 있는지 확인
		if(user != null) {
			return false;
		}
		
		// 유저 생성 시 1개의 행이 생성되므로 result가 1이여야만 정상 동작으로 간주
		int result = userDao.insertUser(username, password);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean updateUser(String username, UpdateRequestDto request) {
		String profileImageUrl = saveProfileImage(request.getProfileImage());
		String password = request.getPassword();

		if (StringUtils.hasText(password)) {
			password = passwordEncoder.encode(password);
		} else {
			password = null;
		}

		if (request.getNickname() == null && password == null && request.getBio() == null && profileImageUrl == null) {
			return userDao.selectUser(username) != null;
		}

		int result = userDao.updateUser(username, request.getNickname(), password, request.getBio(), profileImageUrl);

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

	private String saveProfileImage(MultipartFile profileImage) {
		if (profileImage == null || profileImage.isEmpty()) {
			return null;
		}

		try {
			Files.createDirectories(PROFILE_IMAGE_DIR);
			String extension = StringUtils.getFilenameExtension(profileImage.getOriginalFilename());
			String fileName = UUID.randomUUID().toString();
			if (StringUtils.hasText(extension)) {
				fileName += "." + extension;
			}

			profileImage.transferTo(PROFILE_IMAGE_DIR.resolve(fileName));
			return PROFILE_IMAGE_URL_PREFIX + fileName;
		} catch (IOException e) {
			throw new IllegalStateException("프로필 이미지를 저장하지 못했습니다.", e);
		}
	}
}
