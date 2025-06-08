package com.imdc.milkdespencer.models.Response;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RazorpayQrPaymentResponse {
    @SerializedName("entity")
    private String entity;

    @SerializedName("count")
    private int count;

    @SerializedName("items")
    private List<PaymentItem> items;

    public String getEntity() {
        return entity;
    }

    public int getCount() {
        return count;
    }

    public List<PaymentItem> getItems() {
        return items;
    }

    public static class PaymentItem {

        @SerializedName("id")
        private String id;

        @SerializedName("entity")
        private String entity;

        @SerializedName("amount")
        private Integer amount;

        @SerializedName("currency")
        private String currency;

        @SerializedName("status")
        private String status;

        @SerializedName("order_id")
        private String orderId;

        @SerializedName("invoice_id")
        private String invoiceId;

        @SerializedName("international")
        private boolean international;

        @SerializedName("method")
        private String method;

        @SerializedName("amount_refunded")
        private int amountRefunded;

        @SerializedName("refund_status")
        private String refundStatus;

        @SerializedName("captured")
        private boolean captured;

        @SerializedName("description")
        private String description;

        @SerializedName("card_id")
        private String cardId;

        @SerializedName("bank")
        private String bank;

        @SerializedName("wallet")
        private String wallet;

        @SerializedName("vpa")
        private String vpa;

        @SerializedName("email")
        private String email;

        @SerializedName("contact")
        private String contact;

        @SerializedName("customer_id")
        private String customerId;

        @SerializedName("notes")
        private Notes notes;

        @SerializedName("fee")
        private Integer fee;

        @SerializedName("tax")
        private Integer tax;

        @SerializedName("error_code")
        private String errorCode;

        @SerializedName("error_description")
        private String errorDescription;

        @SerializedName("error_source")
        private String errorSource;

        @SerializedName("error_step")
        private String errorStep;

        @SerializedName("error_reason")
        private String errorReason;

        @SerializedName("acquirer_data")
        private AcquirerData acquirerData;

        @SerializedName("created_at")
        private long createdAt;

        @SerializedName("authorized_at")
        private long authorizedAt;

        @SerializedName("auto_captured")
        private boolean autoCaptured;

        @SerializedName("captured_at")
        private long capturedAt;

        @SerializedName("late_authorized")
        private boolean lateAuthorized;

        @SerializedName("upi")
        private Upi upi;

        // Getters for each field (add as needed)

        public static class Notes {
            @SerializedName("notes_key_1")
            private String notesKey1;

            @SerializedName("notes_key_2")
            private String notesKey2;

            public String getNotesKey1() {
                return notesKey1;
            }

            public String getNotesKey2() {
                return notesKey2;
            }
        }

        public static class AcquirerData {
            @SerializedName("rrn")
            private String rrn;

            public String getRrn() {
                return rrn;
            }
        }

        public static class Upi {
            @SerializedName("payer_account_type")
            private String payerAccountType;

            @SerializedName("vpa")
            private String vpa;

            public String getPayerAccountType() {
                return payerAccountType;
            }

            public String getVpa() {
                return vpa;
            }
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}

