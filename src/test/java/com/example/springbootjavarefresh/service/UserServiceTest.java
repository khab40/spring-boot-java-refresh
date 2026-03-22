package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.AdminUpdateUserRequest;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserRole;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void shouldUpdateUserWithAdminFields() {
        User user = new User();
        user.setId(7L);
        user.setEmail("old@example.com");
        user.setFirstName("Old");
        user.setLastName("Name");
        user.setRole(UserRole.USER);
        user.setEmailVerified(Boolean.FALSE);

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail("new@example.com");
        request.setFirstName("New");
        request.setLastName("Name");
        request.setCompany("Desk");
        request.setCountry("US");
        request.setPhoneNumber("+1");
        request.setRole(UserRole.ADMIN);
        request.setEmailVerified(Boolean.TRUE);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        User updated = userService.updateUserAdmin(7L, request);

        assertEquals("new@example.com", updated.getEmail());
        assertEquals("New", updated.getFirstName());
        assertEquals("Desk", updated.getCompany());
        assertEquals(UserRole.ADMIN, updated.getRole());
        assertEquals(true, updated.isEmailVerified());
        assertEquals(LocalDateTime.class, updated.getEmailVerifiedAt().getClass());
    }

    @Test
    void shouldUpdateOnlyRoleWhenRequested() {
        User user = new User();
        user.setId(11L);
        user.setRole(UserRole.USER);

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer((invocation) -> invocation.getArgument(0));

        User updated = userService.updateUserRole(11L, UserRole.ADMIN);

        assertEquals(UserRole.ADMIN, updated.getRole());
    }
}
