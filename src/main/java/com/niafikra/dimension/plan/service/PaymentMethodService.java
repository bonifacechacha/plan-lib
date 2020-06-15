package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.plan.domain.PaymentMethod;
import com.niafikra.dimension.plan.event.SaveEvent;
import com.niafikra.dimension.plan.repository.PaymentMethodRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/26/17 12:29 PM
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class PaymentMethodService {

    private PaymentMethodRepository paymentMethodRepository;
    private ApplicationEventPublisher publisher;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository, ApplicationEventPublisher publisher) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.publisher = publisher;
    }

    @Transactional
    public PaymentMethod save(PaymentMethod paymentMethod) {
        paymentMethod = paymentMethodRepository.save(paymentMethod);
        publisher.publishEvent(new SaveEvent<PaymentMethod>(paymentMethod));
        return paymentMethod;
    }

    @Transactional
    public void delete(PaymentMethod paymentMethod) {
        paymentMethodRepository.delete(paymentMethod);
    }

    public Page<PaymentMethod> findPaymentMethods(String nameFilter, Pageable pageable) {
        return paymentMethodRepository.findAllByNameContainingIgnoreCase(nameFilter, pageable);
    }

    public Long countPaymentMethods(String nameFilter) {
        return paymentMethodRepository.countAllByNameContainingIgnoreCase(nameFilter);
    }

    public List<PaymentMethod> getAll() {
        return paymentMethodRepository.findAll();
    }
}
