package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.approval.domain.Approvable;
import com.niafikra.dimension.core.security.domain.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PL_PeriodAdjustment")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class PeriodAdjustment implements Approvable {
    public static final String APPROVAL_TYPE = "PeriodAdjustment";
    public static final String APPROVAL_REF_PREFIX = "PeriodAdjustment:";
    public static final String APPROVAL_CRITERIA = "PeriodAdjustmentApprovalCriteria";

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
    private User creator;

    @ManyToOne
    @NotNull
    private Budget budget;

    @NotNull
    private LocalDate proposedEndDate;

    @Lob
    @NotNull
    @NotBlank
    private String reason;

    private Boolean approved;

    public PeriodAdjustment(Budget budget) {
        this.budget = budget;
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

    public LocalDate getCurrentEndDate() {
        return getBudget().getEndDate();
    }

    public String toString() {
        return "End date adjustment to " + proposedEndDate + " for " + budget;
    }
}
