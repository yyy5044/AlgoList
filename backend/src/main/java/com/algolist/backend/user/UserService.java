package com.algolist.backend.user;

import java.util.List;

public interface UserService {
	
	public List<UserDto> selectAllUsers();
	
	public UserDto selectUser(String username);
	
	public boolean insertUser(String username, String password);
	
	public boolean updateUser(String username, String password);
	
	public boolean deleteUser(String username);
	
	public UserDto login(String username, String password);
}
