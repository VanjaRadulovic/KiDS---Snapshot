package cli.command;

import app.AppConfig;
import app.CausalBroadcastShared;

public class SnapshotInfoCommand implements CLICommand {

    @Override
    public String commandName() {
        return "snapshot";
    }

    @Override
    public void execute(String args) {

        AppConfig.timestampedStandardPrint(CausalBroadcastShared.getVectorClock().toString());

        AppConfig.timestampedStandardPrint("hello " + CausalBroadcastShared.getSnapshotCollector().getBitcakeManager().getCurrentBitcakeAmount());
    }

}