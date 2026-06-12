package com.algolist.backend.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReminderServiceImplTest {

	@Test
	void updateGradeNormalizesAllowedGrade() {
		ReminderDao reminderDao = mock(ReminderDao.class);
		ReminderServiceImpl service = new ReminderServiceImpl(reminderDao);
		UpdateGradeRequestDto request = request(" yellow ");
		ReminderProblemDto updatedProblem = reminderProblem("YELLOW", LocalDate.now().minusDays(7));

		when(reminderDao.updateGrade(eq(1L), eq(10L), any(String.class))).thenReturn(1);
		when(reminderDao.selectOne(1L, 10L)).thenReturn(updatedProblem);

		ReminderProblemDto result = service.updateGrade(1L, 10L, request);

		ArgumentCaptor<String> gradeCaptor = ArgumentCaptor.forClass(String.class);
		verify(reminderDao).updateGrade(eq(1L), eq(10L), gradeCaptor.capture());
		assertThat(gradeCaptor.getValue()).isEqualTo("YELLOW");
		assertThat(result.getReviewDueDate()).isEqualTo(LocalDate.now());
	}

	@Test
	void updateGradeAllowsGreenAndReturnsNoReviewDueDate() {
		ReminderDao reminderDao = mock(ReminderDao.class);
		ReminderServiceImpl service = new ReminderServiceImpl(reminderDao);
		UpdateGradeRequestDto request = request("GREEN");
		ReminderProblemDto updatedProblem = reminderProblem("GREEN", LocalDate.now());

		when(reminderDao.updateGrade(1L, 10L, "GREEN")).thenReturn(1);
		when(reminderDao.selectOne(1L, 10L)).thenReturn(updatedProblem);

		ReminderProblemDto result = service.updateGrade(1L, 10L, request);

		assertThat(result.getGrade()).isEqualTo("GREEN");
		assertThat(result.getReviewDueDate()).isNull();
	}

	@Test
	void updateGradeRejectsUnsupportedGrade() {
		ReminderDao reminderDao = mock(ReminderDao.class);
		ReminderServiceImpl service = new ReminderServiceImpl(reminderDao);

		assertThatThrownBy(() -> service.updateGrade(1L, 10L, request("BLUE")))
				.isInstanceOf(IllegalArgumentException.class);

		verify(reminderDao, never()).updateGrade(any(), any(), any());
	}

	@Test
	void updateGradeRejectsBlankGrade() {
		ReminderDao reminderDao = mock(ReminderDao.class);
		ReminderServiceImpl service = new ReminderServiceImpl(reminderDao);

		assertThatThrownBy(() -> service.updateGrade(1L, 10L, request(" ")))
				.isInstanceOf(IllegalArgumentException.class);

		verify(reminderDao, never()).updateGrade(any(), any(), any());
	}

	@Test
	void updateGradeReturnsNullWhenUserProblemIsNotOwned() {
		ReminderDao reminderDao = mock(ReminderDao.class);
		ReminderServiceImpl service = new ReminderServiceImpl(reminderDao);

		when(reminderDao.updateGrade(1L, 10L, "RED")).thenReturn(0);

		ReminderProblemDto result = service.updateGrade(1L, 10L, request("RED"));

		assertThat(result).isNull();
		verify(reminderDao, never()).selectOne(any(), any());
	}

	private UpdateGradeRequestDto request(String grade) {
		UpdateGradeRequestDto request = new UpdateGradeRequestDto();
		request.setGrade(grade);
		return request;
	}

	private ReminderProblemDto reminderProblem(String grade, LocalDate lastSolvedDate) {
		ReminderProblemDto reminderProblem = new ReminderProblemDto();
		reminderProblem.setUserProblemId(10L);
		reminderProblem.setGrade(grade);
		reminderProblem.setLastSolvedDate(lastSolvedDate);
		return reminderProblem;
	}
}
