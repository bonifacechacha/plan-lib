package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.consumer.domain.Consumer;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.money.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/2/17 8:49 PM
 */
@Entity
@Table(name = "PL_Expense")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

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
    private User associatedUser;

    @ManyToOne
//    @NotNull
    private Consumer consumer;

    @NotNull
    @Valid
    @ManyToOne
    private Payment payment;

    /**
     * Approved retirement request entries only
     */
    @OneToMany(fetch = FetchType.EAGER)
    private Set<RetirementEntry> retirementEntries;

    @OneToMany(fetch = FetchType.EAGER)
    private Set<Payment> settlements;

    /**
     * Once all requested confirms that he is done reconciling and all the settlements are done mark reconciled to lock further retirements
     */
    private boolean reconciled;

    public Expense(Budget budget) {
        this.budget = budget;
        this.payment = new Payment(Money.getZERO(), true);
    }

    public Expense(Budget budget,
                   Role role,
                   Resource resource,
                   User associatedUser,
                   Consumer consumer) {
        this.budget = budget;
        this.role = role;
        this.resource = resource;
        this.associatedUser = associatedUser;
        this.consumer = consumer;
    }

    public void setPayment(Payment settlement) {
        if (!settlement.isPaid()) throw new IllegalArgumentException("The settlement for expense must be a payment");
        this.payment = settlement;
    }

    public void retire(RetirementEntry entry) {
        if (!entry.isAccepted())
            throw new IllegalArgumentException("Retirement must be accepted to be associated with the expense");

        if (hasSettlements())
            throw new IllegalArgumentException("Retirements are not allowed once the settlement started");

        this.retirementEntries.add(entry);
    }

    public LocalDateTime getPaymentTime() {
        return payment.getTime();
    }

    @NotNull
    public PaymentMethod getPaymentMethod() {
        return payment.getMethod();
    }

    public String getPaymentReference() {
        return payment.getReference();
    }

    public String getNotes() {
        return payment.getDescription();
    }

    public LocalDateTime getTimeCreated() {
        return payment.getTimeCreated();
    }

    public Money getPaidAmount() {
        return getPayment().getAmount();
    }

    public Money getActualAmount() {
//        if (!hasRetirements()) return getPaidAmount();
//        else return getTotalRetirement();

        //until the expense is reconciled the amount remains to be the actual payment amount
        //when reconciled the actual amount is the sum of all the retirement entries
        if (isReconciled() && hasRetirements()) return getTotalRetirement();
        else return getPaidAmount();
    }

    public boolean hasRetirements() {
        return !CollectionUtils.isEmpty(getRetirementEntries());
    }

    public String toString() {
        return new StringBuilder()
                .append(getActualAmount())
                .append(" for ")
                .append(resource)
                .append(" by ")
                .append(role)
                .toString();
    }

    public boolean hasSettlements() {
        return !CollectionUtils.isEmpty(getSettlements());
    }

    public boolean requiresPayment() {
        return getPaidAmount().isLessThan(getTotalRetirement());
    }

    public boolean isCompletelySettled() {
        return getTotalSettlement().equals(getRetiredDifference().absolute());
    }

    public Money getTotalSettlement() {
        return getSettlements()
                .stream()
                .map(payment -> payment.getAmount())
                .reduce(Money.getZERO(), (m1, m2) -> m1.plus(m2));
    }

    public Money getTotalRetirement() {
        return this.getRetirementEntries()
                .stream()
                .map(retirement -> retirement.getAmount())
                .reduce(Money.getZERO(), (m1, m2) -> m1.plus(m2));
    }

    public Money getRetiredDifference() {
        return getPaidAmount().minus(getTotalRetirement());
    }

    public Money getPendingSettlement() {
        return getRetiredDifference().absolute().minus(getTotalSettlement());
    }

    public void settle(Payment payment) {

        if (requiresPayment() != payment.isPaid())
            throw new IllegalArgumentException("Settlement for the retirement should be a " + (requiresPayment() ? "payment" : "receipt"));

        if (settlements == null)
            settlements = new LinkedHashSet<>();

        settlements.add(payment);
    }

    public void cancelRetirements(Collection<RetirementEntry> cancelledEntries) {
        if (hasSettlements())
            throw new IllegalStateException("You can not cancel retirement once there are settlements associated with the expense :" + getTotalSettlement());

        setRetirementEntries(
                this.getRetirementEntries()
                        .stream()
                        //filter to remain with those which are not on the cancelled request
                        .filter(retirement -> !cancelledEntries.contains(retirement))
                        .collect(Collectors.toSet())
        );

    }

    public void addRetirements(List<RetirementEntry> entries) {
        if (this.retirementEntries == null)
            this.retirementEntries = new LinkedHashSet<>();

        this.retirementEntries.addAll(entries);
    }

    public boolean canSettle() {
        return !isReconciled() && !isCompletelySettled() && hasRetirements();
    }

    public boolean canRetire() {
        return !isReconciled() && !hasSettlements();
    }

    public boolean canReconcile() {
        return !isReconciled() && getRetiredDifference().isZero() && isCompletelySettled();
    }
}
