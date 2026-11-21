package com.nithishramurthy.farm.fresh.integration;

import com.nithishramurthy.farm.fresh.entity.Category;
import com.nithishramurthy.farm.fresh.entity.Product;
import com.nithishramurthy.farm.fresh.mainprog.FarmFreshApplication;
import com.nithishramurthy.farm.fresh.repository.CategoryRepository;
import com.nithishramurthy.farm.fresh.repository.DiscountRuleRepository;
import com.nithishramurthy.farm.fresh.repository.OrderItemRepository;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The public catalogue the shop loads: prices and stock come from the database. */
@SpringBootTest(classes = FarmFreshApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private DiscountRuleRepository discountRuleRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    @MockBean
    private JavaMailSender mailSender;

    private Product plums;
    private Product honey;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        discountRuleRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category fruits = new Category();
        fruits.setName("Fruits");
        fruits = categoryRepository.save(fruits);

        Category preserves = new Category();
        preserves.setName("Honey & Jams");
        preserves = categoryRepository.save(preserves);

        plums = productRepository.save(product("Ooty Plums", "199.00", 10, fruits));
        honey = productRepository.save(product("Raw Honey", "349.00", 0, preserves));
    }

    @Test
    void catalogueIsPublicAndShowsStock() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ooty Plums"))
                .andExpect(jsonPath("$[0].category").value("fruits"))
                .andExpect(jsonPath("$[0].stock").value(10))
                .andExpect(jsonPath("$[0].inStock").value(true))
                .andExpect(jsonPath("$[1].category").value("honey-&-jams"))
                .andExpect(jsonPath("$[1].stock").value(0))
                .andExpect(jsonPath("$[1].inStock").value(false));
    }

    @Test
    void productGoesOutOfStockWhenTheLastUnitIsTaken() throws Exception {
        plums.setStockQuantity(0);
        productRepository.save(plums);

        mockMvc.perform(get("/api/products/" + plums.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.inStock").value(false));
    }

    @Test
    void unknownProductIs404() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noOfferMeansNoContentAndFullPrices() throws Exception {
        mockMvc.perform(get("/api/offers/today"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + honey.getId()))
                .andExpect(jsonPath("$.price").value(349.0))
                .andExpect(jsonPath("$.mrp").value(nullValue()));
    }

    private static Product product(String name, String price, int stock, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setOriginalPrice(new BigDecimal(price));
        product.setWeight("1 kg");
        product.setStockQuantity(stock);
        product.setCategory(category);
        return product;
    }
}
