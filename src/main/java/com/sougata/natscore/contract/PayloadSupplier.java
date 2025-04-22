package com.sougata.natscore.contract;

import com.sougata.natscore.model.PayloadWrapper;

public interface PayloadSupplier {
    PayloadWrapper<byte[]> supply();
}