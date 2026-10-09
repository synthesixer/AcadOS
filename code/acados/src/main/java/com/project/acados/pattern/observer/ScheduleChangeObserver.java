package com.project.acados.pattern.observer;

import com.project.acados.domain.entity.Section;

import java.util.List;

/**
 * Observer side of the schedule change Observer pattern.
 * ScheduleChangePublisher calls every attached observer when the timetable of sections changes.
 * Reference: class diagram.puml (observer)
 * Observer interface for listening to schedule changes.
 * Reference: class diagram.puml (§8 observer), Implement_Plan-AcadOS.md §7, §17.2
 */
public interface ScheduleChangeObserver {

    void onScheduleChanged(List<Section> sections);
}

