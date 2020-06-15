package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.core.util.HasName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/26/17 11:57 AM
 */
@Entity
@Table(name = "PL_PaymentMethod")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class PaymentMethod implements HasName, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @NotBlank
    @Column(unique = true)
    private String name;

    @Lob
    private String description;

    public String toString() {
        return name;
    }
}
