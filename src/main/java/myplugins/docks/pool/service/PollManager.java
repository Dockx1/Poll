package myplugins.docks.pool.service;

import myplugins.docks.pool.Pool;
import myplugins.docks.pool.model.Poll;
import myplugins.docks.pool.model.PollOption;

import java.io.File;
import java.sql.*;
import java.util.*;

public class PollManager {

    private final Pool plugin;
    private Connection connection;

    public PollManager(Pool plugin) {
        this.plugin = plugin;
        try {
            initDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize poll database", e);
        }
    }

    private void initDatabase() throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "polls.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        connection = DriverManager.getConnection(url);

        try (Statement stmt = connection.createStatement()) {
            // Enable foreign keys just in case we add them later
            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS polls (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    question TEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    ends_at BIGINT NOT NULL,
                    closed INTEGER NOT NULL DEFAULT 0,
                    creator_uuid TEXT
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS poll_options (
                    poll_id INTEGER NOT NULL,
                    idx INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    votes INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (poll_id, idx)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS poll_votes (
                    poll_id INTEGER NOT NULL,
                    voter_uuid TEXT NOT NULL,
                    option_index INTEGER NOT NULL,
                    PRIMARY KEY (poll_id, voter_uuid)
                )
                """);
        }
    }

    public void load() {
        // No cache to load - we query the DB on demand.
        plugin.getLogger().info("Poll database initialized (SQLite).");
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close poll database: " + e.getMessage());
            }
        }
    }

    public enum VoteResult {
        SUCCESS,
        ALREADY_VOTED,
        POLL_CLOSED,
        INVALID_OPTION
    }

    // ================== Query helpers ==================

    public Poll getPoll(int id) {
        try {
            Poll poll = null;
            // Load poll row
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, question, created_at, ends_at, closed, creator_uuid FROM polls WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String question = rs.getString("question");
                        long createdAt = rs.getLong("created_at");
                        long endsAt = rs.getLong("ends_at");
                        boolean closed = rs.getInt("closed") != 0;
                        String creatorStr = rs.getString("creator_uuid");
                        UUID creatorUuid = null;
                        if (creatorStr != null && !creatorStr.isBlank()) {
                            try {
                                creatorUuid = UUID.fromString(creatorStr);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                        poll = new Poll(id, question, createdAt, endsAt, creatorUuid);
                        poll.setClosed(closed);
                    }
                }
            }

            if (poll == null) {
                return null;
            }

            // Load options
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT idx, text, votes FROM poll_options WHERE poll_id = ? ORDER BY idx ASC")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idx = rs.getInt("idx");
                        String text = rs.getString("text");
                        int votes = rs.getInt("votes");
                        PollOption option = new PollOption(idx, text, votes);
                        poll.addOption(option);
                    }
                }
            }

            // Load votes (who voted which option)
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT voter_uuid, option_index FROM poll_votes WHERE poll_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String uuidStr = rs.getString("voter_uuid");
                        int optIndex = rs.getInt("option_index");
                        try {
                            UUID voter = UUID.fromString(uuidStr);
                            poll.recordVote(voter, optIndex);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }

            return poll;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load poll #" + id + ": " + e.getMessage());
            return null;
        }
    }

    public List<Poll> getActivePolls() {
        List<Poll> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM polls WHERE closed = 0 AND ends_at > ? ORDER BY id ASC")) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    Poll poll = getPoll(id);
                    if (poll != null) {
                        list.add(poll);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get active polls: " + e.getMessage());
        }

        return list;
    }

    public List<Integer> getAllPollIds() {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM polls ORDER BY id ASC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get poll ids: " + e.getMessage());
        }
        return ids;
    }

    // ================ Mutating operations =================

    public Poll createPoll(String question, long durationMillis, Map<Integer, String> optionsTexts, UUID creatorUuid) {
        long now = System.currentTimeMillis();
        long endsAt = now + durationMillis;

        try {
            connection.setAutoCommit(false);

            int pollId;
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO polls (question, created_at, ends_at, closed, creator_uuid) VALUES (?, ?, ?, 0, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, question);
                ps.setLong(2, now);
                ps.setLong(3, endsAt);
                ps.setString(4, creatorUuid != null ? creatorUuid.toString() : null);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        pollId = keys.getInt(1);
                    } else {
                        connection.rollback();
                        throw new SQLException("Failed to retrieve generated poll id.");
                    }
                }
            }

            Poll poll = new Poll(pollId, question, now, endsAt, creatorUuid);

            // Insert options
            try (PreparedStatement psOpt = connection.prepareStatement(
                    "INSERT INTO poll_options (poll_id, idx, text, votes) VALUES (?, ?, ?, 0)")) {
                for (Map.Entry<Integer, String> entry : optionsTexts.entrySet()) {
                    int idx = entry.getKey();
                    String text = entry.getValue();
                    if (text == null || text.isBlank()) continue;

                    psOpt.setInt(1, pollId);
                    psOpt.setInt(2, idx);
                    psOpt.setString(3, text);
                    psOpt.addBatch();

                    poll.addOption(new PollOption(idx, text, 0));
                }
                psOpt.executeBatch();
            }

            connection.commit();
            connection.setAutoCommit(true);

            return poll;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            plugin.getLogger().severe("Failed to create poll: " + e.getMessage());
            return null;
        }
    }

    public boolean closePoll(int id) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE polls SET closed = 1 WHERE id = ?")) {
            ps.setInt(1, id);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close poll #" + id + ": " + e.getMessage());
            return false;
        }
    }

    public boolean removePoll(int id) {
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement psVotes = connection.prepareStatement(
                    "DELETE FROM poll_votes WHERE poll_id = ?")) {
                psVotes.setInt(1, id);
                psVotes.executeUpdate();
            }

            try (PreparedStatement psOpts = connection.prepareStatement(
                    "DELETE FROM poll_options WHERE poll_id = ?")) {
                psOpts.setInt(1, id);
                psOpts.executeUpdate();
            }

            int deletedPolls;
            try (PreparedStatement psPoll = connection.prepareStatement(
                    "DELETE FROM polls WHERE id = ?")) {
                psPoll.setInt(1, id);
                deletedPolls = psPoll.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(true);

            return deletedPolls > 0;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            plugin.getLogger().severe("Failed to remove poll #" + id + ": " + e.getMessage());
            return false;
        }
    }

    public VoteResult vote(Poll poll, UUID playerId, int optionIndex) {
        if (poll == null) {
            return VoteResult.POLL_CLOSED;
        }
        if (poll.isClosed()) {
            closePoll(poll.getId());
            return VoteResult.POLL_CLOSED;
        }
        if (!poll.getOptions().containsKey(optionIndex)) {
            return VoteResult.INVALID_OPTION;
        }
        if (poll.hasVoted(playerId)) {
            return VoteResult.ALREADY_VOTED;
        }

        // Update in-memory model
        poll.recordVote(playerId, optionIndex);
        PollOption option = poll.getOptions().get(optionIndex);
        option.incrementVotes();

        // Persist in DB
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement psVote = connection.prepareStatement(
                    "INSERT INTO poll_votes (poll_id, voter_uuid, option_index) VALUES (?, ?, ?)")) {
                psVote.setInt(1, poll.getId());
                psVote.setString(2, playerId.toString());
                psVote.setInt(3, optionIndex);
                psVote.executeUpdate();
            }

            try (PreparedStatement psOpt = connection.prepareStatement(
                    "UPDATE poll_options SET votes = ? WHERE poll_id = ? AND idx = ?")) {
                psOpt.setInt(1, option.getVotes());
                psOpt.setInt(2, poll.getId());
                psOpt.setInt(3, optionIndex);
                psOpt.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            plugin.getLogger().severe("Failed to record vote for poll #" + poll.getId() + ": " + e.getMessage());
            return VoteResult.INVALID_OPTION; // generic failure
        }

        return VoteResult.SUCCESS;
    }

    public void expirePolls() {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE polls SET closed = 1 WHERE closed = 0 AND ends_at < ?")) {
            ps.setLong(1, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to expire polls: " + e.getMessage());
        }
    }
}
