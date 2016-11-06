package io.pillopl.eventsource.shop.domain;

import com.google.common.collect.ImmutableList;
import io.pillopl.eventsource.shop.domain.events.DomainEvent;
import io.pillopl.eventsource.shop.domain.events.ItemOrdered;
import io.pillopl.eventsource.shop.domain.events.ItemPaid;
import io.pillopl.eventsource.shop.domain.events.ItemPaymentTimeout;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
@Wither
public class ShopItem {

    private final UUID uuid;
    private final ImmutableList<DomainEvent> changes;
    private final ShopItemState state;

    public ShopItem pay(Instant when) {
        throwIfStateIs(ShopItemState.INITIALIZED, "Cannot pay for not ordered item");
        if (state != ShopItemState.PAID) {
            return applyChange(new ItemPaid(uuid, when));
        } else {
            return this;
        }
    }

    private ShopItem apply(ItemPaid event) {
        return this.withState(ShopItemState.PAID);
    }

    public ShopItem order(UUID uuid, Instant when, int minutesToPaymentTimeout, BigDecimal price) {
        if (state == ShopItemState.INITIALIZED) {
            return applyChange(new ItemOrdered(uuid, when, calculatePaymentTimeoutDate(when, minutesToPaymentTimeout), price));
        } else {
            return this;
        }
    }

    private Instant calculatePaymentTimeoutDate(Instant boughtAt, int hoursToPaymentTimeout) {
        final Instant paymentTimeout = boughtAt.plus(hoursToPaymentTimeout, ChronoUnit.MINUTES);
        if (paymentTimeout.isBefore(boughtAt)) {
            throw new IllegalArgumentException("Payment timeout day is before ordering date!");
        }
        return paymentTimeout;
    }

    public ShopItem markTimeout(Instant when) {
        throwIfStateIs(ShopItemState.INITIALIZED, "Payment is not missing yet");
        throwIfStateIs(ShopItemState.PAID, "Item already paid");
        if (state == ShopItemState.ORDERED) {
            return applyChange(new ItemPaymentTimeout(uuid, when));
        } else {
            return this;
        }
    }

    private void throwIfStateIs(ShopItemState unexpectedState, String msg) {
        if (state == unexpectedState) {
            throw new IllegalStateException(msg + (" UUID: " + uuid));
        }
    }

    public static ShopItem from(UUID uuid, List<DomainEvent> history) {
        return history
                .stream()
                .reduce(
                        new ShopItem(uuid, ImmutableList.of(), ShopItemState.INITIALIZED),
                        (tx, event) -> tx.applyChange(event, false),
                        (t1, t2) -> {throw new UnsupportedOperationException();}
                );
    }

    private ShopItem apply(ItemOrdered event) {
        return this
                .withUuid(event.getUuid())
                .withState(ShopItemState.ORDERED);
    }

    private ShopItem apply(ItemPaymentTimeout event) {
        return this.withState(ShopItemState.PAYMENT_MISSING);
    }

    private ShopItem applyChange(DomainEvent event, boolean isNew) {
        final ShopItem item = this.apply(event);
        if (isNew) {
            return new ShopItem(item.getUuid(), appendChange(item, event), item.getState());
        } else {
            return item;
        }
    }

    private ImmutableList<DomainEvent> appendChange(ShopItem item, DomainEvent event) {
        return ImmutableList
                .<DomainEvent>builder()
                .addAll(item.getChanges())
                .add(event)
                .build();
    }

    private ShopItem apply(DomainEvent event) {
        if (event instanceof ItemPaid) {
            return this.apply((ItemPaid) event);
        } else if (event instanceof ItemOrdered) {
            return this.apply((ItemOrdered) event);
        } else if (event instanceof ItemPaymentTimeout) {
            return this.apply((ItemPaymentTimeout) event);
        } else {
            throw new IllegalArgumentException("Cannot handle event " + event.getClass());
        }
    }

    private ShopItem applyChange(DomainEvent event) {
        return applyChange(event, true);
    }

    public ImmutableList<DomainEvent> getUncommittedChanges() {
        return changes;
    }

    public ShopItem markChangesAsCommitted() {
        return this.withChanges(ImmutableList.of());
    }

}
