package com.project.acados.state;

import com.project.acados.domain.entity.Section;
import com.project.acados.domain.enums.SectionStatus;
import com.project.acados.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectionStateTest {

    private Section section(SectionStatus status) {
        return Section.builder()
                .id(1L)
                .sectionNumber(1)
                .capacity(40)
                .status(status)
                .build();
    }

    @Test
    void getState_whenStatusActive_returnsActiveState() {
        assertThat(section(SectionStatus.ACTIVE).getState()).isInstanceOf(ActiveSectionState.class);
    }

    @Test
    void getState_whenStatusCancelled_returnsCancelledState() {
        assertThat(section(SectionStatus.CANCELLED).getState()).isInstanceOf(CancelledSectionState.class);
    }

    @Test
    void getState_whenBuiltWithoutStatus_defaultsToActiveState() {
        Section section = Section.builder().sectionNumber(1).capacity(40).build();

        assertThat(section.getStatus()).isEqualTo(SectionStatus.ACTIVE);
        assertThat(section.getState()).isInstanceOf(ActiveSectionState.class);
    }

    @Test
    void cancel_whenActive_changesStatusAndStateToCancelled() {
        Section section = section(SectionStatus.ACTIVE);

        section.cancel();

        assertThat(section.getStatus()).isEqualTo(SectionStatus.CANCELLED);
        assertThat(section.getState()).isInstanceOf(CancelledSectionState.class);
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsBusinessRuleException() {
        Section section = section(SectionStatus.ACTIVE);
        section.cancel();

        assertThatThrownBy(section::cancel).isInstanceOf(BusinessRuleException.class);
        assertThat(section.getStatus()).isEqualTo(SectionStatus.CANCELLED);
    }

    @Test
    void setStatus_afterStateWasRead_keepsStateInSyncWithStatus() {
        Section section = section(SectionStatus.ACTIVE);
        assertThat(section.getState()).isInstanceOf(ActiveSectionState.class);

        section.setStatus(SectionStatus.CANCELLED);

        assertThat(section.getState()).isInstanceOf(CancelledSectionState.class);
    }
}
