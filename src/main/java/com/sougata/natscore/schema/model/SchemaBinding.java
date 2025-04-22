package com.sougata.natscore.schema.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemaBinding {
    private String topic;
    private String protoMessageType; // e.g. com.sougata.userprotos.UserRequest
    private String descriptorSha256; // SHA of the serialized descriptor (for comparison)
    private String descriptorBase64; // optional: for humans or further tooling
}
