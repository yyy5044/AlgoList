package com.algolist.backend.user;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private static final Path PROFILE_IMAGE_DIR = Path.of("uploads", "profile-images"); // 백엔드가 파일을 저장하는 실제 경로
	private static final String PROFILE_IMAGE_URL_PREFIX = "/uploads/profile-images/"; // 프론트가 접근할 URL(WebConfig에 선언)
	private static final List<String> ALLOWED_PROFILE_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
	private static final int MAX_PROFILE_IMAGE_WIDTH = 1024;
	private static final int MAX_PROFILE_IMAGE_HEIGHT = 1024;

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

		String profileImageUrl = saveProfileImage(request.getProfileImage());
		
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
		String profileImageUrl = saveProfileImage(request.getProfileImage());
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

	// 비밀번호 로직이 적합한지 확인하는 메서드
	private void validatePassword(String password) {
		if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
			throw new IllegalArgumentException("비밀번호는 8자 이상이며 영어와 숫자를 모두 포함해야 합니다.");
		}
	}

	// 받아온 이미지 파일을 저장하기 위한 메서드
	private String saveProfileImage(MultipartFile profileImage) {
		if (profileImage == null || profileImage.isEmpty()) {
			return null;
		}

		try {
			validateProfileImage(profileImage);

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

	// 이미지 형식이 올바른지 확인하는 메서드(이미지 파일 형식, 이미지 파일 픽셀 크기), 올바르지 않다면 예외 반환
	private void validateProfileImage(MultipartFile profileImage) throws IOException {
		if (!ALLOWED_PROFILE_IMAGE_TYPES.contains(profileImage.getContentType())) {
			throw new IllegalArgumentException("프로필 이미지는 jpg, png, webp, gif 형식만 가능합니다.");
		}

		BufferedImage image = ImageIO.read(profileImage.getInputStream());
		if (image == null) {
			throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");
		}

		if (image.getWidth() > MAX_PROFILE_IMAGE_WIDTH || image.getHeight() > MAX_PROFILE_IMAGE_HEIGHT) {
			throw new IllegalArgumentException("프로필 이미지는 1024x1024 이하만 가능합니다.");
		}
	}
}
