package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.UserEntitlement;
import com.example.springbootjavarefresh.entity.UserRole;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.PaymentService;
import com.example.springbootjavarefresh.service.UserEntitlementService;
import com.example.springbootjavarefresh.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
private MockMvc mockMvc;
    @Mock
    private UserService userService;
    @Mock
    private UserEntitlementService userEntitlementService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private UserDetailsService userDetailsService;
    @InjectMocks
    private UserController userController;



    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

@Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetUserById() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("trader@example.com");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setRole(UserRole.ADMIN);
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("trader@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListAllUsers() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("trader@example.com");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setRole(UserRole.USER);
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Alice"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateUser() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setEmail("new@example.com");
        user.setFirstName("New");
        user.setLastName("User");
        user.setRole(UserRole.USER);
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
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUser() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setEmail("updated@example.com");
        user.setFirstName("Updated");
        user.setLastName("User");
        user.setRole(UserRole.ADMIN);
        user.setEmailVerified(Boolean.TRUE);
        when(userService.updateUserAdmin(org.mockito.ArgumentMatchers.eq(2L), any())).thenReturn(user);

        mockMvc.perform(put("/api/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "updated@example.com",
                                  "firstName": "Updated",
                                  "lastName": "User",
                                  "company": "Desk",
                                  "country": "US",
                                  "phoneNumber": "+1-555-1234",
                                  "role": "ADMIN",
                                  "emailVerified": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserRole() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setEmail("ops@example.com");
        user.setFirstName("Ops");
        user.setLastName("User");
        user.setRole(UserRole.ADMIN);
        when(userService.updateUserRole(3L, UserRole.ADMIN)).thenReturn(user);

        mockMvc.perform(patch("/api/users/3/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnUserEntitlements() throws Exception {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setId(30L);
        com.example.springbootjavarefresh.entity.DataProduct product = new com.example.springbootjavarefresh.entity.DataProduct();
        product.setId(5L);
        product.setCode("PX");
        product.setName("Product X");
        product.setCurrency("usd");
        product.setPrice(new java.math.BigDecimal("1.00"));
        product.setAccessType(com.example.springbootjavarefresh.entity.ProductAccessType.ONE_TIME_PURCHASE);
        product.setBillingInterval(com.example.springbootjavarefresh.entity.BillingInterval.ONE_TIME);
        entitlement.setProduct(product);
        when(userEntitlementService.getEntitlementsByUserId(1L)).thenReturn(List.of(entitlement));

        mockMvc.perform(get("/api/users/1/entitlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30L))
                .andExpect(jsonPath("$[0].product.code").value("PX"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnUserPayments() throws Exception {
        com.example.springbootjavarefresh.entity.PaymentTransaction payment = new com.example.springbootjavarefresh.entity.PaymentTransaction();
        payment.setId(40L);
        payment.setCurrency("usd");
        payment.setStatus(com.example.springbootjavarefresh.entity.PaymentTransactionStatus.SUCCEEDED);
        payment.setAmount(new java.math.BigDecimal("99.99"));
        com.example.springbootjavarefresh.entity.DataProduct product = new com.example.springbootjavarefresh.entity.DataProduct();
        product.setId(6L);
        product.setCode("PY");
        product.setName("Product Y");
        product.setCurrency("usd");
        product.setPrice(new java.math.BigDecimal("99.99"));
        product.setAccessType(com.example.springbootjavarefresh.entity.ProductAccessType.ONE_TIME_PURCHASE);
        product.setBillingInterval(com.example.springbootjavarefresh.entity.BillingInterval.ONE_TIME);
        payment.setProduct(product);
        when(paymentService.getTransactionsByUserId(1L)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/users/1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(40L))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[0].product.code").value("PY"));
    }
}