package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.core.security.domain.User;
import com.niafikra.dimension.core.util.HasName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/31/17 2:43 PM
 */
@Entity
@Table(name = "PL_CostCenter")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "name"})
public class CostCenter implements HasName, Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @NotBlank
    @NotNull
    @Column(unique = true)
    private String name;

    @Lob
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<Role> roles = new LinkedHashSet<>();


    @ManyToMany(fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<Resource> resources = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<User> planners = new LinkedHashSet<>();

    @Override
    public String toString() {
        return name;
    }

}
