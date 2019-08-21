package org.openhab.binding.lgwebos.internal.handler;

import com.google.gson.JsonElement;

public class Response {
    /** Required response type */
    private String type;
    /** Optional payload */
    private JsonElement payload;
    /**
     * Message ID to which this is a response to.
     * This is optional.
     */
    private Integer id;

    /** Optional error message. */
    private String error;

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getError() {
        return error;
    }

    public JsonElement getPayload() {
        return payload;
    }
}
