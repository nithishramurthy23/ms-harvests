package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.CatalogProduct;
import com.nithishramurthy.farm.fresh.dto.TodayOffer;
import com.nithishramurthy.farm.fresh.entity.Category;
import com.nithishramurthy.farm.fresh.entity.DiscountRule;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.repository.DiscountRuleRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DiscountRuleRepository discountRuleRepository;

    @InjectMocks
    private ProductService productService;

    private Category fruits;
    private Category vegetables;

    @BeforeEach
    void setUp() {
        fruits = category(1L, "fruits");
        vegetables = category(2L, "vegetables");
    }

    @Test
    void currentPriceIsNormalPriceWhenNoDiscountToday() {
        when(discountRuleRepository.findByDayOfWeekAndActive(any(DayOfWeek.class), eq(true)))
                .thenReturn(List.of());

        Product apple = product(fruits, "180.00", "199.00");

        assertThat(productService.getCurrentPrice(apple)).isEqualByComparingTo("180.00");
    }

    @Test
    void currentPriceAppliesDiscountForMatchingCategory() {
        when(discountRuleRepository.findByDayOfWeekAndActive(any(DayOfWeek.class), eq(true)))
                .thenReturn(List.of(rule(fruits, "10")));

        Product apple = product(fruits, "199.00", "199.00");

        // 10% off 199 = 179.1, rounded to whole rupees like the frontend does
        assertThat(productService.getCurrentPrice(apple)).isEqualByComparingTo("179");
    }

    @Test
    void discountForAnotherCategoryIsIgnored() {
        when(discountRuleRepository.findByDayOfWeekAndActive(any(DayOfWeek.class), eq(true)))
                .thenReturn(List.of(rule(vegetables, "15")));

        Product apple = product(fruits, "199.00", "199.00");

        assertThat(productService.getCurrentPrice(apple)).isEqualByComparingTo("199.00");
    }

    @Test
    void productWithoutCategoryNeverGetsDiscount() {
        Product giftCard = product(null, "500.00", "500.00");

        assertThat(productService.getCurrentPrice(giftCard)).isEqualByComparingTo("500.00");
    }

    @Test
    void discountUsesOriginalPriceNotCurrentPrice() {
        when(discountRuleRepository.findByDayOfWeekAndActive(any(DayOfWeek.class), eq(true)))
                .thenReturn(List.of(rule(fruits, "20")));

        // price was already lowered once; the discount must not stack on top of it
        Product mango = product(fruits, "80.00", "100.00");

        assertThat(productService.getCurrentPrice(mango)).isEqualByComparingTo("80");
        assertThat(mango.getPrice()).isEqualByComparingTo("80.00");
    }

    @Test
    void catalogShowsTodaysPriceFullPriceAndStock() {
        when(discountRuleRepository.findByDayOfWeekAndActive(any(DayOfWeek.class), eq(true)))
                .thenReturn(List.of(rule(fruits, "10")));
        Product plums = product(fruits, "199.00", "199.00");
        plums.setId(101L);
        plums.setStockQuantity(4);
        Product carrots = product(vegetables, "40.00", "40.00");
        carrots.setId(201L);
        carrots.setStockQuantity(0);
        when(productRepository.findAll()).thenReturn(List.of(carrots, plums));

        List<CatalogProduct> catalog = productService.listCatalog();

        assertThat(catalog).extracting(CatalogProduct::id).containsExactly(101L, 201L);
        CatalogProduct discounted = catalog.get(0);
        assertThat(discounted.price()).isEqualByComparingTo("179");
        assertThat(discounted.mrp()).isEqualByComparingTo("199.00");
        assertThat(discounted.offPercent()).isEqualByComparingTo("10");
        assertThat(discounted.inStock()).isTrue();

        CatalogProduct soldOut = catalog.get(1);
        assertThat(soldOut.mrp()).isNull();
        assertThat(soldOut.stock()).isZero();
        assertThat(soldOut.inStock()).isFalse();
    }

    @Test
    void todayOfferUsesTheCategorySlug() {
        Category herbs = category(3L, "Herbs & Spices");
        when(discountRuleRepository.findByDayOfWeekAndActive(any(DayOfWeek.class), eq(true)))
                .thenReturn(List.of(rule(herbs, "20")));

        TodayOffer offer = productService.todayOffer().orElseThrow();

        assertThat(offer.category()).isEqualTo("herbs-&-spices");
        assertThat(offer.percent()).isEqualByComparingTo("20");
    }

    private static Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private static Product product(Category category, String price, String originalPrice) {
        Product product = new Product();
        product.setName("Test product");
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setOriginalPrice(new BigDecimal(originalPrice));
        return product;
    }

    private static DiscountRule rule(Category category, String percent) {
        DiscountRule rule = new DiscountRule();
        rule.setCategory(category);
        rule.setDiscountPercent(new BigDecimal(percent));
        rule.setActive(true);
        return rule;
    }
}
