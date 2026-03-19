package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.entity.AuthProvider;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldMapRequestIntoUserEntity() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("buyer@example.com");
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setPassword("super-secret");
        request.setCompany("Quant Desk");
        request.setCountry("UK");
        request.setPhoneNumber("+44-555-000");

        User savedUser = new User();
        savedUser.setId(7L);
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-secret");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(savedUser);

        User result = userService.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertEquals("buyer@example.com", persisted.getEmail());
        assertEquals("Ada", persisted.getFirstName());
        assertEquals("Lovelace", persisted.getLastName());
        assertEquals("hashed-secret", persisted.getPasswordHash());
        assertEquals(AuthProvider.LOCAL, persisted.getAuthProvider());
        assertEquals("Quant Desk", persisted.getCompany());
        assertEquals("UK", persisted.getCountry());
        assertEquals("+44-555-000", persisted.getPhoneNumber());
        assertEquals(false, persisted.isEmailVerified());
        assertEquals(7L, result.getId());
    }

    @Test
    void shouldCreateOrUpdateGoogleUser() {
        User savedUser = new User();
        savedUser.setId(9L);
        when(userRepository.findByAuthProviderAndProviderSubject(AuthProvider.GOOGLE, "google-subject"))
                .thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmail("google@example.com")).thenReturn(java.util.Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(savedUser);

        User result = userService.upsertGoogleUser("google@example.com", "Go", "Ogler", "google-subject");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertEquals(AuthProvider.GOOGLE, persisted.getAuthProvider());
        assertEquals("google-subject", persisted.getProviderSubject());
        assertEquals(true, persisted.isEmailVerified());
        assertEquals(9L, result.getId());
    }
}
