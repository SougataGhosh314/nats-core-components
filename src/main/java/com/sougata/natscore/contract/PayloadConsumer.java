package com.sougata.natscore.contract;

import com.sougata.natscore.model.PayloadWrapper;

public interface PayloadConsumer {
    void consume(PayloadWrapper<byte[]> message);
}