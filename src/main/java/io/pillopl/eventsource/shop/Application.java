package io.pillopl.eventsource.shop;

import io.pillopl.eventsource.shop.boundary.ShopItems;
import io.pillopl.eventsource.shop.domain.commands.Buy;
import io.pillopl.eventsource.shop.domain.commands.Command;
import io.pillopl.eventsource.shop.domain.commands.MarkPaymentTimeout;
import io.pillopl.eventsource.shop.domain.commands.Pay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableScheduling
@EnableBinding(Processor.class)
@Slf4j
public class Application {

    @Autowired
    ShopItems shopItems;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.run(args);
    }

    @Scheduled(fixedRate = 5000)
    public void anyBoughtItem() {
        final UUID uuid = UUID.randomUUID();
        shopItems.buy(new Buy(uuid, BigDecimal.TEN, Instant.now()));
        shopItems.pay(new Pay(uuid, Instant.now()));
    }

    @StreamListener(Sink.INPUT)
    public void commandStream(Command command) {
        log.info("Received command {}", command);
        if (command instanceof MarkPaymentTimeout) {
            shopItems.markPaymentTimeout((MarkPaymentTimeout) command);
        } else if (command instanceof Buy) {
            shopItems.buy((Buy) command);
        } else if (command instanceof Pay) {
            shopItems.pay((Pay) command);
        }
    }
}
