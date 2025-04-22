package com.sougata.natscore.util;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.security.MessageDigest;
import java.util.HexFormat;

public final class ProtobufUtils {
    public static String sha256Hex(Descriptors.Descriptor descriptor) throws Exception {
        DescriptorProtos.DescriptorProto proto = descriptor.toProto();
        byte[] bytes = proto.toByteArray();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
}
