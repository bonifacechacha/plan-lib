package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.approval.domain.Approvable;
import com.niafikra.dimension.attachment.domain.Attachment;
import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.money.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @Author Juma mketto
 * @Date 1/2/19.
 */


@Entity
@Table(name = "PL_AllocationAdjustment")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "resource"})
public class AllocationAdjustment implements Approvable {
    public static final String APPROVAL_TYPE = "AllocationAdjustment";
    public static final String APPROVAL_REF_PREFIX = "AllocationAdjustment:";
    public static final String APPROVAL_CRITERIA = "AllocationAdjustmentApprovalCriteria";

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

    @ManyToOne
    @NotNull
    private Resource resource;

    @ManyToOne
    @NotNull
    private Role role;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "proposed_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "proposed_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "proposed_currency"))
    })
    private Money proposedAmount = Money.getZERO();

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "approved_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "approved_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "approved_currency"))
    })
    private Money allocatedAmount = Money.getZERO();

    @Lob
    @NotNull
    @NotBlank
    private String reason;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attachment> attachments = new LinkedHashSet<>();

    private Boolean approved;

    public AllocationAdjustment(Budget budget) {
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

    public String toString() {
        return "Allocation adjustment for " + budget;
    }


}
