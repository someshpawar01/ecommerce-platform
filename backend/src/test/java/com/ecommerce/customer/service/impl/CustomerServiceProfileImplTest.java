package com.ecommerce.customer.service.impl;

import com.ecommerce.customer.dto.request.UpdateCustomerProfileRequest;
import com.ecommerce.customer.dto.response.CustomerProfileResponse;
import com.ecommerce.customer.entity.CustomerProfile;
import com.ecommerce.customer.exception.CustomerProfileNotFoundException;
import com.ecommerce.customer.exception.InvalidCustomerProfileException;
import com.ecommerce.customer.repository.CustomerProfileRepository;
import com.ecommerce.identity.api.CustomerIdentityReader;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    @Mock
    private CustomerProfileRepository repository;

    @Mock
    private CustomerIdentityReader identityReader;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CustomerProfileServiceImpl service;


    @Test
    void shouldRetrieveCustomerProfile() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(profile.getUserId()).thenReturn(101L);
        when(profile.getFirstName()).thenReturn("John");
        when(profile.getLastName()).thenReturn("Doe");
        when(repository.findById(101L)).thenReturn(Optional.of(profile));
        when(identityReader.getEmail(101L)).thenReturn("john@example.com");

        CustomerProfileResponse response = service.getMyProfile(101L);

        assertEquals(101L, response.userId());
        assertEquals("john@example.com", response.email());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());

        verify(repository).findById(101L);
        verify(identityReader).getEmail(101L);
    }

    @Test
    void shouldUpdateFirstName() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(profile.getUserId()).thenReturn(101L);
        when(repository.findById(101L)).thenReturn(Optional.of(profile));

        service.updateMyProfile(101L, new UpdateCustomerProfileRequest("Johnny", null));

        verify(profile).setFirstName("Johnny");
        verify(profile, never()).setLastName(anyString());
    }

    @Test
    void shouldUpdateLastNameOnly() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(profile.getUserId()).thenReturn(101L);
        when(repository.findById(101L)).thenReturn(Optional.of(profile));

        service.updateMyProfile(101L, new UpdateCustomerProfileRequest(null, "Smith"));

        verify(profile, never()).setFirstName(anyString());
        verify(profile).setLastName("Smith");
    }

    @Test
    void shouldTrimProvidedNames() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(profile.getUserId()).thenReturn(101L);
        when(repository.findById(101L)).thenReturn(Optional.of(profile));

        service.updateMyProfile(101L, new UpdateCustomerProfileRequest(" John ", " Smith "));

        verify(profile).setFirstName("John");
        verify(profile).setLastName("Smith");
    }

    @Test
    void shouldRejectBlankFirstName() {
        CustomerProfile profile = mock(CustomerProfile.class);
        when(repository.findById(101L)).thenReturn(Optional.of(profile));

        assertThrows(
            InvalidCustomerProfileException.class,
            () -> service.updateMyProfile(101L, new UpdateCustomerProfileRequest("   ", null)));

        verify(profile, never()).setFirstName(anyString());
    }

    @Test
    void shouldRejectBlankLastName() {
        CustomerProfile profile = mock(CustomerProfile.class);
        when(repository.findById(101L)).thenReturn(Optional.of(profile));

        assertThrows(
            InvalidCustomerProfileException.class,
            () -> service.updateMyProfile(101L, new UpdateCustomerProfileRequest(null, "   ")));

        verify(profile, never()).setLastName(anyString());
    }

    @Test
    void shouldRejectMissingCustomerProfile() {
        when(repository.findById(101L)).thenReturn(Optional.empty());

        assertThrows(CustomerProfileNotFoundException.class, () -> service.getMyProfile(101L));

        verifyNoInteractions(identityReader);
    }

    @Test
    void shouldNotModifyProfileWhenNoFieldsProvided() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(profile.getUserId()).thenReturn(101L);
        when(repository.findById(101L)).thenReturn(Optional.of(profile));

        service.updateMyProfile(101L, new UpdateCustomerProfileRequest(null, null));

        verify(profile, never()).setFirstName(anyString());
        verify(profile, never()).setLastName(anyString());
    }

    @Test
    void shouldRetrieveCustomerProfileWithoutEntityManagerInteraction() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(profile.getUserId()).thenReturn(101L);
        when(profile.getFirstName()).thenReturn("John");
        when(profile.getLastName()).thenReturn("Doe");
        when(repository.findById(101L)).thenReturn(Optional.of(profile));
        when(identityReader.getEmail(101L)).thenReturn("john@example.com");

        CustomerProfileResponse response = service.getMyProfile(101L);

        assertEquals(101L, response.userId());
        assertEquals("john@example.com", response.email());

        verifyNoInteractions(entityManager);
    }
}
