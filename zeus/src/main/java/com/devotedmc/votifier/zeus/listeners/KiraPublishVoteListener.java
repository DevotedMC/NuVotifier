package com.devotedmc.votifier.zeus.listeners;

import com.devotedmc.votifier.zeus.events.VotifierEvent;
import com.github.maxopoly.zeus.plugin.event.ZEventHandler;
import com.github.maxopoly.zeus.plugin.event.ZeusListener;

public class KiraPublishVoteListener implements ZeusListener {
    @ZEventHandler
    public void onVoteReceived(VotifierEvent e) {
        //TODO Kira hook goes here @Max
        System.out.println(e.getVote().getUsername() + " is supporting Devoted Hell!");
    }
}
