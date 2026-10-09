package com.project.acados.state;

import com.project.acados.domain.entity.Section;
import com.project.acados.exception.BusinessRuleException;

/**
 * State of a CANCELLED section: it cannot be cancelled again.
 */
public class CancelledSectionState implements SectionState {

    @Override
    public void cancel(Section section) {
        throw new BusinessRuleException("Section นี้ถูกยกเลิกไปแล้ว");
    }
}
