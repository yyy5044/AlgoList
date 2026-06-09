package com.algolist.backend.user.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileImageService {

	private static final Path PROFILE_IMAGE_DIR = Path.of("uploads", "profile-images"); // 백엔드가 파일을 저장하는 실제 경로
	private static final String PROFILE_IMAGE_URL_PREFIX = "/uploads/profile-images/"; // 프론트가 접근할 URL(WebConfig에 선언)
	private static final List<String> ALLOWED_PROFILE_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
	private static final int MAX_PROFILE_IMAGE_WIDTH = 1024;
	private static final int MAX_PROFILE_IMAGE_HEIGHT = 1024;

	// 받아온 이미지 파일을 저장하기 위한 메서드
	public String saveProfileImage(MultipartFile profileImage) {
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
