package io.pillopl.eventsource.shop.boundary;

import io.pillopl.eventsource.shop.domain.ShopItem;
import io.pillopl.eventsource.shop.domain.ShopItemRepository;
import io.pillopl.eventsource.shop.domain.commands.Order;
import io.pillopl.eventsource.shop.domain.commands.MarkPaymentTimeout;
import io.pillopl.eventsource.shop.domain.commands.Pay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.UnaryOperator;

@Service
@Transactional
@Slf4j
public class ShopItems {

    private final ShopItemRepository itemRepository;
    private final int hoursToPaymentTimeout;

    @Autowired
    public ShopItems(ShopItemRepository itemRepository, @Value("${minutes.to.payment.timeout:1}") int hoursToPaymentTimeout) {
        this.itemRepository = itemRepository;
        this.hoursToPaymentTimeout = hoursToPaymentTimeout;
    }

    public void order(Order command) {
        withItem(command.getUuid(), item ->
                item.order(command.getUuid(), command.getWhen(), hoursToPaymentTimeout, command.getPrice())
        );
        log.info("{} item ordered at {}", command.getUuid(), command.getWhen());
    }

    public void pay(Pay command) {
        withItem(command.getUuid(), item ->
                        item.pay(command.getWhen())
        );
        log.info("{} item paid at {}", command.getUuid(), command.getWhen());
    }

    public void markPaymentTimeout(MarkPaymentTimeout command) {
        withItem(command.getUuid(), item ->
                        item.markTimeout(command.getWhen())
        );
        log.info("{} item marked as payment timeout at {}", command.getUuid(), command.getWhen());
    }

    public ShopItem getByUUID(UUID uuid) {
        return itemRepository.getByUUID(uuid);
    }

    private ShopItem withItem(UUID uuid, UnaryOperator<ShopItem> action) {
        final ShopItem item = getByUUID(uuid);
        final ShopItem modified = action.apply(item);
        return itemRepository.save(modified);
    }

}
