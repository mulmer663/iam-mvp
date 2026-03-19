package com.iam.registry.application;

import com.iam.registry.domain.user.IamUser;
import com.iam.registry.domain.user.IamUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistryServiceTest {

    @Mock
    private IamUserRepository userRepository;

    @InjectMocks
    private UserRegistryService userRegistryService;

    @Test
    @DisplayName("IamUser 엔티티 저장 단위 테스트")
    void test_saveUser() {
        // given
        IamUser userToSave = IamUser.builder()
                .userName("test.user")
                .externalId("EXT-001")
                .active(true)
                .build();

        IamUser savedMockUser = IamUser.builder()
                .id(1L)
                .userName("test.user")
                .externalId("EXT-001")
                .active(true)
                .build();

        when(userRepository.save(any(IamUser.class))).thenReturn(savedMockUser);

        // when
        IamUser result = userRegistryService.saveUser(userToSave);

        // then
        verify(userRepository, times(1)).save(any(IamUser.class));
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("EXT-001", result.getExternalId());
    }
}
