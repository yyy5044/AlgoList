package com.algolist.backend.user.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algolist.backend.global.exception.DuplicateEmailException;
import com.algolist.backend.user.dao.EmailVerificationDao;
import com.algolist.backend.user.dao.UserDao;
import com.algolist.backend.user.dto.EmailVerificationDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	private static final Duration CODE_TTL = Duration.ofMinutes(10);
	private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
	private static final Duration FAILED_ATTEMPT_BLOCK_DURATION = Duration.ofMinutes(10);
	private static final int MAX_FAILED_ATTEMPTS = 5;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final EmailVerificationDao emailVerificationDao;
	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder;
	private final JavaMailSender mailSender;
	private final EmailVerificationAttemptService emailVerificationAttemptService;

	@Value("${spring.mail.username:}")
	private String mailFrom;

	@Override
	@Transactional
	// 인증 코드 보내기 메서드
	public void sendVerificationCode(String email) {
		String normalizedEmail = normalizeEmail(email);
		// 이미 user 테이블에 존재하는 이메일인지 확인(중복 방지), 인증 코드 보내기 전에 처리
		if (userDao.countByEmail(normalizedEmail) > 0) {
			throw new DuplicateEmailException("이미 가입된 이메일입니다.");
		}

		LocalDateTime now = LocalDateTime.now();
		// 이전 인증 요청 기록 가져오기
		EmailVerificationDto verification = emailVerificationDao.selectByEmail(normalizedEmail);
		// 잦은 인증 실패로 일시정지된 상태라면 예외 반환
		if (verification != null && verification.getConsumedAt() == null && isBlocked(verification, now)) {
			throw new IllegalArgumentException("인증 실패 횟수를 초과했습니다. 10분 후 다시 요청해주세요.");
		}
		// 이전에 이미 인증 시도를 했고 RESEND_COOLDOWN이 지나지 않은 상황이라면 예외 반환
		if (verification != null && verification.getConsumedAt() == null && verification.getUpdatedAt() != null
				&& verification.getUpdatedAt().isAfter(now.minus(RESEND_COOLDOWN))) {
			throw new IllegalArgumentException("인증 코드는 60초 후에 다시 요청할 수 있습니다.");
		}

		String code = generateCode();
		// 만든 코드를 BCrypt 방식으로 암호화하여 저장
		String codeHash = passwordEncoder.encode(code);
		LocalDateTime expiresAt = now.plus(CODE_TTL);
		// 이전에 인증 요청을 한 적 없으면 insert로 email_verifications 데이터 생성, 이미 있으면 갱신
		if (verification == null) {
			emailVerificationDao.insertVerification(normalizedEmail, codeHash, expiresAt);
		} else {
			emailVerificationDao.refreshVerification(normalizedEmail, codeHash, expiresAt);
		}

		sendMail(normalizedEmail, code);
	}

	@Override
	@Transactional
	// 인증 코드 확인 메서드
	public void confirmVerificationCode(String email, String code) {
		String normalizedEmail = normalizeEmail(email);
		String verificationCode = StringUtils.hasText(code) ? code.trim() : "";
		// 인증 코드가 0~9로 이루어진 6자리 문자열인지 확인, 아니면 예외 반환
		if (!verificationCode.matches("\\d{6}")) {
			throw new IllegalArgumentException("인증 코드는 6자리 숫자여야 합니다.");
		}
		
		// 인증 코드가 유효한지 확인
		EmailVerificationDto verification = getActiveVerification(normalizedEmail);
		LocalDateTime now = LocalDateTime.now();
		if (isBlocked(verification, now)) {
			throw new IllegalArgumentException("인증 실패 횟수를 초과했습니다. 10분 후 다시 시도해주세요.");
		}
		// 실패 횟수를 초과한 인증 코드는 더 이상 사용할 수 없으므로 새 인증 코드를 요청하도록 예외 반환
		if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
			throw new IllegalArgumentException("인증 실패 횟수를 초과했습니다. 새 인증 코드를 요청해주세요.");
		}

		// 인증 실패 시 failed_attempts 값 증가
		if (!passwordEncoder.matches(verificationCode, verification.getCodeHash())) {
			boolean willBeBlocked = verification.getFailedAttempts() + 1 >= MAX_FAILED_ATTEMPTS;
			LocalDateTime blockedUntil = now.plus(FAILED_ATTEMPT_BLOCK_DURATION);
			// 실패 횟수를 초과하면 FAILED_ATTEMPT_BLOCK_DURATION만큼 정지
			emailVerificationAttemptService.recordFailedAttempt(normalizedEmail, MAX_FAILED_ATTEMPTS, blockedUntil);
			if (willBeBlocked) {
				throw new IllegalArgumentException("인증 실패 횟수를 초과했습니다. 10분 후 다시 요청해주세요.");
			}
			throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
		}

		// 인증 성공 시 현재 email_verifications 데이터의 verified_at을 현재 시간으로 변경
		int result = emailVerificationDao.markVerified(normalizedEmail, now);
		if (result != 1) {
			throw new IllegalArgumentException("이메일 인증이 더 이상 유효하지 않습니다.");
		}
	}

	@Override
	// 입력한 이메일이 올바른 이메일 데이터인지 확인하는 메서드
	public String normalizeEmail(String email) {
		// 아무것도 작성하지 않았을 시 예외 반환
		if (!StringUtils.hasText(email)) {
			throw new IllegalArgumentException("이메일을 입력해주세요.");
		}

		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		// 이메일의 길이가 255자 이상이나 abc123...@abc123.abc 처럼 이메일 포맷이 맞지 않으면 예외 반환
		if (normalizedEmail.length() > 255 || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
			throw new IllegalArgumentException("이메일 형식이 올바르지 않습니다.");
		}
		return normalizedEmail;
	}

	@Override
	// 회원가입 전 이메일 인증 여부를 한번 더 확인(UserServiceImpl에서 사용)
	public LocalDateTime validateSignupEmail(String email) {
		String normalizedEmail = normalizeEmail(email);
		if (userDao.countByEmail(normalizedEmail) > 0) {
			throw new DuplicateEmailException("이미 가입된 이메일입니다.");
		}

		EmailVerificationDto verification = emailVerificationDao.selectByEmail(normalizedEmail);
		// 인증 데이터가 없거나, 인증되지 않았거나, 이미 사용된 이메일이거나, 만료 시간이 존재하지 않거나, 이미 만료된 인증 데이터라면 예외 반환
		if (verification == null || verification.getVerifiedAt() == null || verification.getConsumedAt() != null
				|| verification.getExpiresAt() == null || !verification.getExpiresAt().isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("회원가입 전에 이메일 인증을 완료해주세요.");
		}

		return verification.getVerifiedAt();
	}

	@Override
	// 회원가입 시 인증 데이터의 consumed_at 설정(UserServiceImpl에서 사용)
	public void consumeVerifiedEmail(String email) {
		String normalizedEmail = normalizeEmail(email);
		int result = emailVerificationDao.consumeVerifiedEmail(normalizedEmail);
		if (result != 1) {
			throw new IllegalArgumentException("이메일 인증이 더 이상 유효하지 않습니다.");
		}
	}

	@Override
	// expires_at이나 consumed_at이 threshold보다 오래됐다면 삭제
	public int deleteExpiredOrConsumedBefore(LocalDateTime threshold) {
		return emailVerificationDao.deleteExpiredOrConsumedBefore(threshold);
	}

	// 인증 실패 초과로 요청 정지여부 확인
	private boolean isBlocked(EmailVerificationDto verification, LocalDateTime now) {
		return verification.getBlockedUntil() != null && verification.getBlockedUntil().isAfter(now);
	}

	// 인증 코드를 확인할 때 만료/사용 완료된 인증인지 확인하는 메서드
	private EmailVerificationDto getActiveVerification(String email) {
		EmailVerificationDto verification = emailVerificationDao.selectByEmail(email);
		if (verification == null || verification.getConsumedAt() != null) {
			throw new IllegalArgumentException("이메일 인증 코드를 먼저 요청해주세요.");
		}
		if (verification.getExpiresAt() == null || !verification.getExpiresAt().isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("인증 코드가 만료되었습니다. 새 인증 코드를 요청해주세요.");
		}
		return verification;
	}

	// 무작위 인증 코드 발생 메서드(000000~999999)
	private String generateCode() {
		return String.format("%06d", RANDOM.nextInt(1_000_000));
	}

	// 이메일 보내기
	private void sendMail(String email, String code) {
		SimpleMailMessage message = new SimpleMailMessage();
		if (StringUtils.hasText(mailFrom)) {
			message.setFrom(mailFrom);
		}
		message.setTo(email);
		message.setSubject("[AlgoList] 이메일 인증 코드");
		message.setText("AlgoList 이메일 인증 코드는 " + code + "입니다. 이 코드는 10분 후 만료됩니다.");
		mailSender.send(message);
	}
}
