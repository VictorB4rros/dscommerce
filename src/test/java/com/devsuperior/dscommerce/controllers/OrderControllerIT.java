package com.devsuperior.dscommerce.controllers;

import com.devsuperior.dscommerce.dto.OrderDTO;
import com.devsuperior.dscommerce.entities.User;
import com.devsuperior.dscommerce.tests.OrderFactory;
import com.devsuperior.dscommerce.tests.TokenUtil;
import com.devsuperior.dscommerce.tests.UserFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private Long existingOrderId, nonExistingOrderId, existingOrderIdOtherClient;
    private String adminUsername, clientUsername, password, adminBearerToken, clientBearerToken, invalidToken;
    private User client;

    @BeforeEach
    void setUp() throws Exception {
        existingOrderId = 1L;
        existingOrderIdOtherClient = 2L;
        nonExistingOrderId = 100L;

        adminUsername = "alex@gmail.com";
        clientUsername = "maria@gmail.com";
        password = "123456";
        adminBearerToken = tokenUtil.obtainAccessToken(mockMvc, adminUsername, password);
        clientBearerToken = tokenUtil.obtainAccessToken(mockMvc, clientUsername, password);
        invalidToken = adminBearerToken + "xpto"; // Simulates wrong password
        client = UserFactory.createClientUser();
    }

    @Test
    public void findByIdShouldReturnOrderDTOWhenIdExistsAndAdminLogged() throws Exception {
        ResultActions result = mockMvc.perform(get("/orders/{existingOrderId}", existingOrderId)
                .header("Authorization", "Bearer " + adminBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(1L));
        result.andExpect(jsonPath("$.status").value("PAID"));
        result.andExpect(jsonPath("$.client").exists());
        result.andExpect(jsonPath("$.client.id").value(1L));
        result.andExpect(jsonPath("$.client.name").value("Maria Brown"));
        result.andExpect(jsonPath("$.payment").exists());
        result.andExpect(jsonPath("$.items").exists());
        result.andExpect(jsonPath("$.items[1].name").value("Macbook Pro"));
        result.andExpect(jsonPath("$.total").exists());
    }

    @Test
    public void findByIdShouldReturnOrderDTOWhenIdExistsAndClientLogged() throws Exception {
        ResultActions result = mockMvc.perform(get("/orders/{existingOrderId}", existingOrderId)
                .header("Authorization", "Bearer " + clientBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(1L));
        result.andExpect(jsonPath("$.status").value("PAID"));
        result.andExpect(jsonPath("$.client").exists());
        result.andExpect(jsonPath("$.client.id").value(1L));
        result.andExpect(jsonPath("$.client.name").value("Maria Brown"));
        result.andExpect(jsonPath("$.payment").exists());
        result.andExpect(jsonPath("$.items").exists());
        result.andExpect(jsonPath("$.items[1].name").value("Macbook Pro"));
        result.andExpect(jsonPath("$.total").exists());
    }

    @Test
    public void findByIdShouldReturnForbiddenWhenIdExistsAndClientLoggedAndOrderDoesNotBelongToClient() throws Exception {
        ResultActions result = mockMvc.perform(get("/orders/{existingOrderIdOtherClient}", existingOrderIdOtherClient)
                .header("Authorization", "Bearer " + clientBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
    }

    @Test
    public void findByIdShouldReturnNotFoundWhenIdDoesNotExistAndAdminLogged() throws Exception {
        ResultActions result = mockMvc.perform(get("/orders/{nonExistingOrderId}", nonExistingOrderId)
                .header("Authorization", "Bearer " + adminBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    @Test
    public void findByIdShouldReturnNotFoundWhenIdDoesNotExistAndClientLogged() throws Exception {
        ResultActions result = mockMvc.perform(get("/orders/{nonExistingOrderId}", nonExistingOrderId)
                .header("Authorization", "Bearer " + clientBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    @Test
    public void findByIdShouldReturnUnauthorizedWhenIdExistsAndInvalidToken() throws Exception {
        ResultActions result = mockMvc.perform(get("/orders/{existingOrderId}", existingOrderId)
                .header("Authorization", "Bearer " + invalidToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    public void insertShouldReturnOrderDTOCreatedWhenClientLogged() throws Exception {
        OrderDTO orderDTO = OrderFactory.createOrderDTO(client);
        String jsonBody = objectMapper.writeValueAsString(orderDTO);

        Long expectedProductId = orderDTO.getItems().getFirst().getProductId();

        ResultActions result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + clientBearerToken)
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print());

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").value(4L));
        result.andExpect(jsonPath("$.status").value("WAITING_PAYMENT"));
        result.andExpect(jsonPath("$.client.name").value("Maria Brown"));
        result.andExpect(jsonPath("$.items[0].productId").value(expectedProductId));
    }

    @Test
    public void insertShouldReturnUnprocessableEntityWhenOrderHasNoItemsAndClientLogged() throws Exception {
        OrderDTO orderDTO = OrderFactory.createInvalidOrderDTO(client);
        String jsonBody = objectMapper.writeValueAsString(orderDTO);

        ResultActions result = mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + clientBearerToken)
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print());

        result.andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void insertShouldReturnUnauthorizedWhenInvalidToken() throws Exception {
        OrderDTO orderDTO = OrderFactory.createOrderDTO(client);
        String jsonBody = objectMapper.writeValueAsString(orderDTO);

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + invalidToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnauthorized());
    }
}
