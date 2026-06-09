package com.algolist.backend.user.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPageResponseDto {
	private List<UserDto> users;
	private int page;
	private int size;
	private long totalCount;
	private int totalPages;
}
