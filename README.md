# Sample event sourced application with Command Query Responsibility Segregation

** Event sourcing **

Shop item can be ordered, paid for, and marked as timed out (payment missing). Aggregate root (ShopItem) emits 3 different types of domain events: ItemBought, ItemPaid, ItemPaymentMissing. All of them are consequences of commands.

Event store is constructed in database as EventStream table with collection of EventDescriptors. EventStream is fetched by unique aggregate root uuid.

Application keep sending events to kafka broker. Kafka is bound with the use of spring stream