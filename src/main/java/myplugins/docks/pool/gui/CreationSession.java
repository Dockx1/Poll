package myplugins.docks.pool.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreationSession {

    private final UUID playerId;
    private final String question;
    private final long durationMillis;

    private final Map<Integer, String> options = new HashMap<>();
    private int awaitingOption = -1;

    public CreationSession(UUID playerId, String question, long durationMillis) {
        this.playerId = playerId;
        this.question = question;
        this.durationMillis = durationMillis;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getQuestion() {
        return question;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public Map<Integer, String> getOptions() {
        return options;
    }

    public int getAwaitingOption() {
        return awaitingOption;
    }

    public void setAwaitingOption(int awaitingOption) {
        this.awaitingOption = awaitingOption;
    }

    public boolean hasAtLeastTwoOptions() {
        long count = options.values().stream()
                .filter(s -> s != null && !s.isBlank())
                .count();
        return count >= 2;
    }
}
