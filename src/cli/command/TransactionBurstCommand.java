package cli.command;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import java.util.concurrent.ThreadLocalRandom;
import app.snapshot_bitcake.ABManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.util.MessageUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionBurstCommand implements CLICommand {

	private static final int TRANSACTION_COUNT = 5;
	private static final int BURST_WORKERS = 5;
	private static final int MAX_TRANSFER_AMOUNT = 5;
	
	private final SnapshotCollector snapshotCollector;

	private final String LOCK = "LOCK";
	
	public TransactionBurstCommand(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}
	
	private class TransactionBurstWorker implements Runnable {
		
		@Override
		public void run() {
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			for (int i = 0; i < TRANSACTION_COUNT; i++) {
				ServentInfo receiverInfo = AppConfig.getInfoById(1 + rand.nextInt(AppConfig.getServentCount()));


				while (receiverInfo.getId() == AppConfig.myServentInfo.getId()) {
					receiverInfo = AppConfig.getInfoById(1 + rand.nextInt(AppConfig.getServentCount()));
				}

				int amount = 1 + rand.nextInt(MAX_TRANSFER_AMOUNT);


				synchronized (LOCK) {

					Map<Integer, Integer> vectorClock = new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock());

					Message transactionMessage = new TransactionMessage(AppConfig.myServentInfo, receiverInfo, null, vectorClock, amount,
							snapshotCollector.getBitcakeManager());

					transactionMessage.sendEffect();

					CausalBroadcastShared.addSentTransaction(transactionMessage);
					CausalBroadcastShared.commitCausalMessage(transactionMessage);

					for (int neighbor : AppConfig.myServentInfo.getNeighbors()) {
						MessageUtil.sendMessage(transactionMessage.changeReceiver(neighbor).makeMeASender());
					}
				}

			}
		}
	}
	
	@Override
	public String commandName() {
		return "transaction_burst";
	}

	@Override
	public void execute(String args) {
		for (int i = 0; i < BURST_WORKERS; i++) {
			Thread t = new Thread(new TransactionBurstWorker());
			
			t.start();
		}
	}

	
}
