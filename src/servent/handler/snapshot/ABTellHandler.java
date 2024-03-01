package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ABTellMessage;

public class ABTellHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public ABTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
            if (clientMessage.getMessageType() == MessageType.AB_TELL) {
                int neighborAmount = Integer.parseInt(clientMessage.getMessageText());
                ABTellMessage tellAmountMessage = (ABTellMessage) clientMessage;

                snapshotCollector.addABSnapshotInfo(
                        "node" + String.valueOf(clientMessage.getOriginalSenderInfo().getId()),
                        neighborAmount,
                        tellAmountMessage.getSendTransactions(),
                        tellAmountMessage.getReceivedTransactions()
                );
            } else {
                AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
            }
    }

}