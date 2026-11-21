package com.ecommerce.customer.service.impl;

import com.ecommerce.customer.dto.request.CreateCustomerAddressRequest;
import com.ecommerce.customer.dto.request.UpdateCustomerAddressRequest;
import com.ecommerce.customer.dto.response.CustomerAddressResponse;
import com.ecommerce.customer.entity.CustomerAddress;
import com.ecommerce.customer.entity.CustomerProfile;
import com.ecommerce.customer.exception.CustomerAddressNotFoundException;
import com.ecommerce.customer.exception.CustomerProfileNotFoundException;
import com.ecommerce.customer.exception.InvalidCustomerAddressException;
import com.ecommerce.customer.repository.CustomerAddressRepository;
import com.ecommerce.customer.repository.CustomerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAddressServiceImplTest {

    @Mock
    private CustomerAddressRepository addressRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @InjectMocks
    private CustomerAddressServiceImpl service;

    @Test
    void shouldCreateFirstAddressAsDefault() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.existsByCustomerProfileUserId(101L)).thenReturn(false);
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAddressResponse response = service.createAddress(101L, request());

        assertTrue(response.isDefault());
        assertEquals("John Doe", response.recipientName());
        assertEquals("Pune", response.city());

        verify(addressRepository).save(argThat(CustomerAddress::isDefault));
    }

    @Test
    void shouldCreateSubsequentAddressAsNonDefault() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.existsByCustomerProfileUserId(101L)).thenReturn(true);
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAddressResponse response = service.createAddress(101L, request());

        assertFalse(response.isDefault());

        verify(addressRepository).save(argThat(address -> !address.isDefault()));
    }

    @Test
    void shouldRejectCreationWhenCustomerProfileDoesNotExist() {
        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.empty());

        assertThrows(CustomerProfileNotFoundException.class, () -> service.createAddress(101L, request()));

        verifyNoInteractions(addressRepository);
    }

    @Test
    void shouldRetrieveAllCustomerAddresses() {
        CustomerAddress home = CustomerAddress.builder()
            .id(1L)
            .label("Home")
            .city("Pune")
            .isDefault(true)
            .build();

        CustomerAddress office = CustomerAddress.builder()
            .id(2L)
            .label("Office")
            .city("Mumbai")
            .isDefault(false)
            .build();

        when(addressRepository.findByCustomerProfileUserIdOrderByCreatedAtDesc(101L))
            .thenReturn(List.of(home, office));

        List<CustomerAddressResponse> responses = service.getMyAddresses(101L);

        assertEquals(2, responses.size());
        assertEquals("Home", responses.get(0).label());
        assertEquals("Office", responses.get(1).label());
    }

    @Test
    void shouldReturnEmptyListWhenCustomerHasNoAddresses() {
        when(addressRepository.findByCustomerProfileUserIdOrderByCreatedAtDesc(101L))
            .thenReturn(List.of());

        List<CustomerAddressResponse> responses = service.getMyAddresses(101L);

        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldRetrieveAddressOwnedByCustomer() {
        CustomerAddress address = CustomerAddress.builder()
            .id(501L)
            .label("Home")
            .city("Pune")
            .isDefault(true)
            .build();

        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L))
            .thenReturn(Optional.of(address));

        CustomerAddressResponse response = service.getMyAddress(101L, 501L);

        assertEquals(501L, response.id());
        assertEquals("Home", response.label());
    }

    @Test
    void shouldRejectAddressNotOwnedByCustomer() {
        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L))
            .thenReturn(Optional.empty());

        assertThrows(CustomerAddressNotFoundException.class, () -> service.getMyAddress(101L, 501L));

        verify(addressRepository).findByIdAndCustomerProfileUserId(501L, 101L);
    }

    @Test
    void shouldUpdateOnlyProvidedField() {
        CustomerAddress address = CustomerAddress.builder()
            .id(501L)
            .recipientName("John Doe")
            .city("Pune")
            .isDefault(true)
            .build();

        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L))
            .thenReturn(Optional.of(address));

        UpdateCustomerAddressRequest request = new UpdateCustomerAddressRequest(
            null, null, null, null, null, null, null, "Mumbai", null, null, null);

        CustomerAddressResponse response = service.updateAddress(101L, 501L, request);

        assertEquals("Mumbai", response.city());
        assertEquals("John Doe", response.recipientName());
        assertTrue(response.isDefault());

        verify(addressRepository, never()).save(any(CustomerAddress.class));
    }

    @Test
    void shouldRejectBlankRecipientName() {
        CustomerAddress address = CustomerAddress.builder()
            .id(501L)
            .recipientName("John Doe")
            .isDefault(true)
            .build();

        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L))
            .thenReturn(Optional.of(address));

        UpdateCustomerAddressRequest request = new UpdateCustomerAddressRequest(
            null, "   ", null, null, null, null, null, null, null, null, null);

        assertThrows(InvalidCustomerAddressException.class, () -> service.updateAddress(101L, 501L, request));

        verify(addressRepository, never()).save(any(CustomerAddress.class));
    }

    @Test
    void shouldRejectUpdateForAnotherCustomersAddress() {
        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L))
            .thenReturn(Optional.empty());

        UpdateCustomerAddressRequest request = new UpdateCustomerAddressRequest(
            null, null, null, null, null, null, null, "Mumbai", null, null, null);

        assertThrows(CustomerAddressNotFoundException.class, () -> service.updateAddress(101L, 501L, request));

        verify(addressRepository, never()).save(any(CustomerAddress.class));
    }

    @Test
    void shouldNotChangeDefaultStatusDuringAddressUpdate() {
        CustomerAddress address = CustomerAddress.builder()
            .id(501L)
            .recipientName("John Doe")
            .city("Pune")
            .isDefault(true)
            .build();

        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L))
            .thenReturn(Optional.of(address));

        UpdateCustomerAddressRequest request = new UpdateCustomerAddressRequest(
            null, null, null, null, null, null, null, "Mumbai", null, null, null);

        service.updateAddress(101L, 501L, request);

        assertTrue(address.isDefault());
    }

    @Test
    void shouldChangeDefaultAddress() {
        CustomerProfile profile = mock(CustomerProfile.class);

        CustomerAddress home = CustomerAddress.builder()
            .id(501L)
            .isDefault(true)
            .build();

        CustomerAddress office = CustomerAddress.builder()
            .id(502L)
            .isDefault(false)
            .build();

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.findByIdAndCustomerProfileUserId(502L, 101L)).thenReturn(Optional.of(office));
        when(addressRepository.findByCustomerProfileUserIdAndIsDefaultTrue(101L)).thenReturn(Optional.of(home));

        doAnswer(invocation -> {
            assertFalse(home.isDefault());
            assertFalse(office.isDefault());
            return null;
        }).when(addressRepository).flush();

        CustomerAddressResponse response = service.setDefaultAddress(101L, 502L);

        assertFalse(home.isDefault());
        assertTrue(office.isDefault());
        assertTrue(response.isDefault());

        verify(addressRepository).flush();
    }

    @Test
    void shouldNotModifyAddressWhenAlreadyDefault() {
        CustomerProfile profile = mock(CustomerProfile.class);

        CustomerAddress home = CustomerAddress.builder()
            .id(501L)
            .isDefault(true)
            .build();

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L)).thenReturn(Optional.of(home));

        CustomerAddressResponse response = service.setDefaultAddress(101L, 501L);

        assertTrue(response.isDefault());

        verify(addressRepository, never()).flush();
        verify(addressRepository, never()).save(any());
        verify(addressRepository, never()).findByCustomerProfileUserIdAndIsDefaultTrue(anyLong());
    }

    @Test
    void shouldRejectSettingAnotherCustomersAddressAsDefault() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.findByIdAndCustomerProfileUserId(502L, 101L)).thenReturn(Optional.empty());

        assertThrows(CustomerAddressNotFoundException.class, () -> service.setDefaultAddress(101L, 502L));

        verify(addressRepository, never()).flush();
    }

    @Test
    void shouldDeleteNonDefaultAddressWithoutChangingDefault() {
        CustomerProfile profile = mock(CustomerProfile.class);

        CustomerAddress office = CustomerAddress.builder()
            .id(502L)
            .isDefault(false)
            .build();

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.findByIdAndCustomerProfileUserId(502L, 101L)).thenReturn(Optional.of(office));

        service.deleteAddress(101L, 502L);

        verify(addressRepository).delete(office);
        verify(addressRepository, never()).findFirstByCustomerProfileUserIdAndIdNotOrderByIdAsc(anyLong(), anyLong());
        verify(addressRepository, never()).flush();
    }

    @Test
    void shouldSelectReplacementWhenDeletingDefaultAddress() {
        CustomerProfile profile = mock(CustomerProfile.class);

        CustomerAddress home = CustomerAddress.builder()
            .id(501L)
            .isDefault(true)
            .build();

        CustomerAddress office = CustomerAddress.builder()
            .id(502L)
            .isDefault(false)
            .build();

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L)).thenReturn(Optional.of(home));
        when(addressRepository.findFirstByCustomerProfileUserIdAndIdNotOrderByIdAsc(101L, 501L))
            .thenReturn(Optional.of(office));

        doAnswer(invocation -> {
            assertFalse(office.isDefault());
            return null;
        }).when(addressRepository).flush();

        service.deleteAddress(101L, 501L);

        verify(addressRepository).delete(home);
        verify(addressRepository).flush();

        assertTrue(office.isDefault());

        InOrder order = inOrder(addressRepository);
        order.verify(addressRepository).delete(home);
        order.verify(addressRepository).flush();
    }

    @Test
    void shouldDeleteOnlyAddressWithoutReplacement() {
        CustomerProfile profile = mock(CustomerProfile.class);

        CustomerAddress home = CustomerAddress.builder()
            .id(501L)
            .isDefault(true)
            .build();

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L)).thenReturn(Optional.of(home));
        when(addressRepository.findFirstByCustomerProfileUserIdAndIdNotOrderByIdAsc(101L, 501L))
            .thenReturn(Optional.empty());

        service.deleteAddress(101L, 501L);

        verify(addressRepository).delete(home);
        verify(addressRepository).flush();
    }

    @Test
    void shouldRejectDeletingAnotherCustomersAddress() {
        CustomerProfile profile = mock(CustomerProfile.class);

        when(customerProfileRepository.findByUserIdForUpdate(101L)).thenReturn(Optional.of(profile));
        when(addressRepository.findByIdAndCustomerProfileUserId(501L, 101L)).thenReturn(Optional.empty());

        assertThrows(CustomerAddressNotFoundException.class, () -> service.deleteAddress(101L, 501L));

        verify(addressRepository, never()).delete(any(CustomerAddress.class));
    }

    private CreateCustomerAddressRequest request() {
        return new CreateCustomerAddressRequest(
            "Home",
            "John Doe",
            "+919876543210",
            "Flat 101, Green Residency",
            "Baner Road",
            "Near City Park",
            "Baner",
            "Pune",
            "Maharashtra",
            "411045",
            "IN"
        );
    }
}
