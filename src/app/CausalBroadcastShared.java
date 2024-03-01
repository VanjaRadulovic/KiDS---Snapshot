package app;

import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.ABAskHandler;
import servent.handler.snapshot.ABTellHandler;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.*;
import java.util.concurrent.*;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li> Vector clock for current instance
 * <li> Commited message list
 * <li> Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 * 
 * @author bmilojkovic
 *
 */
public class CausalBroadcastShared {

	private static final Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>();
	private static final List<Message> commitedCausalMessageList = new CopyOnWriteArrayList<>();
	private static final Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
	private static final Object pendingMessagesLock = new Object();
	private static SnapshotCollector snapshotCollector;
	private static final List<Message> sentTransactions = new CopyOnWriteArrayList<>();
	private static final List<Message> receivedTransactions = new CopyOnWriteArrayList<>();
	public static final ExecutorService threadPool = Executors.newWorkStealingPool();

	public static void setSnapshotCollector(SnapshotCollector sc) {
		snapshotCollector = sc;
	}

	public static SnapshotCollector getSnapshotCollector() {
		return snapshotCollector;
	}

	public static void addReceivedTransaction(Message receivedTransaction) {
		receivedTransactions.add(receivedTransaction);
	}

	public static List<Message> getReceivedTransactions() {
		return receivedTransactions;
	}

	public static void addSentTransaction(Message sendTransaction) {
		sentTransactions.add(sendTransaction);
	}

	public static List<Message> getSentTransactions() {
		return sentTransactions;
	}

	public static void initializeVectorClock(int serventCount) {
		for(int i = 0; i < serventCount; i++) {
			vectorClock.put(i, 0);
		}
	}
	
	public static void incrementClock(int serventId) {
		vectorClock.computeIfPresent(serventId, (key, oldValue) -> oldValue+1);
	}
	
	public static Map<Integer, Integer> getVectorClock() {
		return vectorClock;
	}
	
	public static List<Message> getCommitedCausalMessages() {
		List<Message> toReturn = new CopyOnWriteArrayList<>(commitedCausalMessageList);
		
		return toReturn;
	}
	
	public static void addPendingMessage(Message msg) {
		pendingMessages.add(msg);
	}
	
	public static void commitCausalMessage(Message newMessage) {
		AppConfig.timestampedStandardPrint("Committing " + newMessage);
		commitedCausalMessageList.add(newMessage);
		incrementClock(newMessage.getOriginalSenderInfo().getId());

		checkPendingMessages();
	}
	
	private static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
		if (clock1.size() != clock2.size()) {
			throw new IllegalArgumentException("Clocks are not same size how why");
		}
		
		for(int i = 0; i < clock1.size(); i++) {
			if (clock2.get(i) > clock1.get(i)) {
				return true;
			}
		}
		
		return false;
	}

	public static void checkPendingMessages() {
		boolean gotWork = true;

		while (gotWork) {
			gotWork = false;

			synchronized (pendingMessagesLock) {
				Iterator<Message> iterator = pendingMessages.iterator();

				Map<Integer, Integer> myVectorClock = getVectorClock();
				while (iterator.hasNext()) {
					Message pendingMessage = iterator.next();
					BasicMessage message = (BasicMessage)pendingMessage;

					if (!otherClockGreater(myVectorClock, message.getSenderVectorClock())) {
						gotWork = true;

						AppConfig.timestampedStandardPrint("Committing " + pendingMessage);
						commitedCausalMessageList.add(pendingMessage);
						incrementClock(pendingMessage.getOriginalSenderInfo().getId());

						MessageHandler messageHandler = new NullHandler(message);

						MessageType messageType = message.getMessageType();

						switch (messageType) {
							case TRANSACTION:
								if (message.getOriginalReceiverInfo().getId() == AppConfig.myServentInfo.getId()) {
									messageHandler = new TransactionHandler(message, snapshotCollector.getBitcakeManager());
								}
								break;
							case AB_ASK:
								if (!snapshotCollector.getCollecting().get()) {
									messageHandler = new ABAskHandler(message, snapshotCollector);
								}
								break;
							case AB_TELL:
								if (message.getOriginalReceiverInfo().getId() == AppConfig.myServentInfo.getId()) {
									messageHandler = new ABTellHandler(message, snapshotCollector);
								}
								break;
						}

						threadPool.submit(messageHandler);

						iterator.remove();
						break;
					}
				}
			}
		}

	}

}
