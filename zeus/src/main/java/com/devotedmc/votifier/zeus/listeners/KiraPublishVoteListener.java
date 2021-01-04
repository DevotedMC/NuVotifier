package com.devotedmc.votifier.zeus.listeners;

import com.devotedmc.votifier.zeus.events.VotifierEvent;
import com.github.maxopoly.zeus.plugin.event.ZEventHandler;
import com.github.maxopoly.zeus.plugin.event.ZeusListener;

public class KiraPublishVoteListener implements ZeusListener {
    @ZEventHandler
    public void onVoteReceived(VotifierEvent e) {
        System.out.println("Kira would get this vote " + e.getVote());
        //TODO use DAO to insert and publish to Kira
    }
}
