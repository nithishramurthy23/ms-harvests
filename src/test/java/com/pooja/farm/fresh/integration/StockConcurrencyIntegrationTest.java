package com.pooja.farm.fresh.integration;

import com.pooja.farm.fresh.entity.Product;
import com.pooja.farm.fresh.mainprog.FarmFreshApplication;
import com.pooja.farm.fresh.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Many customers try to buy the last few items at the same moment.
 * The conditional UPDATE in ProductRepository.decrementStock must make sure
 * exactly as many succeed as there is stock, and the count never goes negative.
 */
@SpringBootTest(classes = FarmFreshApplication.class)
@ActiveProfiles("test")
class StockConcurrencyIntegrationTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void concurrentBuyersCannotOversell() throws Exception {
        Product honey = new Product();
        honey.setName("Wild Honey");
        honey.setPrice(new BigDecimal("350.00"));
        honey.setOriginalPrice(new BigDecimal("350.00"));
        honey.setStockQuantity(5);
        Long id = productRepository.save(honey).getId();

        int buyers = 20;
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < buyers; i++) {
            Callable<Boolean> buyOne = () -> {
                start.await();
                return transactionTemplate.execute(tx -> productRepository.decrementStock(id, 1) == 1);
            };
            results.add(pool.submit(buyOne));
        }
        start.countDown();

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                succeeded++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(5);
        assertThat(productRepository.findById(id).orElseThrow().getStockQuantity()).isZero();
    }
}
