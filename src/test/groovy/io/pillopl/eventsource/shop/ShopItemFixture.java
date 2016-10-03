package io.pillopl.eventsource.shop;


import com.google.common.collect.ImmutableList;
import io.pillopl.eventsource.shop.domain.ShopItem;
import io.pillopl.eventsource.shop.domain.ShopItemState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;

public class ShopItemFixture {

    public static final Instant ANY_TIME = now();
    public static final int ANY_NUMBER_OF_HOURS_TO_PAYMENT_TIMEOUT = 48;
    public static final BigDecimal ANY_PRICE = BigDecimal.TEN;


    public static ShopItem initialized() {
        return new ShopItem(null, ImmutableList.of(), ShopItemState.INITIALIZED);
    }

    public static ShopItem bought(UUID uuid) {
        return initialized()
                .buy(uuid, ANY_TIME, ANY_NUMBER_OF_HOURS_TO_PAYMENT_TIMEOUT, ANY_PRICE)
                .markChangesAsCommitted();
    }

    public static ShopItem paid(UUID uuid) {
        return initialized()
                .buy(uuid, now(), ANY_NUMBER_OF_HOURS_TO_PAYMENT_TIMEOUT, ANY_PRICE)
                .pay(now())
                .markChangesAsCommitted();
    }

    public static ShopItem withTimeout(UUID uuid) {
        return initialized()
                .buy(uuid, now(), ANY_NUMBER_OF_HOURS_TO_PAYMENT_TIMEOUT, ANY_PRICE)
                .markTimeout(now())
                .markChangesAsCommitted();
    }

    public static ShopItem withTimeoutAndPaid(UUID uuid) {
        return initialized()
                .buy(uuid, now(), ANY_NUMBER_OF_HOURS_TO_PAYMENT_TIMEOUT, ANY_PRICE)
                .markTimeout(now())
                .pay(now())
                .markChangesAsCommitted();
    }

}
