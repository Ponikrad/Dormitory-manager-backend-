package com.dorm.manag;

import com.dorm.manag.dto.CreatePaymentRequest;
import com.dorm.manag.dto.PaymentDto;
import com.dorm.manag.entity.Payment;
import com.dorm.manag.entity.PaymentMethod;
import com.dorm.manag.entity.PaymentStatus;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.PaymentRepository;
import com.dorm.manag.service.PaymentService;
import com.dorm.manag.service.PdfService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PdfService pdfService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldCreatePaymentSuccessfully() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student_test");
        user.setRoomNumber("101");

        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setDescription("Czynsz za marzec");
        request.setPaymentType("RENT");
        request.setCurrency("PLN");
        request.setDueDate(LocalDateTime.now().plusDays(7));

        Payment savedPayment = new Payment();
        savedPayment.setId(10L);
        savedPayment.setUser(user);
        savedPayment.setAmount(request.getAmount());
        savedPayment.setStatus(PaymentStatus.PENDING);
        savedPayment.setTransactionId("TXN-12345678");

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentDto result = paymentService.createPayment(request, user);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("500.00"), result.getAmount());

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}