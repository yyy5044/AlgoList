package com.algolist.backend.user.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.algolist.backend.user.dao.UserDao;
import com.algolist.backend.user.dto.CreateRequestDto;
import com.algolist.backend.user.dto.ReleaseSuspensionRequestDto;
import com.algolist.backend.user.dto.SuspendUserRequestDto;
import com.algolist.backend.user.dto.UpdateRoleRequestDto;
import com.algolist.backend.user.dto.UpdateRequestDto;
import com.algolist.backend.user.dto.UserDetailDto;
import com.algolist.backend.user.dto.UserDto;
import com.algolist.backend.user.dto.UserPageResponseDto;
import com.algolist.backend.user.dto.UserSuspensionInfoDto;

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
	// 페이징 처리를 위한 selectUsers 비즈니스 로직
	public UserPageResponseDto selectUsers(int page, int size, String status, String searchType, String keyword) {
		page = Math.max(page, 1); // 현재 페이지(최소는 1)
		size = Math.min(Math.max(size, 1), 100); // 페이지마다 표시할 회원 수 (최소 1, 최대 100)
		int offset = (page - 1) * size; // 몇 번째 회원부터 표시해야 할지 결정(page = 2, size = 10이라면 11번째 유저부터 보여주기)
		String accountStatus = StringUtils.hasText(status) && !"ALL".equals(status) ? status.trim() : null; // 조건이 없으면 "ALL"
		String searchTarget = "nickname".equals(searchType) ? "nickname" : "username"; // nickname과 username 중 무엇으로 보여줄 지 설정
		String searchKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null; 

		long totalCount = userDao.countUsers(accountStatus, searchTarget, searchKeyword); // 총 몇 명의 유저가 조회되었는지 확인
		List<UserDto> users = userDao.selectUsers(accountStatus, searchTarget, searchKeyword, size, offset); // 현재 페이지에서의 유저 목록 확인

		UserPageResponseDto response = new UserPageResponseDto(); // 객체에 값 할당
		response.setUsers(users);
		response.setPage(page);
		response.setSize(size);
		response.setTotalCount(totalCount);
		response.setTotalPages((int) Math.ceil((double) totalCount / size));
		return response;
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

	@Override
	@Transactional
	// 유저 정지, 테이블 2개를 수정해야 하므로 트랜잭션으로 구현
	public boolean suspendUser(String username, SuspendUserRequestDto request, Long adminId) {
		if (request == null || request.getSuspendedUntil() == null) {
			throw new IllegalArgumentException("정지 종료일을 입력해주세요.");
		}

		if (request.getSuspendedUntil().isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("정지 종료일은 오늘 또는 이후 날짜여야 합니다.");
		}

		UserDto user = userDao.selectUserForAuth(username);
		if (user == null) {
			return false;
		}

		if ("ADMIN".equals(user.getRole())) {
			throw new IllegalArgumentException("관리자 계정은 정지할 수 없습니다.");
		}

		if ("DELETED".equals(user.getAccountStatus())) {
			throw new IllegalArgumentException("삭제된 계정은 정지할 수 없습니다.");
		}

		if ("SUSPENDED".equals(user.getAccountStatus())) {
			throw new IllegalArgumentException("이미 정지된 계정입니다.");
		}

		String reason = StringUtils.hasText(request.getReason()) ? request.getReason().trim() : null;
		LocalDateTime suspendedUntil = request.getSuspendedUntil().plusDays(1).atStartOfDay();
		int updateResult = userDao.suspendUser(user.getUserId());
		if (updateResult != 1) {
			return false;
		}

		int insertResult = userDao.insertUserSuspension(user.getUserId(), reason, suspendedUntil, adminId);
		if (insertResult != 1) {
			throw new IllegalStateException("정지 이력을 저장하지 못했습니다.");
		}

		return true;
	}

	@Override
	@Transactional
	// 유저 정지 해제, 계정 상태와 정지 이력을 함께 수정해야 하므로 트랜잭션으로 구현
	public boolean releaseUserSuspension(String username, ReleaseSuspensionRequestDto request, Long adminId) {
		UserDto user = userDao.selectUserForAuth(username);
		if (user == null) {
			return false;
		}

		if ("DELETED".equals(user.getAccountStatus())) {
			throw new IllegalArgumentException("삭제된 계정은 정지 해제할 수 없습니다.");
		}

		if (!"SUSPENDED".equals(user.getAccountStatus())) {
			throw new IllegalArgumentException("정지된 계정만 해제할 수 있습니다.");
		}

		String releaseReason = request != null && StringUtils.hasText(request.getReleaseReason())
				? request.getReleaseReason().trim()
				: null;
		int updateResult = userDao.releaseUserSuspension(user.getUserId());
		if (updateResult != 1) {
			return false;
		}

		int releaseResult = userDao.updateUserSuspensionRelease(user.getUserId(), adminId, releaseReason);
		if (releaseResult < 1) {
			throw new IllegalArgumentException("해제할 정지 이력을 찾을 수 없습니다.");
		}

		return true;
	}

	@Override
	public boolean updateUserRole(String username, UpdateRoleRequestDto request, Long adminId) {
		if (request == null || !StringUtils.hasText(request.getRole())) {
			throw new IllegalArgumentException("변경할 권한을 선택해주세요.");
		}

		String role = request.getRole().trim().toUpperCase();
		if (!"USER".equals(role) && !"ADMIN".equals(role)) {
			throw new IllegalArgumentException("권한은 USER 또는 ADMIN만 가능합니다.");
		}

		UserDto user = userDao.selectUserForAuth(username);
		if (user == null) {
			return false;
		}

		if (adminId != null && adminId.equals(user.getUserId())) {
			throw new IllegalArgumentException("자기 자신의 권한은 변경할 수 없습니다.");
		}

		if ("DELETED".equals(user.getAccountStatus())) {
			throw new IllegalArgumentException("삭제된 계정의 권한은 변경할 수 없습니다.");
		}

		if (role.equals(user.getRole())) {
			return true;
		}

		int result = userDao.updateUserRole(user.getUserId(), role);
		return result == 1;
	}

	@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
	@Transactional
	// 매일 00:05에 정지 종료일이 지난 유저를 자동 해제
	public void releaseExpiredSuspensions() {
		int releaseCount = userDao.releaseExpiredSuspensionHistories();
		if (releaseCount > 0) {
			userDao.activateUsersWithoutActiveSuspension();
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
