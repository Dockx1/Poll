package myplugins.docks.pool.model;

public class PollOption {

    private final int index;
    private String text;
    private int votes;

    public PollOption(int index, String text) {
        this(index, text, 0);
    }

    public PollOption(int index, String text, int votes) {
        this.index = index;
        this.text = text;
        this.votes = votes;
    }

    public int getIndex() {
        return index;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getVotes() {
        return votes;
    }

    public void incrementVotes() {
        this.votes++;
    }
}
