package com.niafikra.dimension.plan.event;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

/**
 * @author Boniface Chacha
 * @email boniface.chacha@niafikra.com
 * @email bonifacechacha@gmail.com
 * @date 9/6/17 2:58 PM
 */
public class DeleteEvent<T> implements ResolvableTypeProvider {

    private T entity;

    public DeleteEvent(T entity) {
        this.entity = entity;
    }

    public T getEntity() {
        return entity;
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(getClass(),
                ResolvableType.forInstance(entity));
    }
}
