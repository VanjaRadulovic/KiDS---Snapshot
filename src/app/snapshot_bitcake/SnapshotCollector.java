package app.snapshot_bitcake;

import app.Cancellable;
import servent.message.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	AtomicBoolean getCollecting();

	BitcakeManager getBitcakeManager();

	void addNaiveSnapshotInfo(String snapshotSubject, int amount);

	void addABSnapshotInfo(String snapshotSubject, int amount,
						   List<Message> sendTransactions, List<Message> receivedTransactions);

	void startCollecting();

}