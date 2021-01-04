package com.devotedmc.votifier.zeus.commands;

import com.devotedmc.votifier.zeus.NuVotifier;
import com.github.maxopoly.zeus.commands.ZCommand;
import com.github.maxopoly.zeus.commands.ZeusCommand;
import com.github.maxopoly.zeus.commands.sender.CommandSender;

@ZCommand(
        description = "Reload NuVotifier",
        altIds = "nvreload",
        id = "znvreload"
)
public class NVReload extends ZeusCommand {
    @Override
    public String handle(CommandSender commandSender, String s) {
        return NuVotifier.getInstance().reload() ? "NuVotifier has been reloaded!" : "Looks like there was a problem reloading NuVotifier, check the exceptions!";
    }
}
