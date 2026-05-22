package com.algolist.backend.user;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserDao userDao;

	@Override
	public List<UserDto> selectAllUsers() {
		return userDao.selectAllUsers();
	}

	@Override
	public UserDto selectUser(String username) {
		return userDao.selectUser(username);
	}

	@Override
	public boolean insertUser(String username, String password) {
		int result = userDao.insertUser(username, password);

		if (result != 1) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean updateUser(String username, String password) {
		int result = userDao.updateUser(username, password);

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
	public UserDto login(String username, String password) {
		UserDto user = userDao.selectUser(username);

		if (user == null) {
			return null;
		}

		if (!password.equals(user.getPassword())) {
			return null;
		}

		return user;
	}

}
