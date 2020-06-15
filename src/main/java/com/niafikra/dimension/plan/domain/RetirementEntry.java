package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.attachment.domain.Attachment;
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
import java.util.HashSet;
import java.util.Set;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/30/17 10:06 PM
 */
@Entity
@Table(name = "PL_RetirementEntry")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "timeCreated"})
public class RetirementEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @CreationTimestamp
    private LocalDateTime timeCreated;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    //The actual amount spent for the expense recorded
    @NotNull
    @Embedded
    private Money amount;

    @Lob
    @NotNull
    @NotBlank
    private String description;

    private boolean accepted;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attachment> attachments;

    public RetirementEntry(Money amount) {
        this.amount = amount;
        this.attachments = new HashSet<>();
    }

    public RetirementEntry(Money amount, String description, Set<Attachment> attachments) {
        this.amount = amount;
        this.description = description;
        this.attachments = attachments;
    }

    public String toString() {
        return String.format("Retired %s : %s", amount, description);
    }
}
