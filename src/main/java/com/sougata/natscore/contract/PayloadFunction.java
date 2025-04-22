package com.sougata.natscore.contract;

import com.sougata.natscore.model.PayloadWrapper;

public interface PayloadFunction {
    PayloadWrapper<byte[]> process(PayloadWrapper<byte[]> request);
}