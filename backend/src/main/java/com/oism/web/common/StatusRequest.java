package com.oism.web.common;

import jakarta.validation.constraints.NotNull;

/** Body chung cua PATCH .../status: bat/tat (active) mot doi tuong. */
public record StatusRequest(@NotNull(message = "active không được để trống") Boolean active) {
}
