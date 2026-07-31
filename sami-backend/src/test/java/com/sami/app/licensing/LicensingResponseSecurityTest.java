package com.sami.app.licensing;

import com.sami.app.licensing.dto.LicensingDtos.LicenseResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LicensingResponseSecurityTest {

    @Test
    void normalLicenseResponseDoesNotExposeLicenseKey() {
        boolean exposesLicenseKey = Arrays.stream(LicenseResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .anyMatch("licenseKey"::equals);

        assertFalse(exposesLicenseKey,
                "Normal license responses must not expose the raw license key");
    }

    @Test
    void normalLicenseResponseDoesNotExposeActivationFingerprint() {
        boolean exposesFingerprint = Arrays.stream(LicenseResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .anyMatch("activationFingerprint"::equals);

        assertFalse(exposesFingerprint,
                "Normal license responses must not expose the activation fingerprint");
    }
}
