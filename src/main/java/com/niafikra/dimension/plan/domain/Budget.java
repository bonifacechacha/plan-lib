package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.approval.domain.Approvable;
import com.niafikra.dimension.attachment.domain.Attachment;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.util.HasName;
import com.niafikra.dimension.money.Money;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/2/17 2:25 PM
 */

@Entity
@Table(name = "PL_Budget")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "title", "startDate", "endDate"})
public class Budget implements Approvable, HasName {
    public static final String APPROVAL_REF_PREFIX = "Budget:";
    public static final String APPROVAL_TYPE = "Budget";
    public static final String APPROVAL_CRITERIA = "BudgetApprovalCriteria";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;


    @CreationTimestamp
    private LocalDateTime timeCreated;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @NotBlank
    @NotNull
    @Column(unique = true)
    private String title;

    @Lob
    private String description;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @ManyToOne
    @NotNull
    private CostCenter costCenter;

    @ManyToOne
    @NotNull
    private User creator;

    @OneToMany(mappedBy = "budget", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Allocation> allocations;

    @Column(nullable = true, columnDefinition = "TINYINT(1) default NULL")
    private Boolean approved;

    private boolean archived;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "fund_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "fund_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "fund_currency"))
    })
    private Money fund = Money.getZERO();

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "cost_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "cost_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "cost_currency"))
    })
    private Money cost = Money.getZERO();

    private boolean allowExpensesWithLessBalance;

    private boolean allowRequestWithLessBalance;

    private boolean allowRequestWithLessGrossBalance;

    private boolean allowRequestWithSimilarPending;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attachment> attachments = new LinkedHashSet<>();

    public static Long getId(String approvalReference) {
        String idString = approvalReference.toLowerCase().replace(APPROVAL_TYPE.toLowerCase() + ":", "");
        return Long.parseLong(idString);
    }

    public static String generateAllocationsImportUrl(Budget budget) {
        return String.format("/budget/%d/allocation/format", budget.getId());
    }

    public String toString() {
        return getTitle()
                + " "
                + getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " | "
                + getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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

    public boolean isApproved() {
        return approved == Boolean.TRUE;
    }

    public boolean isDeclined() {
        return approved == Boolean.FALSE;
    }

    public boolean isWaiting() {
        return approved == null;
    }

    public boolean isActive() {
        return isApproved() && isUpToDate() && !isArchived();
    }

    public boolean isUpToDate() {
        return isUpToDate(LocalDate.now());
    }

    public boolean isUpToDate(LocalDate date) {
        return !getStartDate().isAfter(date) && !getEndDate().isBefore(date);
    }

    @Override
    public String getName() {
        return title;
    }

    @PrePersist
    public void validate() {
        if (getStartDate().isAfter(getEndDate()))
            throw new IllegalArgumentException("The budget end date should be after the start date :" + getStartDate() + " > " + getEndDate());
    }
}