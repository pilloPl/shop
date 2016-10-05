package io.pillopl.eventsource.shop.domain

import io.pillopl.eventsource.shop.domain.events.ItemPaid
import io.pillopl.eventsource.shop.domain.events.ItemOrdered
import io.pillopl.eventsource.shop.domain.events.ItemPaymentTimeout
import spock.lang.Specification
import spock.lang.Unroll

import static io.pillopl.eventsource.shop.ShopItemFixture.ordered
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

    def 'should emit item ordered event when ordering initialized item'() {
        when:
            ShopItem item = initialized().order(uuid, now(), PAYMENT_DEADLINE_IN_HOURS, ANY_PRICE)
        then:
            item.getUncommittedChanges().size() == 1
            item.getUncommittedChanges().head().type() == ItemOrdered.TYPE
    }

    def 'should calculate #deadline when ordering at #orderingAt and expiration in days #expiresIn'() {
        when:
            ShopItem item = initialized().order(uuid, parse(orderingAt), expiresInMinutes, ANY_PRICE)
        then:
            ((ItemOrdered) item.getUncommittedChanges().head()).paymentTimeoutDate == parse(deadline)
        where:
            orderingAt             | expiresInMinutes || deadline
            "1995-10-23T10:12:35Z" | 0                || "1995-10-23T10:12:35Z"
            "1995-10-23T10:12:35Z" | 1                || "1995-10-23T10:13:35Z"
            "1995-10-23T10:12:35Z" | 2                || "1995-10-23T10:14:35Z"
            "1995-10-23T10:12:35Z" | 20               || "1995-10-23T10:32:35Z"
            "1995-10-23T10:12:35Z" | 24               || "1995-10-23T10:36:35Z"
    }

    def 'Payment expiration date cannot be in the past'() {
        given:
            ShopItem item = initialized()
        when:
            item.order(uuid, now(), -1, ANY_PRICE)
        then:
            Exception e = thrown(IllegalArgumentException)
            e.message.contains("Payment timeout day is before ordering date")
    }

    def 'ordering an item should be idempotent'() {
        given:
            ShopItem item = ordered(uuid)
        when:
            item.order(uuid, now(), PAYMENT_DEADLINE_IN_HOURS, ANY_PRICE)
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

    def 'should emit item paid event when paying for ordered item'() {
        when:
            ShopItem item = ordered(uuid).pay(now())
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
            ShopItem item = ordered(uuid).markTimeout(now())
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
