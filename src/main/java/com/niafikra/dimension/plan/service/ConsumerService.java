package com.niafikra.dimension.plan.service;

import com.niafikra.dimension.category.domain.Category;
import com.niafikra.dimension.plan.domain.Consumer;
import com.niafikra.dimension.plan.domain.QConsumer;
import com.niafikra.dimension.plan.repository.ConsumerRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
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
 * @date 8/31/17 12:10 PM
 */

@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ConsumerService {
    private ConsumerRepository consumerRepository;

    public ConsumerService(ConsumerRepository consumerRepository) {
        this.consumerRepository = consumerRepository;
    }

    @Transactional
    public Consumer save(Consumer consumer) {
        consumer = consumerRepository.save(consumer);
        return consumer;
    }

    public void delete(Consumer consumer) {
        consumerRepository.delete(consumer);
    }

    public Long countConsumers(String codeFilter, String nameFilter, Category category) {
        return consumerRepository.count(createPredicate(codeFilter, nameFilter, category));
    }

    private Predicate createPredicate(String codeFilter, String nameFilter, Category category) {
        QConsumer consumer = QConsumer.consumer;
        BooleanBuilder query = new BooleanBuilder();

        if (category != null)
            query.and(consumer.category.eq(category));
        if (codeFilter != null)
            query.and(consumer.code.containsIgnoreCase(codeFilter));
        if (nameFilter != null)
            query.and(consumer.name.containsIgnoreCase(nameFilter));

        return query;
    }

    public Page<Consumer> findConsumers(String codeFilter, String nameFilter, Category category, Pageable pageable) {
        return consumerRepository.findAll(createPredicate(codeFilter, nameFilter, category), pageable);
    }

    public List<Consumer> getAllActive(boolean active) {
        return consumerRepository.findAllByActive(active);
    }

    public List<Consumer> findAll() {
        return consumerRepository.findAll();
    }

    public Consumer getConsumer(Long consumerId) {
        return consumerRepository.findById(consumerId)
                .orElseThrow(() -> new IllegalArgumentException("There is no consumer with id " + consumerId));
    }
}
