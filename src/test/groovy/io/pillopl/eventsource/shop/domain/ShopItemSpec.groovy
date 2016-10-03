package io.pillopl.eventsource.shop.domain

import io.pillopl.eventsource.shop.domain.events.ItemPaid
import io.pillopl.eventsource.shop.domain.events.ItemBought
import io.pillopl.eventsource.shop.domain.events.ItemPaymentTimeout
import spock.lang.Specification
import spock.lang.Unroll

import static io.pillopl.eventsource.shop.ShopItemFixture.bought
import static io.pillopl.eventsource.shop.ShopItemFixture.initialized
import static io.pillopl.eventsource.shop.ShopItemFixture.paid
import static io.pillopl.eventsource.shop.ShopItemFixture.withTimeout
import static io.pillopl.eventsource.shop.ShopItemFixture.withTimeoutAndPaid
import static java.time.Instant.now
import static java.time.Instant.parse

@Unroll
class ShopItemSpec extends Specification {

    private static final int PAYMENT_DEADLINE_IN_HOURS = 48
    private static final BigDecimal ANY_PRICE = BigDecimal.TEN
    private final UUID uuid = UUID.randomUUID()

    def 'should emit item bought event when buying initialized item'() {
        when:
            ShopItem item = initialized().buy(uuid, now(), PAYMENT_DEADLINE_IN_HOURS, ANY_PRICE)
        then:
            item.getUncommittedChanges().size() == 1
            item.getUncommittedChanges().head().type() == ItemBought.TYPE
    }

    def 'should calculate #deadline when buying at #buyingAt and expiration in days #expiresIn'() {
        when:
            ShopItem item = initialized().buy(uuid, parse(buyingAt), expiresIn, ANY_PRICE)
        then:
            ((ItemBought) item.getUncommittedChanges().head()).paymentTimeoutDate == parse(deadline)
        where:
            buyingAt               | expiresIn || deadline
            "1995-10-23T10:12:35Z" | 0         || "1995-10-23T10:12:35Z"
            "1995-10-23T10:12:35Z" | 1         || "1995-10-23T11:12:35Z"
            "1995-10-23T10:12:35Z" | 2         || "1995-10-23T12:12:35Z"
            "1995-10-23T10:12:35Z" | 20        || "1995-10-24T06:12:35Z"
            "1995-10-23T10:12:35Z" | 24        || "1995-10-24T10:12:35Z"
            "1995-10-23T10:12:35Z" | 48        || "1995-10-25T10:12:35Z"
    }

    def 'Payment expiration date cannot be in the past'() {
        given:
            ShopItem item = initialized()
        when:
            item.buy(uuid, now(), -1, ANY_PRICE)
        then:
            Exception e = thrown(IllegalArgumentException)
            e.message.contains("Payment timeout day is before buying date")
    }

    def 'buying an item should be idempotent'() {
        given:
            ShopItem item = bought(uuid)
        when:
            item.buy(uuid, now(), PAYMENT_DEADLINE_IN_HOURS, ANY_PRICE)
        then:
            item.getUncommittedChanges().isEmpty()
    }

    def 'cannot pay for just initialized item'() {
        given:
            ShopItem item = initialized()
        when:
            item.pay(now())
        then:
            thrown(IllegalStateException)
    }

    def 'cannot mark payment timeout when item just initialized'() {
        given:
            ShopItem item = initialized()
        when:
            item.markTimeout(now())
        then:
            thrown(IllegalStateException)
    }

    def 'should emit item paid event when paying for bought item'() {
        when:
            ShopItem item = bought(uuid).pay(now())
        then:
            item.getUncommittedChanges().size() == 1
            item.getUncommittedChanges().head().type() == ItemPaid.TYPE
    }

    def 'paying for an item should be idempotent'() {
        given:
            ShopItem item = paid(uuid)
        when:
            item.pay(now())
        then:
            item.getUncommittedChanges().isEmpty()
    }

    def 'should emit payment timeout event when marking item as payment missing'() {
        when:
            ShopItem item = bought(uuid).markTimeout(now())
        then:
            item.getUncommittedChanges().size() == 1
            item.getUncommittedChanges().head().type() == ItemPaymentTimeout.TYPE
    }

    def 'marking payment timeout should be idempotent'() {
        when:
            ShopItem item = withTimeout(uuid).markTimeout(now())
        then:
            item.getUncommittedChanges().isEmpty()
    }

    def 'cannot mark payment missing when item already paid'() {
        when:
            paid(uuid).markTimeout(now())
        then:
            thrown(IllegalStateException)
    }

    def 'should emit item paid event when receiving missed payment'() {
        when:
            ShopItem item = withTimeout(uuid).pay(now())
        then:
            item.getUncommittedChanges().size() == 1
            item.getUncommittedChanges().head().type() == ItemPaid.TYPE

    }

    def 'receiving payment after timeout should be idempotent'() {
        when:
            ShopItem item = withTimeoutAndPaid(uuid).pay(now())
        then:
            item.getUncommittedChanges().isEmpty()
    }

}
