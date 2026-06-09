package com.algolist.backend.user.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 역할 변경을 위한 Dto
public class UpdateRoleRequestDto {
	private String role;
}
