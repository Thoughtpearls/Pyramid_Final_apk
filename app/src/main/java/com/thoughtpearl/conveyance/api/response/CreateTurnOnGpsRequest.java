package com.thoughtpearl.conveyance.api.response;

public class CreateTurnOnGpsRequest {
    String type;
    String message;

    public CreateTurnOnGpsRequest(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
