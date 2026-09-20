package com.inquiro.business.onboarding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingCatalogTest {

    @Test
    void providesSupportedBusinessTypes() {
        assertThat(OnboardingCatalog.businessTypes())
                .extracting(OnboardingCatalog.BusinessTypeOption::code)
                .containsExactly("HOSPITALITY", "RESTAURANT", "HEALTHCARE", "OTHER");
    }

    @Test
    void providesHospitalityServiceTemplates() {
        assertThat(OnboardingCatalog.servicesFor("HOSPITALITY"))
                .extracting(OnboardingCatalog.ServiceTemplate::code)
                .containsExactly("ROOM_BOOKING", "AIRPORT_PICKUP");
    }

    @Test
    void providesRestaurantAndHealthcareTemplates() {
        assertThat(OnboardingCatalog.servicesFor("restaurant"))
                .extracting(OnboardingCatalog.ServiceTemplate::code)
                .containsExactly("TABLE_RESERVATION", "BUFFET_RESERVATION");

        assertThat(OnboardingCatalog.servicesFor("HEALTHCARE"))
                .extracting(OnboardingCatalog.ServiceTemplate::code)
                .containsExactly("DOCTOR_APPOINTMENT");
    }

    @Test
    void unknownBusinessTypeDoesNotInventServices() {
        assertThat(OnboardingCatalog.servicesFor("UNKNOWN")).isEmpty();
    }
}
