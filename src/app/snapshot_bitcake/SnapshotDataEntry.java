package app.snapshot_bitcake;

import servent.message.Message;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SnapshotDataEntry {

    private int amount;
    private List<Message> sendTransactions;
    private List<Message> receivedTransactions;

    public int getAmount() {
        return amount;
    }

    public List<Message> getSendTransactions() {
        return sendTransactions;
    }

    public List<Message> getReceivedTransactions() {
        return receivedTransactions;
    }

    public SnapshotDataEntry(int amount, List<Message> sendTransaction, List<Message> receivedTransactions){
        this.amount = amount;
        this.sendTransactions =  new CopyOnWriteArrayList<>(sendTransaction);
        this.receivedTransactions =  new CopyOnWriteArrayList<>(receivedTransactions);
    }
}
