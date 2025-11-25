package myplugins.docks.pool.util;

public final class DurationParser {

    private DurationParser() {
    }

    public static long parseToMillis(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }

        long totalMillis = 0;
        StringBuilder number = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isDigit(c)) {
                number.append(c);
                continue;
            }

            if (number.length() == 0) {
                return -1;
            }

            long value;
            try {
                value = Long.parseLong(number.toString());
            } catch (NumberFormatException ex) {
                return -1;
            }
            number.setLength(0);

            switch (Character.toLowerCase(c)) {
                case 'd':
                    totalMillis += value * 24L * 60L * 60L * 1000L;
                    break;
                case 'h':
                    totalMillis += value * 60L * 60L * 1000L;
                    break;
                case 'm':
                    totalMillis += value * 60L * 1000L;
                    break;
                case 's':
                    totalMillis += value * 1000L;
                    break;
                default:
                    return -1;
            }
        }

        if (number.length() > 0) {
            try {
                long value = Long.parseLong(number.toString());
                totalMillis += value * 60L * 1000L;
            } catch (NumberFormatException ex) {
                return -1;
            }
        }

        return totalMillis;
    }
}
