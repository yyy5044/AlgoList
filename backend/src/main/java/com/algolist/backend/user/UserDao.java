package com.algolist.backend.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {

	// 모든 유저 리스트 반환
	public List<UserDto> selectAllUsers();
	
	// username으로 특정 유저 찾기
	public UserDto selectUser(@Param("username") String username);
	
	// username, password로 유저 추가
	public int insertUser(@Param("username") String username, @Param("password") String password);
	
	// 유저 수정(지금은 password만)
	public int updateUser(@Param("username") String username, @Param("password") String password);
	
	// 유저 삭제
	public int deleteUser(@Param("username") String username);
}
