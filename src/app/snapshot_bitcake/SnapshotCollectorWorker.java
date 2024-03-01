package app.snapshot_bitcake;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.message.Message;
import servent.message.snapshot.ABAskMessage;
import servent.message.util.MessageUtil;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private final AtomicBoolean collecting = new AtomicBoolean(false);

	private final AtomicBoolean second = new AtomicBoolean(false);
	@Override
	public AtomicBoolean getCollecting() {
		return collecting;
	}
	private final Map<String, Integer> collectedNaiveValues = new ConcurrentHashMap<>();
	private final Map<String, SnapshotDataEntry> collectedABValues = new ConcurrentHashMap<>();

	private SnapshotType snapshotType = SnapshotType.NAIVE;
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;

		switch (snapshotType) {
			case AB -> bitcakeManager = new ABManager();
			case NONE -> {
				AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
				System.exit(0);
			}
		}
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (!working) {
					return;
				}
			}

			Map<Integer, Integer> vectorClock;
			Message askMessage;
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */
			
			//1 send asks
				switch (snapshotType) {
					case AB:
						vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

						askMessage = new ABAskMessage(
								AppConfig.myServentInfo,
								null,
								null,
								vectorClock
						);

						for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
							askMessage = askMessage.changeReceiver(neighbor);

							MessageUtil.sendMessage(askMessage);
						}

						addABSnapshotInfo(
								"node" + AppConfig.myServentInfo.getId(),
								bitcakeManager.getCurrentBitcakeAmount(),
								CausalBroadcastShared.getSentTransactions(),
								CausalBroadcastShared.getReceivedTransactions()
						);

						// Increment clock for original sender
						CausalBroadcastShared.commitCausalMessage(askMessage);
						break;

					case NONE:
						//Shouldn't be able to come here. See constructor.
						break;
				}

			//2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
				case AB:
					if (collectedABValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;

				case NONE:
				//Shouldn't be able to come here. See constructor.
				break;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (!working) {
					return;
				}
			}
			
			//print
			int sum;
			switch (snapshotType) {
			case AB:
				sum = 0;
				for (Entry<String, SnapshotDataEntry> itemAmount : collectedABValues.entrySet()) {
					SnapshotDataEntry entry = itemAmount.getValue();

					int totalAmount = entry.getAmount();
					List<Message> sendTransactions = entry.getSendTransactions();

					sum += totalAmount;
					AppConfig.timestampedStandardPrint("Info for " + itemAmount.getKey() + " = " + totalAmount + " bitcake");


					for (Message sendTransaction : sendTransactions) {
						entry = collectedABValues.get("node" + sendTransaction.getOriginalReceiverInfo().getId());
						List<Message> receivedTransactions = entry.getReceivedTransactions();

						boolean ChannelMessage = false;
						for (Message receivedTransaction : receivedTransactions) {
							if (sendTransaction.getMessageId() == receivedTransaction.getMessageId() &&
								sendTransaction.getOriginalSenderInfo().getId() == receivedTransaction.getOriginalSenderInfo().getId() &&
								sendTransaction.getOriginalReceiverInfo().getId() == receivedTransaction.getOriginalReceiverInfo().getId())
							{
								ChannelMessage = true;
								break;
							}
						}

						if (!ChannelMessage) {
							AppConfig.timestampedStandardPrint(
									"Info for transaction in channel between node "+ sendTransaction.getOriginalReceiverInfo().getId() +" and node "
											+sendTransaction.getOriginalSenderInfo().getId()+ ": "  + sendTransaction.getMessageText() + " bitcake");

							int amountNumber = Integer.parseInt(sendTransaction.getMessageText());

							sum += amountNumber;
						}
					}
				}

				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

				second.set(false);

				collectedABValues.clear(); //reset for next invocation
				break;

			case NONE:
				//Shouldn't be able to come here. See constructor. 
				break;
			}
			collecting.set(false);
		}

	}
	
	@Override
	public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {
		collectedNaiveValues.put(snapshotSubject, amount);
	}

	@Override
	public void addABSnapshotInfo(String node, int amount, List<Message> sendTransactions, List<Message> receivedTransactions) {

		SnapshotDataEntry entry = new SnapshotDataEntry(amount,sendTransactions, receivedTransactions);
		collectedABValues.put(node, entry);
	}

	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);
		
		if (oldValue) {
			second.set(true);
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

}
