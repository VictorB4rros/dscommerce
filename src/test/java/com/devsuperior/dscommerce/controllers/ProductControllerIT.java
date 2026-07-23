package com.devsuperior.dscommerce.controllers;

import com.devsuperior.dscommerce.dto.ProductDTO;
import com.devsuperior.dscommerce.tests.ProductFactory;
import com.devsuperior.dscommerce.tests.TokenUtil;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String productName, adminUsername, clientUsername, password, adminBearerToken, clientBearerToken, invalidToken;
    private Long existingProductId, nonExistingProductId, dependentProductId;

    @BeforeEach
    void setUp() throws Exception {
        productName = "Macbook";

        adminUsername = "alex@gmail.com";
        clientUsername = "maria@gmail.com";
        password = "123456";
        adminBearerToken = tokenUtil.obtainAccessToken(mockMvc, adminUsername, password);
        clientBearerToken = tokenUtil.obtainAccessToken(mockMvc, clientUsername, password);
        invalidToken = adminBearerToken + "xpto"; // Simulates wrong password
        existingProductId = 25L;
        nonExistingProductId = 26L;
        dependentProductId = 3L;
    }

    @Test
    public void findAllShouldReturnPageWhenNameParamIsNotEmpty() throws Exception {
        ResultActions result = mockMvc.perform(get("/products?name={productName}", productName).accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.content[0].id").value(3L));
        result.andExpect(jsonPath("$.content[0].name").value("Macbook Pro"));
        result.andExpect(jsonPath("$.content[0].price").value(1250.0));
        result.andExpect(jsonPath("$.content[0].imgUrl").value("https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/3-big.jpg"));
    }

    @Test
    public void findAllShouldReturnPageWhenNameParamIsEmpty() throws Exception {
        ResultActions result = mockMvc.perform(get("/products").accept(MediaType.APPLICATION_JSON));

        result.andExpect(jsonPath("$.content[0].id").value(1L));
        result.andExpect(jsonPath("$.content[0].name").value("The Lord of the Rings"));
        result.andExpect(jsonPath("$.content[0].price").value(90.5));
        result.andExpect(jsonPath("$.content[0].imgUrl").value("https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/1-big.jpg"));
    }

    @Test
    public void insertShouldReturnProductDTOCreatedWhenAdminLogged() throws Exception {
        ProductDTO productDTO = ProductFactory.createProductDTO();
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        String expectedName = productDTO.getName();
        String expectedDescription = productDTO.getDescription();
        Double expectedPrice = productDTO.getPrice();
        String expectedImgUrl = productDTO.getImgUrl();
        Long expectedCategory = productDTO.getCategories().getFirst().getId();

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + adminBearerToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print());

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").value(26L));
        result.andExpect(jsonPath("$.name").value(expectedName));
        result.andExpect(jsonPath("$.description").value(expectedDescription));
        result.andExpect(jsonPath("$.price").value(expectedPrice));
        result.andExpect(jsonPath("$.imgUrl").value(expectedImgUrl));
        result.andExpect(jsonPath("$.categories[0].id").value(expectedCategory));
    }

    @Test
    public void insertShouldReturnUnprocessableEntityWhenInvalidProductNameAndAdminLogged() throws Exception {
        ProductDTO productDTO = ProductFactory.createInvalidProductDTO("name");
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + adminBearerToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableEntity());
        result.andExpect(jsonPath("$.error").value("Dados inválidos"));
        result.andExpect(jsonPath("$.errors[0].fieldName").value("name"));
        result.andExpect(jsonPath("$.errors[0].message").value("Campo requerido"));
    }

    @Test
    public void insertShouldReturnUnprocessableEntityWhenInvalidProductDescriptionAndAdminLogged() throws Exception {
        ProductDTO productDTO = ProductFactory.createInvalidProductDTO("description");
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + adminBearerToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableEntity());
        result.andExpect(jsonPath("$.error").value("Dados inválidos"));
        result.andExpect(jsonPath("$.errors[0].fieldName").value("description"));
        result.andExpect(jsonPath("$.errors[0].message").value("Campo requerido"));
    }

    @Test
    public void insertShouldReturnUnprocessableEntityWhenNegativeProductPriceAndAdminLogged() throws Exception {
        ProductDTO productDTO = ProductFactory.createInvalidProductDTO("negative price");
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + adminBearerToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableEntity());
        result.andExpect(jsonPath("$.error").value("Dados inválidos"));
        result.andExpect(jsonPath("$.errors[0].fieldName").value("price"));
        result.andExpect(jsonPath("$.errors[0].message").value("O preço deve ser positivo"));
    }

    @Test
    public void insertShouldReturnUnprocessableEntityWhenZeroProductPriceAndAdminLogged() throws Exception {
        ProductDTO productDTO = ProductFactory.createInvalidProductDTO("price equals zero");
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + adminBearerToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableEntity());
        result.andExpect(jsonPath("$.error").value("Dados inválidos"));
        result.andExpect(jsonPath("$.errors[0].fieldName").value("price"));
        result.andExpect(jsonPath("$.errors[0].message").value("O preço deve ser positivo"));
    }

    @Test
    public void insertShouldReturnUnprocessableEntityWhenNoCategoryAndAdminLogged() throws Exception {
        ProductDTO productDTO = ProductFactory.createInvalidProductDTO("no category");
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + adminBearerToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnprocessableEntity());
        result.andExpect(jsonPath("$.error").value("Dados inválidos"));
        result.andExpect(jsonPath("$.errors[0].fieldName").value("categories"));
        result.andExpect(jsonPath("$.errors[0].message").value("Deve ter pelo menos uma categoria"));
    }

    @Test
    public void insertShouldReturnForbiddenWhenClientLogged() throws Exception {
        ProductDTO productDTO = ProductFactory.createProductDTO();
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + clientBearerToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
    }

    @Test
    public void insertShouldReturnUnauthorizedWhenInvalidToken() throws Exception {
        ProductDTO productDTO = ProductFactory.createProductDTO();
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/products")
                .header("Authorization", "Bearer " + invalidToken)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteShouldReturnNoContentWhenProductIdExistsAndAdminLogged() throws Exception {
        ResultActions result = mockMvc.perform(delete("/products/{existingProductId}", existingProductId)
                .header("Authorization", "Bearer " + adminBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNoContent());
    }

    @Test
    public void deleteShouldReturnNotFoundForNonExistingProductWhenAdminLogged() throws Exception {
        ResultActions result = mockMvc.perform(delete("/products/{nonExistingProductId}", nonExistingProductId)
                .header("Authorization", "Bearer " + adminBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    @Test
    @Transactional(propagation = Propagation.SUPPORTS)
    public void deleteShouldReturnBadRequestForDependentProductWhenAdminLogged() throws Exception {
        ResultActions result = mockMvc.perform(delete("/products/{dependentProductId}", dependentProductId)
                .header("Authorization", "Bearer " + adminBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
    }

    @Test
    public void deleteShouldReturnForbiddenWhenClientLogged() throws Exception {
        ResultActions result = mockMvc.perform(delete("/products/{existingProductId}", existingProductId)
                .header("Authorization", "Bearer " + clientBearerToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
    }

    @Test
    public void deleteShouldReturnUnauthorizedWhenInvalidToken() throws Exception {
        ResultActions result = mockMvc.perform(delete("/products/{existingProductId}", existingProductId)
                .header("Authorization", "Bearer " + invalidToken)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnauthorized());
    }
}
