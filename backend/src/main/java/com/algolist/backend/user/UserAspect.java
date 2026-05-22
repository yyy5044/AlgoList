package com.algolist.backend.user;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class UserAspect {

	private final PasswordEncoder passwordEncoder;

	@Around("execution(* com.algolist.backend.user.UserService.insertUser(String, String))"
			+ " || execution(* com.algolist.backend.user.UserService.updateUser(String, String))")
	public Object encodePassword(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();

		if (args.length > 1 && args[1] instanceof String password) {
			args[1] = passwordEncoder.encode(password);
		}

		return joinPoint.proceed(args);
	}
}
