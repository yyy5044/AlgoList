package com.algolist.backend.user;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 수정 요청 시 담긴 데이터를 받기 위한 Dto
public class UpdateRequestDto {
	private String nickname;
	private String password;
	private String bio;
	private MultipartFile profileImage;
}
