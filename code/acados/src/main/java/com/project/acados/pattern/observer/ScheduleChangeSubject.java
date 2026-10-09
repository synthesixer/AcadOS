package com.project.acados.pattern.observer;

import com.project.acados.domain.entity.Section;

import java.util.List;

/** Subject side of the schedule change Observer pattern. */
public interface ScheduleChangeSubject {

    void attach(ScheduleChangeObserver observer);

    void detach(ScheduleChangeObserver observer);

    void notifyObservers(List<Section> sections);
}
