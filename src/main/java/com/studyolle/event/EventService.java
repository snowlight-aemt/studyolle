package com.studyolle.event;

import com.studyolle.domain.Account;
import com.studyolle.domain.Enrollment;
import com.studyolle.domain.Event;
import com.studyolle.domain.Study;
import com.studyolle.enrollment.EnrollmentRepository;
import com.studyolle.event.form.EventForm;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;

import javax.validation.Valid;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModelMapper modelMapper;

    public Event createEvent(Event event, Study study, Account account) {
        event.setCreatedBy(account);
        event.setCreatedDateTime(LocalDateTime.now());
        event.setStudy(study);
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, EventForm eventForm) {
        modelMapper.map(eventForm, event);
        // TODO 모집 인원을 늘린 선착순 모임의 경우에, 자동으로 추가 인원의 참가 신청을 확정 상태로 변경해야 한다. (나중에 할 일)
    }

    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }


    public Enrollment createEnrollment(Event event, Account account) {
        Enrollment enrollment = new Enrollment();
        enrollment.setEvent(event);
        enrollment.setAccount(account);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setAccepted(event.isAbleToAcceptAutomaticallyEnrollment());
        event.getEnrollments().add(enrollment);
        enrollmentRepository.save(enrollment);
        return enrollment;
    }

    public void removeEnrollment(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        event.removeEnrollment(enrollment);
        enrollmentRepository.deleteById(enrollment.getId());

    }
}
