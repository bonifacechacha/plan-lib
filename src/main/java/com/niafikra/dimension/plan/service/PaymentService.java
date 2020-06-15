package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.core.security.SecurityUtils;
import com.niafikra.dimension.core.security.service.UserService;
import com.niafikra.dimension.plan.domain.Payment;
import com.niafikra.dimension.plan.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 10/2/17 8:11 PM
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class PaymentService {

    private PaymentRepository paymentRepository;
    private UserService userService;

    public PaymentService(PaymentRepository paymentRepository, UserService userService) {
        this.paymentRepository = paymentRepository;
        this.userService = userService;
    }

    @Transactional
    public Payment create(Payment payment) {
        payment.setTimeCreated(LocalDateTime.now());
        payment.setCreator(SecurityUtils.getCurrentUser(userService));

        return paymentRepository.save(payment);
    }

    public Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("There is no payment with id :" + id));
    }
}
