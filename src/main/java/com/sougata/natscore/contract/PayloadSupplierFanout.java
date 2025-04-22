package com.sougata.natscore.contract;

import com.sougata.natscore.model.PayloadWrapper;

import java.util.List;

public interface PayloadSupplierFanout {
    List<PayloadWrapper<byte[]>> supply();
}