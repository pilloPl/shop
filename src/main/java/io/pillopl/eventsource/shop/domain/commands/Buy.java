package io.pillopl.eventsource.shop.domain.commands;

import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
public class Buy implements Command {

    private final UUID uuid;
    private final BigDecimal price;
    private final Instant when;

}
