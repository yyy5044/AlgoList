package com.algolist.backend.user;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {

	public List<UserDto> selectAllUsers();
	
	public UserDto selectUser(@Param("username") String username);
	
	public int insertUser(@Param("username") String username, @Param("password") String password);
	
	public int updateUser(@Param("username") String username, @Param("password") String password);
	
	public int deleteUser(@Param("username") String username);
}
