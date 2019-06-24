package org.openhab.binding.lgwebos.internal.handler;

import com.google.gson.JsonElement;

public class Response {
    private String type;
    private JsonElement payload;
    private Integer id;
    private String error;

    public Integer getId() {
        return id;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public String getType() {
        return type;
    }

    public String getError() {
        return error;
    }
}
