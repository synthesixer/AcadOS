package com.project.acados.pattern.observer;

import com.project.acados.domain.entity.Section;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Publishes changed sections to the registered schedule change observers. */
@Component
public class ScheduleChangePublisher implements ScheduleChangeSubject {

    private final List<ScheduleChangeObserver> observers = new ArrayList<>();

    @Override
    public void attach(ScheduleChangeObserver observer) {
        observers.add(observer);
    }

    @Override
    public void detach(ScheduleChangeObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(List<Section> sections) {
        observers.forEach(observer -> observer.onScheduleChanged(sections));
    }
}
