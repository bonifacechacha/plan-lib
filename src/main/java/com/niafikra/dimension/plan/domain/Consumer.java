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
 * @date 8/31/17 9:42 AM
 */
@Entity
@Table(name = "PL_Consumer")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "name"})
public class Consumer implements Serializable, HasName, HasCategory {

    public static final String CATEGORY_TYPE = "Consumer";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @Column(unique = true)
    private String code;

    @NotBlank
    @NotNull
    //TODO THE NAMES FOR CONSUMERS ARE VERY LIKELY TO BE THE SAME SO IT IS BEST NOT TO BE UNIQUE
    //EG PIVOTECH SITE NAME, CAR MAKE
//    @Column(unique = true)
    private String name;

    @Lob
    private String description;

    @ManyToOne
    @NotNull
    private Category category;

    //if this consumer is still active or not user anymore
    //eg if a car is still being used or not
    private boolean active;

    public Consumer(Category category) {
        setCategory(category);
    }

    public void setCategory(Category category) {
        if (category != null && !category.isType(CATEGORY_TYPE))
            throw new IllegalArgumentException("Category must be of type " + CATEGORY_TYPE);
        this.category = category;
    }

    @Override
    public String toString() {
        if (code == null) return getName();
        return getCode() + " : " + getName();
    }
}
