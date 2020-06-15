package com.niafikra.dimension.plan.repository;

import com.niafikra.dimension.core.util.repository.BaseRepository;
import com.niafikra.dimension.plan.domain.Payment;
import org.springframework.stereotype.Repository;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 10/2/17 8:12 PM
 */
@Repository
public interface PaymentRepository extends BaseRepository<Payment, Long> {
}
