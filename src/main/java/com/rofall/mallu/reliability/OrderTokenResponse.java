package com.rofall.mallu.reliability;

import java.time.Instant;
public record OrderTokenResponse(String token, Instant expiresAt) { }
