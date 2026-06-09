package com.algolist.backend.user;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {

	// 모든 유저 리스트 반환
	public List<UserDto> selectAllUsers();

	// 조건에 맞는 유저 리스트 반환
	public List<UserDto> selectUsers(@Param("accountStatus") String accountStatus,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword,
			@Param("size") int size,
			@Param("offset") int offset);

	// 조건에 맞는 유저 수 반환
	public long countUsers(@Param("accountStatus") String accountStatus,
			@Param("searchType") String searchType,
			@Param("keyword") String keyword);
	
	// username으로 특정 유저 찾기(비밀번호 미포함)
	public UserDetailDto selectUser(@Param("username") String username);
	
	// username으로 로그인을 위해 특정 유저 찾기(비밀번호 포함)
	public UserDto selectUserForAuth(@Param("username") String username);
	
	// username, password, nickname, profileImageUrl로 유저 추가
	public int insertUser(@Param("username") String username,
			@Param("password") String password,
			@Param("nickname") String nickname,
			@Param("profileImageUrl") String profileImageUrl);
	
	// 유저 수정
	public int updateUser(@Param("username") String username,
			@Param("nickname") String nickname,
			@Param("password") String password,
			@Param("bio") String bio,
			@Param("profileImageUrl") String profileImageUrl);
	
	// 유저 삭제(Soft Delete)
	public int deleteUser(@Param("username") String username);

	// 유저 정지
	public int suspendUser(@Param("userId") Long userId);

	// 유저 정지 이력 추가
	public int insertUserSuspension(@Param("userId") Long userId,
			@Param("reason") String reason,
			@Param("suspendedUntil") LocalDateTime suspendedUntil,
			@Param("suspendedBy") Long suspendedBy);

	// 유저 정지 해제
	public int releaseUserSuspension(@Param("userId") Long userId);

	// 유저 정지 이력에 해제 정보 추가
	public int updateUserSuspensionRelease(@Param("userId") Long userId,
			@Param("releasedBy") Long releasedBy,
			@Param("releaseReason") String releaseReason);

	// 정지 기간이 만료된 정지 이력 해제
	public int releaseExpiredSuspensionHistories();

	// 활성 정지 이력이 없는 정지 유저 활성화
	public int activateUsersWithoutActiveSuspension();
}
