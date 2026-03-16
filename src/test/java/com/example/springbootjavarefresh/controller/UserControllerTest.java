package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.UserEntitlementService;
import com.example.springbootjavarefresh.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserEntitlementService userEntitlementService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetUserById() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("trader@example.com");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("trader@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListAllUsers() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("trader@example.com");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Alice"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateUser() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setEmail("new@example.com");
        user.setFirstName("New");
        user.setLastName("User");
        when(userService.createUser(any())).thenReturn(user);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com",
                                  "firstName": "New",
                                  "lastName": "User",
                                  "password": "super-secret",
                                  "company": "Desk",
                                  "country": "US"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnUserEntitlements() throws Exception {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(30L);
        when(userEntitlementService.getEntitlementsByUserId(1L)).thenReturn(List.of(entitlement));

        mockMvc.perform(get("/api/users/1/entitlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30L));
    }
}
