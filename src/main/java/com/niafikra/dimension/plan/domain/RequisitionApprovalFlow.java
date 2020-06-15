package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.category.domain.Category;
import com.niafikra.dimension.core.util.HasName;
import com.niafikra.dimension.group.domain.Group;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "PL_RequisitionApprovalFlow")
@Table(name = "PL_RequisitionApprovalFlow")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "costCenter", "role", "resource"})

//todo rename this class to just approval flow
public class RequisitionApprovalFlow implements Serializable, Comparable<RequisitionApprovalFlow> {

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
    private CostCenter costCenter;

    @ManyToOne
    private Role role;

    @ManyToOne
    private Resource resource;

    @ManyToOne
    private Category category;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Group> levels;

    @Override
    public int compareTo(RequisitionApprovalFlow other) {
        int result = compare(costCenter, other.costCenter);
        if (result == 0) result = compare(role, other.role);
        if (result == 0) result = compare(resource, other.resource);
        if (result == 0) result = compare(category, other.category);
        return result;
    }

    private int compare(HasName n1, HasName n2) {
        if (n1 == null) return -1;
        if (n2 == null) return 1;
        else return n1.compareTo(n2);
    }
}
