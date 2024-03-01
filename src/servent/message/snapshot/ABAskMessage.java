package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class ABAskMessage extends BasicMessage {

    private static final long serialVersionUID = -1189395068257017543L;

    public ABAskMessage(ServentInfo sender, ServentInfo receiver, ServentInfo neighbor,
                        Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AB_ASK, sender, receiver, neighbor, senderVectorClock);
    }
}
