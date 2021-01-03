package com.devotedmc.votifier.zeus.events;

import com.github.maxopoly.zeus.plugin.event.ZeusEvent;
import com.vexsoftware.votifier.model.Vote;

public class VotifierEvent implements ZeusEvent {

    private Vote vote;

    public VotifierEvent(Vote vote) {
        this.vote = vote;
    }

    public Vote getVote() {
        return vote;
    }
}
