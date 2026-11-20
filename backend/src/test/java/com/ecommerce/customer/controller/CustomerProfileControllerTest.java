package com.ecommerce.customer.controller;

import com.ecommerce.common.ratelimit.RateLimitingFilter;
import com.ecommerce.customer.dto.request.UpdateCustomerProfileRequest;
import com.ecommerce.customer.dto.response.CustomerProfileResponse;
import com.ecommerce.customer.service.CustomerProfileService;
import com.ecommerce.identity.security.JwtAuthenticationConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = CustomerProfileController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            RateLimitingFilter.class,
            JwtAuthenticationConverter.class
        }
    )
)
@Import(CustomerProfileControllerTest.TestSecurityConfig.class)
@TestPropertySource(properties = "logging.level.org.springframework.security=TRACE")
class CustomerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerProfileService customerProfileService;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                .csrf(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .requestCache(cache -> cache.disable())
                .build();
        }
    }

    @Test
    void shouldReturnCustomerProfile() throws Exception {
        CustomerProfileResponse response =
            new CustomerProfileResponse(101L, "john@example.com", "John", "Doe");

        when(customerProfileService.getMyProfile(101L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/me")
                .with(jwt()
                    .jwt(token -> token.subject("101"))
                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(101))
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(customerProfileService).getMyProfile(101L);
    }

    @Test
    void shouldUpdateCustomerProfile() throws Exception {
        CustomerProfileResponse response =
            new CustomerProfileResponse(101L, "john@example.com", "Johnny", "Doe");

        when(customerProfileService.updateMyProfile(eq(101L), any(UpdateCustomerProfileRequest.class)))
            .thenReturn(response);

        mockMvc.perform(patch("/api/v1/customers/me")
                .with(authenticatedJwt("CUSTOMER"))
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Johnny"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Johnny"))
            .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(customerProfileService).updateMyProfile(eq(101L), any(UpdateCustomerProfileRequest.class));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me"))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(customerProfileService);
    }

    @Test
    void shouldRejectUserWithoutCustomerRole() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me")
                .with(jwt()
                    .jwt(token -> token.subject("101"))
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isForbidden());

        verifyNoInteractions(customerProfileService);
    }

    @Test
    void shouldRejectPatchWithoutCsrfToken() throws Exception {
        mockMvc.perform(patch("/api/v1/customers/me")
                .with(authenticatedJwt("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Johnny"
                    }
                    """))
            .andExpect(status().isForbidden());

        verifyNoInteractions(customerProfileService);
    }

    @Test
    void shouldHaveCsrfFilterForCustomerPatch() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/customers/me");
        request.setServletPath("/api/v1/customers/me");

        boolean csrfEnabled = springSecurityFilterChain.getFilterChains().stream()
            .filter(chain -> chain.matches(request))
            .findFirst()
            .map(chain -> chain.getFilters().stream().anyMatch(CsrfFilter.class::isInstance))
            .orElse(false);

        assertTrue(csrfEnabled, "CsrfFilter is missing from the security filter chain");
    }

    @Test
    void shouldRejectPatchWithInvalidCsrfToken() throws Exception {
        mockMvc.perform(patch("/api/v1/customers/me")
                .with(authenticatedJwt("CUSTOMER"))
                .with(csrf().useInvalidToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "Johnny"
                    }
                    """))
            .andExpect(status().isForbidden());

        verifyNoInteractions(customerProfileService);
    }

    private static RequestPostProcessor authenticatedJwt(String role) {
        Jwt jwt = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject("101")
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        return authentication(authentication);
    }
}
