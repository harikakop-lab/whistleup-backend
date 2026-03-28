package com.whistleup.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class VhsWebhookAuthServiceTest {

    @Test
    void authorize_blankSecret_allowsAll() {
        VhsWebhookAuthService svc = new VhsWebhookAuthService();
        ReflectionTestUtils.setField(svc, "configuredSecret", "");
        assertThat(svc.authorize(null, null)).isTrue();
        assertThat(svc.authorize("wrong", null)).isTrue();
    }

    @Test
    void authorize_matchesHeaderOrBearer() {
        VhsWebhookAuthService svc = new VhsWebhookAuthService();
        ReflectionTestUtils.setField(svc, "configuredSecret", "secret-xyz");
        assertThat(svc.authorize(null, null)).isFalse();
        assertThat(svc.authorize("secret-xyz", null)).isTrue();
        assertThat(svc.authorize(null, "Bearer secret-xyz")).isTrue();
        assertThat(svc.authorize(null, "bearer secret-xyz")).isTrue();
        assertThat(svc.authorize("other", null)).isFalse();
    }
}
