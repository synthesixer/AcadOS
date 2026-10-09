package com.project.acados.dto.response;

import java.util.List;

/** Separates the two tabs described by the Teacher Swap Status user flow (T08). */
public record SwapInboxResponse(List<SwapResponse> sent, List<SwapResponse> received) {
}
