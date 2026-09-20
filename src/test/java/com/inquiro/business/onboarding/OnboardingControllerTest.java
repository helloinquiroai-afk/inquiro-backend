package com.inquiro.business.onboarding;

import com.inquiro.auth.TenantAuthorizationService;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class OnboardingControllerTest {

    @Test
    void catalogUsesPersistedBusinessType() {
        BusinessAccountRepository repository = mock(BusinessAccountRepository.class);
        TenantAuthorizationService authorization = mock(TenantAuthorizationService.class);
        OnboardingService service = mock(OnboardingService.class);

        when(repository.findByBusinessId("biz_1"))
                .thenReturn(new BusinessAccount(
                        "biz_1",
                        "Hotel One",
                        new BusinessProfile("Hotel One", "HOSPITALITY", "", List.of(), null)));

        OnboardingController controller =
                new OnboardingController(service, repository, authorization);

        var response = controller.catalog("biz_1");

        verify(authorization).requireBusinessAccess("biz_1");
        assert response.businessTypes().size() == 4;
        assert response.suggestedServices().size() == 2;
    }
}
