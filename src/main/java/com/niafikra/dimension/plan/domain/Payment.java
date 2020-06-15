package com.niafikra.dimension.plan.domain;

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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A receipt or payment made to or by the organisation
 *
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/30/17 10:25 PM
 */
@Entity
@Table(name = "PL_Payment")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Payment {

    public static final String ON_PAYMENT_EXTRA_NOTE = "payment_extra_note";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @NotNull
    @ManyToOne
    private PaymentMethod method;

    @CreationTimestamp
    private LocalDateTime timeCreated;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    //The actual time the payment was effected
    @NotNull
    @Column(name = "payment_time")
    private LocalDateTime time;

    @NotNull
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "payment_amount", precision = 48, scale = 12)),
            @AttributeOverride(name = "date", column = @Column(name = "payment_date")),
            @AttributeOverride(name = "currency", column = @Column(name = "payment_currency"))
    })
    private Money amount;

    private String reference;

    //The user who recorded this settlement
    @ManyToOne
    @NotNull
    private User creator;

    //The name of payer of the settlement incase it is a receipt or payee incase it is a payment
    @NotBlank
    @NotNull
    private String associate;

    @Lob
    @NotNull
    @NotBlank
    private String description;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Attachment> attachments;

    @ElementCollection
    private Map<String, String> notes;

    //is this settlement a payment? otherwise it is a receipt
    private boolean paid;

    public Payment(String associate, Money amount, boolean paid) {
        this.amount = amount;
        this.paid = paid;
        this.time = LocalDateTime.now();
        this.description = "";
        this.reference = "";
        this.associate = associate;
        this.notes = new HashMap<>();
        this.attachments = new LinkedHashSet<>();
    }

    public Payment(Money amount, boolean paid) {
        this("", amount, paid);
    }

    public boolean isReceived() {
        return !paid;
    }

    public String getNote(String noteKey) {
        if (notes == null) return null;
        return notes.get(noteKey);
    }

    public void putNote(String noteKey, String note) {
        notes.put(noteKey, note);
    }
}
