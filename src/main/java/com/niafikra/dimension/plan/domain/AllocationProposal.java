package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.money.Money;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(of = {"budget", "role", "resource"})
public class AllocationProposal {

    private Budget budget;

    private Role role;

    private Resource resource;

    private Money proposedAmount;

    private String description;

    private String reason;

    public AllocationProposal(Budget budget) {
        this.budget = budget;
        this.proposedAmount = Money.getZERO();
    }

    public AllocationProposal(Allocation allocation) {
        this.budget = allocation.getBudget();
        this.role = allocation.getRole();
        this.resource = allocation.getResource();
        this.proposedAmount = allocation.getProposedAmount();
        this.description = allocation.getDescription();
        this.reason = "";
    }

    public AllocationProposal(Budget budget, Role role, Resource resource, Money amount) {
        this.budget = budget;
        this.role = role;
        this.resource = resource;
        this.proposedAmount = amount;
        this.description = "";
        this.reason = "";
    }
}
