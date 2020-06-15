package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.money.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/2/17 2:28 PM
 */
@Entity
@Table(name = "PL_Allocation")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "budget", "role", "resource"})
public class Allocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;


    @CreationTimestamp
    private LocalDateTime timeCreated;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @ManyToOne
    @NotNull
    private Budget budget;

    @ManyToOne
    @NotNull
    private Role role;

    @ManyToOne
    @NotNull
    private Resource resource;

    @ManyToOne
    @NotNull
    private User creator;

    private String description;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "proposed_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "proposed_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "proposed_currency"))
    })
    private Money proposedAmount;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "allocated_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "allocated_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "allocated_currency"))
    })
    private Money allocatedAmount;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<AllocationChange> changes;

    public Allocation(Budget budget) {
        this.budget = budget;
        this.changes = new LinkedHashSet<>();
        this.allocatedAmount = Money.getZERO();
        this.proposedAmount = Money.getZERO();
    }

    public Allocation(Budget budget, Role role, Resource resource, Money proposedAmount) {
        this(budget, role, resource);
        this.proposedAmount = proposedAmount;
    }

    public Allocation(Budget budget, Role role, Resource resource) {
        this(budget);
        this.role = role;
        this.resource = resource;
    }

    public String toString() {
        return new StringBuilder()
                .append(proposedAmount)
                .append(" | ")
                .append(allocatedAmount)
                .append(" on ")
                .append(resource)
                .append(" for ")
                .append(role)
                .toString();
    }
}
