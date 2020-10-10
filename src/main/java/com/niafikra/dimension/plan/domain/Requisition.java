package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.approval.domain.Approvable;
import com.niafikra.dimension.attachment.domain.Attachment;
import com.niafikra.dimension.consumer.domain.Consumer;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.money.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/30/17 10:06 PM
 */
@Entity
@Table(name = "PL_Requisition")
@Setter
@Getter
@EqualsAndHashCode(of = {"id"})
public class Requisition implements Approvable {

    public static final String APPROVAL_REF_PREFIX = "Requisition:";
    public static final String APPROVAL_TYPE = "Requisition";
    public static final String APPROVAL_CRITERIA = "RequisitionApprovalCriteria";

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
//    @NotNull
    private Consumer consumer;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "requested_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "requested_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "requested_currency"))
    })
    private Money requestedAmount;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "approved_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "approved_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "approved_currency"))
    })
    private Money approvedAmount;

    @Lob
    @NotNull
    @NotBlank
    private String description;

    @ManyToOne
    @NotNull
    private User creator;

    /**
     * A single requisition can be fulfilled through multiple payment to the one requesting
     * For that case more than one expenses will be recorded
     */
    @OneToMany(fetch = FetchType.EAGER)
    private Set<Expense> expenses;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attachment> attachments;

    @Column(nullable = true, columnDefinition = "TINYINT(1) default NULL")
    private Boolean approved;

    /**
     * This flagg is added to simplify searching of requisitions which have not been completely paid from the database
     * If a query to easily fetch complely and/or not completely paid requisitions is found then it shall be remove
     */
    private boolean fulfilled;

    public Requisition() {
        this.requestedAmount = Money.getZERO();
        this.approvedAmount = Money.getZERO();
        this.timeCreated = LocalDateTime.now();
        this.attachments = new LinkedHashSet<>();
        this.expenses = new LinkedHashSet<>();
    }

    public Requisition(Budget budget) {
        this();
        this.budget = budget;
    }

    public Requisition(Budget budget,
                       Role role,
                       Resource resource,
                       Consumer consumer,
                       User creator,
                       Money requestedAmount,
                       String description,
                       Set<Attachment> attachments) {

        this(budget);

        this.role = role;
        this.resource = resource;
        this.consumer = consumer;
        this.requestedAmount = requestedAmount;
        this.description = description;
        this.creator = creator;
        this.attachments = attachments;
    }

    public static Long getId(String approvalReference) {
        String idString = approvalReference.toLowerCase().replace(APPROVAL_TYPE.toLowerCase() + ":", "");
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

    @Override
    public String getApprovalDescription() {
        return toString();
    }

    public boolean isLocked() {
        return false;
    }

    public boolean isApproved() {
        return approved == Boolean.TRUE;
    }

    public boolean isDeclined() {
        return approved == Boolean.FALSE;
    }

    public boolean isWaiting() {
        return approved == null;
    }

    public CostCenter getCostCenter() {
        return getBudget().getCostCenter();
    }

    public String toString() {
        return String.format("%s of %s for %s by %s", approvedAmount, requestedAmount, resource, role);
    }

    /**
     * It has been paid and generated some expenses
     *
     * @return
     */
    public boolean isPaymentProcessed() {
        return !getExpenses().isEmpty();
    }

    public Money getPaidAmount() {
        return expenses.stream()
                .map(Expense::getPaidAmount)
                .reduce(Money.getZERO(), Money::plus);
    }

    public Money getPendingAmount() {
        return approvedAmount.minus(getPaidAmount());
    }

    public Set<RetirementEntry> getRetirements() {
        return expenses.stream()
                .filter(Expense::hasRetirements)
                .flatMap(expense -> expense.getRetirementEntries().stream())
                .collect(Collectors.toSet());
    }

    public boolean isCompletelyPaid() {
        return getPendingAmount().isZero();
    }

    public void addExpense(Expense expense) {
        if (expenses == null)
            expenses = new HashSet<>();

        expenses.add(expense);
    }

    public boolean isReconciled() {
        return getExpenses().stream().allMatch(Expense::isReconciled);
    }

    public List<Expense> getPendingRetirementExpenses() {
        return getExpenses()
                .stream()
                .filter(expense -> expense.canRetire())
                .collect(Collectors.toList());
    }
}
