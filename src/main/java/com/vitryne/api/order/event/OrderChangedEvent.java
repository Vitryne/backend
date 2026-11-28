package com.vitryne.api.order.event;

import com.vitryne.api.order.dto.OrderResponseDTO;

public record OrderChangedEvent(Type type, OrderResponseDTO order) {

    public enum Type {
        ORDER_CREATED,
        ORDER_UPDATED;

        public String eventName() {
            return name().toLowerCase();
        }
    }

    public Long storeId() {
        return order.storeId();
    }
}
