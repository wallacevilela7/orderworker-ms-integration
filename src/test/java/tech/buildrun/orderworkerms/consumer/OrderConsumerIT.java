package tech.buildrun.orderworkerms.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import tech.buildrun.orderworkerms.ContainersConfig;
import tech.buildrun.orderworkerms.ServiceConnectionConfig;
import tech.buildrun.orderworkerms.dto.OrderDto;
import tech.buildrun.orderworkerms.dto.OrderEventDto;
import tech.buildrun.orderworkerms.entity.Order;
import tech.buildrun.orderworkerms.repository.OrderRepository;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static tech.buildrun.orderworkerms.consumer.OrderConsumer.ORDER_CONFIRMED_QUEUE;
import static tech.buildrun.orderworkerms.producer.ShippingProducer.SHIPPING_QUEUE;

@Import(ServiceConnectionConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderConsumerIT extends ContainersConfig {

    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    public static void setup() {
        setupSqs();
    }

    @BeforeEach
    public void beforeEach() {
        orderRepository.deleteAll();
        var sqsClient = getSqsClient();
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(ORDER_CONFIRMED_QUEUE).build());
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(SHIPPING_QUEUE).build());
    }

    @DisplayName("When existing order should publish to shipping queue")
    @Test
    void whenExistingOrderShouldPublishToShippingQueue() throws JsonProcessingException {
        // Arrange
        var orderNUmber = "1234";
        var orderEventDto = new OrderEventDto(orderNUmber);
        var payload = objectMapper.writeValueAsString(orderEventDto);

        Order orderEntity = new Order();
        orderEntity.setOrderNumber(orderNUmber);
        orderEntity.setCustomerEmail("test@email.com");
        orderEntity.setNotified(false);

        orderRepository.save(orderEntity);

        // Act - publicar na fila de pedido confirmado (passo1)
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE, payload);

        // Assert - (aguardar o tempo de processamento das filas e salvamento com Awaitility await)
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var message = sqsTemplate.receive(SHIPPING_QUEUE, String.class);

            assertTrue(message.isPresent());

            var dto = objectMapper.readValue(message.get().getPayload(), OrderDto.class);
            assertEquals(orderNUmber, dto.orderNumber());
            assertEquals(orderEntity.getCustomerEmail(), dto.customerEmail());
        });
    }

    @DisplayName("When existing order should update database")
    @Test
    void whenExistingOrderShouldUpdateDatabase() throws JsonProcessingException {
        // Arrange
        var orderNumber = "1234";
        var orderEventDto = new OrderEventDto(orderNumber);
        var payload = objectMapper.writeValueAsString(orderEventDto);

        Order orderEntity = new Order();
        orderEntity.setOrderNumber(orderNumber);
        orderEntity.setCustomerEmail("test@email.com");
        orderEntity.setNotified(false);

        orderRepository.save(orderEntity);

        // Act - publicar na fila de pedido confirmado (passo1)
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE, payload);

        // Assert
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var orderDb = orderRepository.findByOrderNumber(orderNumber);

            assertTrue(orderDb.isPresent());
            assertTrue(orderDb.get().isNotified());
        });
    }

    @DisplayName("When order not found should not publish to shipping queue")
    @Test
    void whenOrderNotFoundShouldNotPublishToShippingQueue() throws JsonProcessingException {
        // Arrange
        var orderNUmber = "1234";
        var orderEventDto = new OrderEventDto(orderNUmber);
        var payload = objectMapper.writeValueAsString(orderEventDto);

        // Act - publicar na fila de pedido confirmado (passo1)
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE, payload);

        // Assert - (aguardar o tempo de processamento das filas e salvamento com Awaitility await)
        Awaitility.await().atMost(12, TimeUnit.SECONDS).untilAsserted(() -> {
            var message = sqsTemplate.receive(SHIPPING_QUEUE, String.class);
            assertTrue(message.isEmpty());
        });
    }
}