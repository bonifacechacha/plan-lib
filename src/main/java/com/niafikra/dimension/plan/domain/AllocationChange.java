package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.money.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "PL_AllocationChange")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "amount", "user"})
public class AllocationChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;


    @CreationTimestamp
    private LocalDateTime timeCreated;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    private String reason;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "update_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "update_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "update_currency"))
    })
    private Money amount;

    @ManyToOne
    private User user;


    public AllocationChange(User user, Money amount, String reason) {
        this.amount = amount;
        this.reason = reason;
        this.user = user;
    }
}
