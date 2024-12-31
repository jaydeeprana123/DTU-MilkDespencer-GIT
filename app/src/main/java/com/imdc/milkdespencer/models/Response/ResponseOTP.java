package com.imdc.milkdespencer.models.Response;

import java.io.Serializable;
import java.util.List;

public class ResponseOTP implements Serializable {
    private List<Data> data;

    private String sender;

    private String createdDateTime;

    private String source;

    private String id;

    private String body;

    private String type;

    private Integer totalCount;

    private Object error;

    public List<Data> getData() {
        return this.data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getCreatedDateTime() {
        return this.createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTotalCount() {
        return this.totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Object getError() {
        return this.error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public static class Data implements Serializable {
        private String recipient;

        private String message_id;

        public String getRecipient() {
            return this.recipient;
        }

        public void setRecipient(String recipient) {
            this.recipient = recipient;
        }

        public String getMessage_id() {
            return this.message_id;
        }

        public void setMessage_id(String message_id) {
            this.message_id = message_id;
        }
    }
}
