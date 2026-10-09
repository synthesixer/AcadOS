package com.project.acados.state;

import com.project.acados.domain.entity.Section;
import com.project.acados.domain.enums.SectionStatus;

/**
 * State of an ACTIVE section: cancelling moves it to CANCELLED.
 */
public class ActiveSectionState implements SectionState {

    @Override
    public void cancel(Section section) {
        section.setStatus(SectionStatus.CANCELLED);
        section.setState(new CancelledSectionState());
    }
}
