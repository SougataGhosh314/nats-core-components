package com.sougata.natscore.contract;

import com.sougata.natscore.model.PayloadWrapper;

import java.util.List;

public interface PayloadFunctionFanout {
    List<PayloadWrapper<byte[]>> process(PayloadWrapper<byte[]> request);
}