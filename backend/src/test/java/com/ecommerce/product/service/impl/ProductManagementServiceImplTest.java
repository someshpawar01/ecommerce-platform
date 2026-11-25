package com.ecommerce.product.service.impl;

import com.ecommerce.product.dto.request.AssignProductCategoriesRequest;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductCategory;
import com.ecommerce.product.entity.id.ProductCategoryId;
import com.ecommerce.product.enums.ProductStatusEnum;
import com.ecommerce.product.exception.BrandNotFoundException;
import com.ecommerce.product.exception.InvalidProductCategoryException;
import com.ecommerce.product.exception.InvalidProductException;
import com.ecommerce.product.exception.InvalidProductStateException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductResponseMapper;
import com.ecommerce.product.policy.CategoryVisibilityPolicy;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductCategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.utils.SlugGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductManagementServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryVisibilityPolicy categoryVisibilityPolicy;

    @Mock
    private ProductResponseMapper responseMapper;

    @Spy
    private SlugGenerator slugGenerator = new SlugGenerator();

    @InjectMocks
    private ProductManagementServiceImpl service;

    private Product product;
    private Brand samsung;
    private Category electronics;
    private Category mobiles;
    private Category smartphones;

    @BeforeEach
    void setUp() {
        samsung = Brand.builder()
            .id(1L)
            .name("Samsung")
            .slug("samsung")
            .active(true)
            .build();

        product = Product.builder()
            .id(101L)
            .name("Samsung Galaxy S25")
            .slug("samsung-galaxy-s25")
            .brand(samsung)
            .status(ProductStatusEnum.DRAFT)
            .build();

        electronics = Category.builder()
            .id(1L)
            .name("Electronics")
            .slug("electronics")
            .active(true)
            .build();

        mobiles = Category.builder()
            .id(2L)
            .name("Mobiles")
            .slug("mobiles")
            .parent(electronics)
            .active(true)
            .build();

        smartphones = Category.builder()
            .id(3L)
            .name("Smartphones")
            .slug("smartphones")
            .parent(mobiles)
            .active(true)
            .build();
    }

    // -------------------------------------------------------------------------
    // CREATE PRODUCT
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateDraftProductSuccessfully() {
        CreateProductRequest request = new CreateProductRequest(
            "Samsung Galaxy S25", 1L, "Flagship smartphone", "Detailed description");

        when(brandRepository.findById(1L)).thenReturn(Optional.of(samsung));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });
        when(responseMapper.toResponse(any(Product.class), eq(List.of())))
            .thenAnswer(invocation -> productResponse(invocation.getArgument(0)));

        ProductResponse response = service.createProduct(request);

        assertNotNull(response);
        assertEquals(101L, response.id());
        assertEquals("Samsung Galaxy S25", response.name());
        assertEquals("samsung-galaxy-s25", response.slug());
        assertEquals(ProductStatusEnum.DRAFT, response.status());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();

        assertEquals("Samsung Galaxy S25", saved.getName());
        assertEquals("samsung-galaxy-s25", saved.getSlug());
        assertEquals(samsung, saved.getBrand());
        assertEquals("Flagship smartphone", saved.getShortDescription());
        assertEquals("Detailed description", saved.getDescription());
        assertEquals(ProductStatusEnum.DRAFT, saved.getStatus());
    }

    @Test
    void shouldCreateProductWithoutBrand() {
        CreateProductRequest request = new CreateProductRequest("Generic USB Cable", null, null, null);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseMapper.toResponse(any(Product.class), eq(List.of())))
            .thenAnswer(invocation -> productResponse(invocation.getArgument(0)));

        ProductResponse response = service.createProduct(request);

        assertEquals("Generic USB Cable", response.name());
        assertEquals("generic-usb-cable", response.slug());

        verifyNoInteractions(brandRepository);
    }

    @Test
    void shouldNormalizeProductNameAndShortDescription() {
        CreateProductRequest request = new CreateProductRequest(
            "  Samsung Galaxy S25  ", null, "  Flagship smartphone  ", null);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseMapper.toResponse(any(Product.class), eq(List.of())))
            .thenAnswer(invocation -> productResponse(invocation.getArgument(0)));

        ProductResponse response = service.createProduct(request);

        assertEquals("Samsung Galaxy S25", response.name());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        assertEquals("Flagship smartphone", captor.getValue().getShortDescription());
    }

    @Test
    void shouldGenerateUniqueSlugWhenCollisionExists() {
        CreateProductRequest request = new CreateProductRequest("Samsung Galaxy S25", null, null, null);

        when(productRepository.existsBySlug("samsung-galaxy-s25")).thenReturn(true);
        when(productRepository.existsBySlug("samsung-galaxy-s25-2")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseMapper.toResponse(any(Product.class), eq(List.of())))
            .thenAnswer(invocation -> productResponse(invocation.getArgument(0)));

        ProductResponse response = service.createProduct(request);

        assertEquals("samsung-galaxy-s25-3", response.slug());

        verify(productRepository).existsBySlug("samsung-galaxy-s25");
        verify(productRepository).existsBySlug("samsung-galaxy-s25-2");
        verify(productRepository).existsBySlug("samsung-galaxy-s25-3");
    }

    @Test
    void shouldRejectNullProductName() {
        CreateProductRequest request = new CreateProductRequest(null, null, null, null);

        assertThrows(InvalidProductException.class, () -> service.createProduct(request));

        verifyNoInteractions(productRepository, brandRepository);
    }

    @Test
    void shouldRejectBlankProductName() {
        CreateProductRequest request = new CreateProductRequest("   ", null, null, null);

        assertThrows(InvalidProductException.class, () -> service.createProduct(request));

        verifyNoInteractions(productRepository, brandRepository);
    }

    @Test
    void shouldRejectProductNameExceedingMaximumLength() {
        CreateProductRequest request = new CreateProductRequest("A".repeat(256), null, null, null);

        assertThrows(InvalidProductException.class, () -> service.createProduct(request));

        verifyNoInteractions(productRepository, brandRepository);
    }

    @Test
    void shouldRejectProductNameWithoutValidSlug() {
        CreateProductRequest request = new CreateProductRequest("!!!", null, null, null);

        assertThrows(InvalidProductException.class, () -> service.createProduct(request));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectLongShortDescription() {
        CreateProductRequest request = new CreateProductRequest("Phone", null, "A".repeat(501), null);

        assertThrows(InvalidProductException.class, () -> service.createProduct(request));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectNonexistentBrand() {
        CreateProductRequest request = new CreateProductRequest("Samsung Galaxy S25", 999L, null, null);

        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BrandNotFoundException.class, () -> service.createProduct(request));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldRejectInactiveBrand() {
        samsung.setActive(false);

        CreateProductRequest request = new CreateProductRequest("Samsung Galaxy S25", 1L, null, null);

        when(brandRepository.findById(1L)).thenReturn(Optional.of(samsung));

        assertThrows(InvalidProductException.class, () -> service.createProduct(request));

        verify(productRepository, never()).save(any(Product.class));
    }

    // -------------------------------------------------------------------------
    // GET PRODUCT
    // -------------------------------------------------------------------------

    @Test
    void shouldGetProductById() {
        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(productCategoryRepository.findAllByProductIdWithCategory(101L)).thenReturn(List.of());
        when(responseMapper.toResponse(product, List.of())).thenReturn(productResponse(product));

        ProductResponse response = service.getProduct(101L);

        assertNotNull(response);
        assertEquals(101L, response.id());
        assertEquals("Samsung Galaxy S25", response.name());
        assertEquals(ProductStatusEnum.DRAFT, response.status());

        verify(productRepository).findById(101L);
        verify(productCategoryRepository).findAllByProductIdWithCategory(101L);
    }

    @Test
    void shouldGetProductWithCategories() {
        ProductCategory assignment = mock(ProductCategory.class);
        List<ProductCategory> assignments = List.of(assignment);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(productCategoryRepository.findAllByProductIdWithCategory(101L)).thenReturn(assignments);
        when(responseMapper.toResponse(product, assignments)).thenReturn(productResponse(product));

        ProductResponse response = service.getProduct(101L);

        assertNotNull(response);
        verify(responseMapper).toResponse(product, assignments);
    }

    @Test
    void shouldRejectGettingNonexistentProduct() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.getProduct(999L));

        verifyNoInteractions(productCategoryRepository, responseMapper);
    }

    // -------------------------------------------------------------------------
    // UPDATE PRODUCT
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateProductNameWithoutChangingSlug() {
        UpdateProductRequest request = new UpdateProductRequest("Samsung Galaxy S25 5G", null, null, null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(responseMapper.toResponse(eq(product), anyList())).thenAnswer(invocation -> productResponse(product));

        ProductResponse response = service.updateProduct(101L, request);

        assertEquals("Samsung Galaxy S25 5G", response.name());
        assertEquals("samsung-galaxy-s25", response.slug());

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldUpdateProductBrand() {
        Brand apple = Brand.builder().id(2L).name("Apple").active(true).build();

        UpdateProductRequest request = new UpdateProductRequest(null, 2L, null, null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(brandRepository.findById(2L)).thenReturn(Optional.of(apple));
        when(responseMapper.toResponse(eq(product), anyList())).thenAnswer(invocation -> productResponse(product));

        service.updateProduct(101L, request);

        assertEquals(apple, product.getBrand());
        verify(brandRepository).findById(2L);
    }

    @Test
    void shouldUpdateShortDescription() {
        UpdateProductRequest request = new UpdateProductRequest(null, null, "Updated short description", null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(responseMapper.toResponse(eq(product), anyList())).thenAnswer(invocation -> productResponse(product));

        service.updateProduct(101L, request);

        assertEquals("Updated short description", product.getShortDescription());
    }

    @Test
    void shouldUpdateDescription() {
        UpdateProductRequest request = new UpdateProductRequest(null, null, null, "Updated detailed description");

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(responseMapper.toResponse(eq(product), anyList())).thenAnswer(invocation -> productResponse(product));

        service.updateProduct(101L, request);

        assertEquals("Updated detailed description", product.getDescription());
    }

    @Test
    void shouldAllowClearingShortDescription() {
        UpdateProductRequest request = new UpdateProductRequest(null, null, "", null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(responseMapper.toResponse(eq(product), anyList())).thenAnswer(invocation -> productResponse(product));

        service.updateProduct(101L, request);

        assertEquals("", product.getShortDescription());
    }

    @Test
    void shouldPreserveUnspecifiedFields() {
        product.setShortDescription("Original short description");
        product.setDescription("Original description");

        UpdateProductRequest request = new UpdateProductRequest(null, null, null, null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(responseMapper.toResponse(eq(product), anyList())).thenAnswer(invocation -> productResponse(product));

        service.updateProduct(101L, request);

        assertEquals("Samsung Galaxy S25", product.getName());
        assertEquals("Original short description", product.getShortDescription());
        assertEquals("Original description", product.getDescription());
        assertEquals(samsung, product.getBrand());
    }

    @Test
    void shouldRejectBlankNameDuringUpdate() {
        UpdateProductRequest request = new UpdateProductRequest("   ", null, null, null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductException.class, () -> service.updateProduct(101L, request));
    }

    @Test
    void shouldRejectLongNameDuringUpdate() {
        UpdateProductRequest request = new UpdateProductRequest("A".repeat(256), null, null, null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductException.class, () -> service.updateProduct(101L, request));
    }

    @Test
    void shouldRejectLongShortDescriptionDuringUpdate() {
        UpdateProductRequest request = new UpdateProductRequest(null, null, "A".repeat(501), null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductException.class, () -> service.updateProduct(101L, request));
    }

    @Test
    void shouldRejectInactiveBrandDuringUpdate() {
        samsung.setActive(false);

        UpdateProductRequest request = new UpdateProductRequest(null, 1L, null, null);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(samsung));

        assertThrows(InvalidProductException.class, () -> service.updateProduct(101L, request));
    }

    @Test
    void shouldRejectUpdatingActiveProduct() {
        product.setStatus(ProductStatusEnum.ACTIVE);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductStateException.class,
            () -> service.updateProduct(101L, new UpdateProductRequest("Updated", null, null, null)));

        verifyNoInteractions(brandRepository, productCategoryRepository, responseMapper);
    }

    @Test
    void shouldRejectUpdatingArchivedProduct() {
        product.setStatus(ProductStatusEnum.ARCHIVED);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductStateException.class,
            () -> service.updateProduct(101L, new UpdateProductRequest("Updated", null, null, null)));
    }

    @Test
    void shouldRejectUpdatingNonexistentProduct() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
            () -> service.updateProduct(999L, new UpdateProductRequest("Updated", null, null, null)));
    }

    // -------------------------------------------------------------------------
    // ASSIGN CATEGORIES
    // -------------------------------------------------------------------------

    @Test
    void shouldAssignSinglePrimaryCategory() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(3L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(anySet())).thenReturn(List.of(smartphones));
        when(categoryVisibilityPolicy.isVisible(smartphones)).thenReturn(true);
        when(productCategoryRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseMapper.toResponse(eq(product), anyList())).thenReturn(productResponse(product));

        ProductResponse response = service.assignCategories(101L, request);

        assertNotNull(response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductCategory>> captor = ArgumentCaptor.forClass(List.class);

        verify(productCategoryRepository).saveAllAndFlush(captor.capture());

        List<ProductCategory> assignments = captor.getValue();

        assertEquals(1, assignments.size());
        assertEquals(3L, assignments.getFirst().getCategory().getId());
        assertTrue(assignments.getFirst().getPrimary());
        assertEquals(new ProductCategoryId(101L, 3L), assignments.getFirst().getId());
    }

    @Test
    void shouldAssignMultipleCategoriesWithOnePrimary() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(2L, 3L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(anySet())).thenReturn(List.of(mobiles, smartphones));
        when(categoryVisibilityPolicy.isVisible(any(Category.class))).thenReturn(true);
        when(productCategoryRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(responseMapper.toResponse(eq(product), anyList())).thenReturn(productResponse(product));

        service.assignCategories(101L, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductCategory>> captor = ArgumentCaptor.forClass(List.class);

        verify(productCategoryRepository).saveAllAndFlush(captor.capture());

        List<ProductCategory> assignments = captor.getValue();

        assertEquals(2, assignments.size());
        assertEquals(1, assignments.stream().filter(ProductCategory::getPrimary).count());

        ProductCategory primary = assignments.stream()
            .filter(ProductCategory::getPrimary)
            .findFirst()
            .orElseThrow();

        assertEquals(3L, primary.getCategory().getId());
        assertTrue(assignments.stream()
            .allMatch(pc -> pc.getId().getProductId().equals(101L)));
    }

    @Test
    void shouldClearAllCategoryAssignments() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(), null);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(responseMapper.toResponse(eq(product), anyList())).thenReturn(productResponse(product));

        ProductResponse response = service.assignCategories(101L, request);

        assertNotNull(response);

        verify(productRepository).findByIdForUpdate(101L);
        verify(productCategoryRepository).deleteAllByProductId(101L);
        verify(productCategoryRepository, never()).saveAllAndFlush(anyList());
        verify(productCategoryRepository).findAllByProductIdWithCategory(101L);

        verifyNoInteractions(categoryRepository, categoryVisibilityPolicy);
    }

    @Test
    void shouldRejectNullCategoryList() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(null, null);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductCategoryException.class, () -> service.assignCategories(101L, request));

        verify(productCategoryRepository, never()).deleteAllByProductId(anyLong());
    }

    @Test
    void shouldRejectNullCategoryId() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(
            Arrays.asList(1L, null), 1L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductCategoryException.class, () -> service.assignCategories(101L, request));

        verify(productCategoryRepository, never()).deleteAllByProductId(anyLong());
    }

    @Test
    void shouldRejectDuplicateCategoryIds() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(3L, 3L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductCategoryException.class, () -> service.assignCategories(101L, request));

        verifyNoInteractions(categoryRepository, categoryVisibilityPolicy);
    }

    @Test
    void shouldRejectMissingPrimaryCategory() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(2L, 3L), null);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductCategoryException.class, () -> service.assignCategories(101L, request));
    }

    @Test
    void shouldRejectPrimaryCategoryNotIncludedInList() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(2L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductCategoryException.class, () -> service.assignCategories(101L, request));
    }

    @Test
    void shouldRejectPrimaryCategoryWhenAssignmentsAreEmpty() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductCategoryException.class, () -> service.assignCategories(101L, request));
    }

    @Test
    void shouldRejectNonexistentCategory() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(3L, 999L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(anySet())).thenReturn(List.of(smartphones));

        assertThrows(InvalidProductCategoryException.class, () -> service.assignCategories(101L, request));

        verify(productCategoryRepository, never()).deleteAllByProductId(anyLong());
    }

    @Test
    void shouldRejectInvisibleCategory() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(3L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(anySet())).thenReturn(List.of(smartphones));
        when(categoryVisibilityPolicy.isVisible(smartphones)).thenReturn(false);

        assertThrows(InvalidProductCategoryException.class, () -> service.assignCategories(101L, request));

        verify(productCategoryRepository, never()).deleteAllByProductId(anyLong());
    }

    @Test
    void shouldRejectAssigningCategoriesToActiveProduct() {
        product.setStatus(ProductStatusEnum.ACTIVE);

        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(3L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductStateException.class, () -> service.assignCategories(101L, request));

        verifyNoInteractions(categoryRepository, categoryVisibilityPolicy, productCategoryRepository);
    }

    @Test
    void shouldRejectAssigningCategoriesToArchivedProduct() {
        product.setStatus(ProductStatusEnum.ARCHIVED);

        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(3L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));

        assertThrows(InvalidProductStateException.class, () -> service.assignCategories(101L, request));
    }

    @Test
    void shouldRejectAssigningCategoriesToNonexistentProduct() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(3L), 3L);

        when(productRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.assignCategories(999L, request));

        verifyNoInteractions(categoryRepository, categoryVisibilityPolicy, productCategoryRepository);
    }

    @Test
    void shouldDeleteOldAssignmentsBeforeSavingNewOnes() {
        AssignProductCategoriesRequest request = new AssignProductCategoriesRequest(List.of(3L), 3L);

        when(productRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(product));
        when(categoryRepository.findAllById(anySet())).thenReturn(List.of(smartphones));
        when(categoryVisibilityPolicy.isVisible(smartphones)).thenReturn(true);
        when(responseMapper.toResponse(eq(product), anyList())).thenReturn(productResponse(product));

        service.assignCategories(101L, request);

        var inOrder = inOrder(productCategoryRepository);

        inOrder.verify(productCategoryRepository).deleteAllByProductId(101L);
        inOrder.verify(productCategoryRepository).saveAllAndFlush(anyList());
        inOrder.verify(productCategoryRepository).findAllByProductIdWithCategory(101L);
    }

    // -------------------------------------------------------------------------
    // TEST HELPERS
    // -------------------------------------------------------------------------

    private ProductResponse productResponse(Product entity) {
        Brand brand = entity.getBrand();

        return new ProductResponse(
            entity.getId(),
            entity.getName(),
            entity.getSlug(),
            brand == null ? null : brand.getId(),
            brand == null ? null : brand.getName(),
            entity.getShortDescription(),
            entity.getDescription(),
            entity.getStatus(),
            null,
            null,
            null,
            0L,
            List.of()
        );
    }
}
