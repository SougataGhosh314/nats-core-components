package com.sougata.natscore.registry;

import com.sougata.natscore.config.EventComponentConfig;
import com.sougata.natscore.config.EventComponentEntry;
import com.sougata.natscore.contract.*;
import com.sougata.natscore.dispatcher.*;
import org.springframework.stereotype.Component;

@Component
public class NatsEventComponentRegistrar {
    public NatsEventComponentRegistrar(
            EventComponentConfig config, org.springframework.context.ApplicationContext context,
            ConsumerDispatcher consumerDispatcher,
            SupplierDispatcher supplierDispatcher,
            SupplierFanoutDispatcher supplierFanoutDispatcher,
            FunctionDispatcher functionDispatcher,
            FunctionFanoutDispatcher functionFanoutDispatcher
    ) throws ClassNotFoundException {

        for (EventComponentEntry entry : config.getComponents()) {
            Object bean = context.getBean(Class.forName(entry.getHandlerClass()));

            switch (entry.getHandlerType()) {
                case CONSUMER -> consumerDispatcher.register(entry.getReadTopics(), (PayloadConsumer) bean);
                case SUPPLIER -> supplierDispatcher.register((PayloadSupplier) bean);
                case SUPPLIER_FANOUT -> supplierFanoutDispatcher.register((PayloadSupplierFanout) bean);
                case FUNCTION -> functionDispatcher.register(entry.getReadTopics(), (PayloadFunction) bean);
                case FUNCTION_FANOUT -> functionFanoutDispatcher.register(entry.getReadTopics(), (PayloadFunctionFanout) bean);
                default -> throw new IllegalArgumentException("Unknown type: " + entry.getHandlerType());
            }
        }
    }
}
