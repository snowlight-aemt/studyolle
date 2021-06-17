package com.studyolle.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.studyolle.WithAccount;
import com.studyolle.domain.Account;
import com.studyolle.domain.Enrollment;
import com.studyolle.domain.Event;
import com.studyolle.domain.EventType;
import com.studyolle.domain.Study;
import com.studyolle.enrollment.EnrollmentRepository;
import com.studyolle.event.form.EventForm;
import com.studyolle.study.StudyControllerTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest extends StudyControllerTest {
    @Autowired private EventService eventService;
    @Autowired private ModelMapper modelMapper;
    @Autowired private EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("선착순 모임 신청하기 - 성공")
    @WithAccount("keesun")
    void createEventTest_success() throws Exception {
        Account manager = accountRepository.findByNickname("keesun");
        Study study = createStudy("study-test", manager);
        Event event = createRecruitingFCFSEvent(study, manager);
        
        Enrollment enrollmentManager = eventService.createEnrollment(event, manager);
        assertTrue(enrollmentManager.isAccepted());
    }

    @Test
    @DisplayName("선착순 모임 신청하기 - 실패")
    @WithAccount("keesun")
    void createEventTest_fail() throws Exception {
        Account manager = accountRepository.findByNickname("keesun");
        Study study = createStudy("study-test", manager);
        Event event = createRecruitingFCFSEvent(study, manager);
        event.setLimitOfEnrollments(0);
        
        Enrollment enrollmentManager = eventService.createEnrollment(event, manager);
        assertFalse(enrollmentManager.isAccepted());
    }

    // 1. 모임 참가 신청
    //  - FCFS 신청
    //   - 자리가 있는 경우, 확정자로 등록 (DOEN)
    //   - 자리가 없는 경우, 대기자로 등록 (DOEN)

    //  - Confirmation 신청
    //   - 대기 상태로 등록한다. (DOEN)
    @Test
    @DisplayName("FCFS 신청한다 - 자리가 있는 경우 확정자로 등록한다.")
    @WithAccount("keesun")
    void enroll_in_event_that_is_FCSF_if_space_is_empty() throws Exception {
        Account manager = accountRepository.findByNickname("keesun");
        Study study = createStudy("study-test", manager);
        Event event = createRecruitingFCFSEvent(study, manager);

        Account user1 = createAccount("user-1");
        
        Enrollment user1Enrollment = eventService.createEnrollment(event, user1);
        assertTrue(user1Enrollment.isAccepted());

        mockMvc.perform(post("/study/"+ study.getEncodedPath() +"/events/" + event.getId() + "/enroll")
            .with(csrf()))
            .andExpect(status().is3xxRedirection());

        Enrollment managerEnrollment = enrollmentRepository.findByEventAndAccount(event, manager);
        assertTrue(managerEnrollment.isAccepted());
    }

    @Test
    @DisplayName("FCFS 신청한다 - 자리가 없는 경우 대기자로 등록한다.")
    @WithAccount("keesun")
    void enroll_in_event_that_is_FCSF_if_space_is_not_empty() throws Exception {
        Account manager = accountRepository.findByNickname("keesun");
        Study study = createStudy("study-test", manager);
        Event event = createRecruitingFCFSEvent(study, manager);

        Account user1 = createAccount("user-1");
        Account user2 = createAccount("user-2");
        
        Enrollment user1Enrollment = eventService.createEnrollment(event, user1);
        assertTrue(user1Enrollment.isAccepted());
        Enrollment user2Enrollment = eventService.createEnrollment(event, user2);
        assertTrue(user2Enrollment.isAccepted());

        mockMvc.perform(post("/study/"+ study.getEncodedPath() +"/events/" + event.getId() + "/enroll")
            .with(csrf()))
            .andExpect(status().is3xxRedirection());

        Enrollment managerEnrollment = enrollmentRepository.findByEventAndAccount(event, manager);
        assertFalse(managerEnrollment.isAccepted());
    }

    @Test
    @DisplayName("Confirmation 신청한다 - 대기 상태로 등록한다")
    @WithAccount("keesun")
    void enroll_in_event_that_is_CONFIRMATION() throws Exception {
        Account manager = accountRepository.findByNickname("keesun");
        Study study = createStudy("study-test", manager);
        Event event = createRecruitingConfirmationEvent(study, manager);

        Account user1 = createAccount("user-1");
        Account user2 = createAccount("user-2");
        
        Enrollment user1Enrollment = eventService.createEnrollment(event, user1);
        assertFalse(user1Enrollment.isAccepted());
        Enrollment user2Enrollment = eventService.createEnrollment(event, user2);
        assertFalse(user2Enrollment.isAccepted());

        mockMvc.perform(post("/study/"+ study.getEncodedPath() +"/events/" + event.getId() + "/enroll")
            .with(csrf()))
            .andExpect(status().is3xxRedirection());

        Enrollment managerEnrollment = enrollmentRepository.findByEventAndAccount(event, manager);
        assertFalse(managerEnrollment.isAccepted());
    }

    // 2. 모임 참가 취소
    //  - FCFS 취소
    //   - 확정자가 취소시, 다음 대기자를 확정자로 변경한다. (DOEN)
    //   - 대기자가 취소시, 변화가 없음.

    //  - Confirmation 취소
    //   - 대기자가 취소시, 변화가 없음.

    @Test
    @DisplayName("FCFS 취소한다 - 확정자가 취소시, 다음 대기자를 확정자로 변경한다.")
    @WithAccount("keesun")
    void leave_in_event_that_is_FCFS_if_account_is_accepted() throws Exception {
        Account manager = accountRepository.findByNickname("keesun");
        Study study = createStudy("study-test", manager);
        Event event = createRecruitingFCFSEvent(study, manager);

        Account user1 = createAccount("user-1");
        Account user2 = createAccount("user-2");
        
        Enrollment managerEnrollment = eventService.createEnrollment(event, manager);
        assertTrue(managerEnrollment.isAccepted());
        Enrollment user1Enrollment = eventService.createEnrollment(event, user1);
        assertTrue(user1Enrollment.isAccepted());
        Enrollment user2Enrollment = eventService.createEnrollment(event, user2);
        assertFalse(user2Enrollment.isAccepted());

        mockMvc.perform(post("/study/"+ study.getEncodedPath() +"/events/" + event.getId() + "/leave")
            .with(csrf()))
            .andExpect(status().is3xxRedirection());
        
        assertTrue(user2Enrollment.isAccepted());
    }

    // 3. 모임 인원 수 추가시
    //  - FCFS 모임에 대해서 취소시 
    //   - 대기자를 추가된 인원 만큼 확정자로 상태를 변경한다.

    private Event createRecruitingFCFSEvent(Study study, Account manager) {
        EventForm eventForm = createEnrollment();
        Event event = eventService.createEvent(modelMapper.map(eventForm, Event.class), study, manager);

        return event;
    }

    private Event createRecruitingConfirmationEvent(Study study, Account manager) {
        EventForm eventForm = createEnrollment();
        eventForm.setEventType(EventType.CONFIRMATIVE);
        Event event = eventService.createEvent(modelMapper.map(eventForm, Event.class), study, manager);

        return event;
    }

    private EventForm createEnrollment() {
        EventForm eventForm = new EventForm();
        eventForm.setTitle("title");
        eventForm.setEventType(EventType.FCFS);
        eventForm.setDescription("description");
        eventForm.setLimitOfEnrollments(2);
        eventForm.setEndEnrollmentDateTime(LocalDateTime.now().plusHours(5));
        eventForm.setStartDateTime(LocalDateTime.now().plusDays(1));
        eventForm.setEndDateTime(LocalDateTime.now().plusDays(2));
        return eventForm;
    }
}
