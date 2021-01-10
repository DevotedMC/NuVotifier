package com.devotedmc.votifier.zeus.commands;

import com.devotedmc.votifier.zeus.NuVotifier;
import com.github.maxopoly.zeus.commands.ZCommand;
import com.github.maxopoly.zeus.commands.ZeusCommand;
import com.github.maxopoly.zeus.commands.sender.CommandSender;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.ArgsToVote;

@ZCommand(
        description = "Trigger vote listeners with a test vote",
        altIds = "testvote",
        id = "ztestvote"
)
public class TestVote extends ZeusCommand {
    @Override
    public String handle(CommandSender commandSender, String s) {
        Vote v;
        try {
            v = ArgsToVote.parse(s.split(" "));
        } catch (IllegalArgumentException e) {
            return "Error while parsing arguments to create test vote: " + e.getMessage();
        }

        NuVotifier.getInstance().onVoteReceived(v, VotifierSession.ProtocolVersion.TEST, "localhost.test");
        return "Test vote executed: " + v.toString();
    }
}
