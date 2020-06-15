package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.category.domain.Category;
import com.niafikra.dimension.category.domain.HasCategory;
import com.niafikra.dimension.core.util.HasName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/29/17 11:30 PM
 */
@Entity
@Table(name = "PL_Resource")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "name"})
public class Resource implements Serializable, HasName, HasCategory {
    public static final String CATEGORY_TYPE = "Resource";

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

    @NotNull
    @ManyToOne
    private Category category;

    public Resource(Category category) {
        setCategory(category);
    }

    public void setCategory(Category category) {
        if (category != null && !category.isType(CATEGORY_TYPE))
            throw new IllegalArgumentException("Resource category must be of type " + CATEGORY_TYPE);
        this.category = category;
    }

    @Override
    public String toString() {
        return getName();
    }
}
