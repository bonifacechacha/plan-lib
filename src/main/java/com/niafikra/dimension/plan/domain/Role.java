package com.niafikra.dimension.plan.domain;

import com.niafikra.dimension.group.domain.Group;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 8/30/17 10:40 PM
 */
@Entity
@Table(name = "PL_Role")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"resources"})
public class Role extends Group {

    @ManyToMany(fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<Resource> resources = new LinkedHashSet<>();

    @Override
    public String toString() {
        return getName();
    }
}
