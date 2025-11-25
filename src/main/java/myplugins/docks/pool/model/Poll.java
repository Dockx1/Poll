package myplugins.docks.pool.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Poll {

    private final int id;
    private final String question;
    private final long createdAt;
    private final long endsAt;
    private final UUID creatorUuid;

    private final Map<Integer, PollOption> options = new LinkedHashMap<>();
    private final Map<UUID, Integer> votes = new HashMap<>();

    private boolean closed;

    public Poll(int id, String question, long createdAt, long endsAt, UUID creatorUuid) {
        this.id = id;
        this.question = question;
        this.createdAt = createdAt;
        this.endsAt = endsAt;
        this.creatorUuid = creatorUuid;
    }

    public int getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getEndsAt() {
        return endsAt;
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public Map<Integer, PollOption> getOptions() {
        return options;
    }

    public Map<UUID, Integer> getVotes() {
        return votes;
    }

    public boolean isClosed() {
        return closed || System.currentTimeMillis() > endsAt;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void addOption(PollOption option) {
        options.put(option.getIndex(), option);
    }

    public boolean hasVoted(UUID playerId) {
        return votes.containsKey(playerId);
    }

    public void recordVote(UUID playerId, int optionIndex) {
        votes.put(playerId, optionIndex);
    }

    public int getTotalVotes() {
        return votes.size();
    }
}
