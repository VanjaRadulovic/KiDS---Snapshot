package app.snapshot_bitcake;

import servent.message.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	public AtomicBoolean getCollecting(){
		return null;
	}

	@Override
	public void run() {}

	@Override
	public void stop() {}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}

	@Override
	public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {}

	@Override
	public void addABSnapshotInfo(String snapshotSubject, int amount,
								  List<Message> sendTransactions, List<Message> receivedTransactions) {}

	@Override
	public void startCollecting() {}

}
