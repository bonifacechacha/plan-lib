package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.approval.domain.Approvable;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.money.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "PL_Retirement")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
//@EntityListeners(AuditingEntityListener.class)
public class Retirement implements Approvable {

    public static final String APPROVAL_REF_PREFIX = "Retirement:";
    public static final String APPROVAL_TYPE = "Retirement";
    public static final String APPROVAL_CRITERIA = "RetirementApprovalCriteria";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @CreationTimestamp
    private LocalDateTime timeCreated;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    //@CreatedBy
    @NotNull
    @ManyToOne
    private User creator;

    @ManyToOne
    @NotNull
    private Expense expense;

    @NotEmpty
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<RetirementEntry> entries;

    public Retirement(Expense expense) {
        this.expense = expense;
        this.entries = new LinkedHashSet<>();
    }

    public Retirement(Expense expense, User creator) {
        this(expense);
        this.creator = creator;
    }

    public static Long getId(String approvalReference) {
        String idString = approvalReference.toLowerCase().replace(APPROVAL_REF_PREFIX.toLowerCase(), "");
        return Long.parseLong(idString);
    }

    @Override
    public String getApprovalReference() {
        return APPROVAL_REF_PREFIX + id;
    }

    @Override
    public String getApprovalType() {
        return APPROVAL_TYPE;
    }

    public String toString() {
        return String.format("Retirement of %s for %s by %s", expense.getPaidAmount(), expense.getResource(), expense.getRole());
    }

    public Budget getBudget() {
        return getExpense().getBudget();
    }

    public Role getRole() {
        return getExpense().getRole();
    }

    public Resource getResource() {
        return getExpense().getResource();
    }

    public Consumer getConsumer() {
        return getExpense().getConsumer();
    }

    public Money getTotal() {
        return this.getEntries()
                .stream()
                .map(RetirementEntry::getAmount)
                .reduce(Money.getZERO(), Money::plus);
    }

    public Set<RetirementEntry> getAcceptedEntries() {
        return getEntries().stream().filter(entry -> entry.isAccepted()).collect(Collectors.toSet());
    }

    public Set<RetirementEntry> getRejectedEntries() {
        return getEntries().stream().filter(entry -> !entry.isAccepted()).collect(Collectors.toSet());
    }

    public Money getTotalAccepted() {
        return getAcceptedEntries()
                .stream()
                .map(RetirementEntry::getAmount)
                .reduce(Money.getZERO(), Money::plus);
    }

    public BigDecimal calculateEntryPercentage(RetirementEntry entry) {
        return entry.getAmount().percentageOf(getTotal(), 2, RoundingMode.HALF_EVEN);
    }
}
