package com.example.moneymate.api.transaction;

import org.springframework.hateoas.RepresentationModel;

import java.util.List;

public class TransactionCollectionResponse extends RepresentationModel<TransactionCollectionResponse> {

    private final int transactionCount;
    private final List<TransactionResponse> transactions;

    public TransactionCollectionResponse(int transactionCount, List<TransactionResponse> transactions) {
        this.transactionCount = transactionCount;
        this.transactions = transactions;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public List<TransactionResponse> getTransactions() {
        return transactions;
    }
}
