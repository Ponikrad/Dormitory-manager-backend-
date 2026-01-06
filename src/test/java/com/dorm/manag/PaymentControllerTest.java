package com.dorm.manag;

import com.dorm.manag.dto.CreatePaymentRequest;
import com.dorm.manag.dto.PaymentDto;
import com.dorm.manag.entity.PaymentMethod;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.PaymentService;
import com.dorm.manag.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "testuser") // Symulacja zalogowanego użytkownika
    void shouldReturnCreatedStatusWhenPaymentIsValid() throws Exception {
        // Given
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.BLIK);
        request.setDescription("Opłata za klucz");

        User mockUser = new User();
        mockUser.setUsername("testuser");

        PaymentDto mockResponse = new PaymentDto();
        mockResponse.setId(1L);
        mockResponse.setAmount(new BigDecimal("100.00"));

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(paymentService.createPayment(any(CreatePaymentRequest.class), eq(mockUser)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/payments/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // Oczekiwany kod 201
                .andExpect(jsonPath("$.payment.amount").value(100.00))
                .andExpect(jsonPath("$.message").value("Payment created successfully"));
    }
}