package com.rofall.mallu.reliability;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderTokenRequest(@NotEmpty List<@NotNull Long> cartItemIds) { }
