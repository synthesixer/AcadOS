package com.project.acados.state;

import com.project.acados.domain.entity.Section;

/**
 * State Pattern: behaviour of a Section that depends on its current status.
 * Reference: class diagram.puml (state), Section State Diagram (State Pattern).puml
 */
public interface SectionState {

    void cancel(Section section);
}
