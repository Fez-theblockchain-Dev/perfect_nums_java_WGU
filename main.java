/*
 * Perfect number finder (works on JDK 17 and newer, including JDK 21).
 *
 * A perfect number equals the sum of its proper divisors:
 *   6  -> 1 + 2 + 3          = 6
 *   28 -> 1 + 2 + 4 + 7 + 14 = 28
 *
 * Compile and run:
 *   javac main.java
 *   java Main
 */

import java.util.List;
import java.util.Scanner;
import java.util.stream.LongStream;

class Main {

    /** Groups the answer for one search so the range and its hits travel together. */
    record PerfectSearch(long limit, List<Long> numbers) {

        static PerfectSearch upTo(long limit) {
            var hits = LongStream.rangeClosed(2, limit)
                    .filter(PerfectSearch::isPerfect)
                    .boxed()
                    .toList();
            return new PerfectSearch(limit, hits);
        }

        static boolean isPerfect(long n) {
            return n > 1 && sumOfProperDivisors(n) == n;
        }

        /**
         * Sum of every divisor of n below n, walking only up to sqrt(n)
         * and adding divisors in pairs.
         */
        static long sumOfProperDivisors(long n) {
            long sum = 1;                       // 1 divides every n > 1
            for (long d = 2; d * d <= n; d++) {
                if (n % d != 0) {
                    continue;
                }
                sum += d;
                long paired = n / d;
                if (paired != d) {             // skip the square root when n is a perfect square
                    sum += paired;
                }
            }
            return sum;
        }
    }

    /** Shortcut so other classes (like OverflowTests) can call Main.isPerfect(n). */
    static boolean isPerfect(long n) {
        return PerfectSearch.isPerfect(n);
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        // Part 1: the fixed range the assignment asks for.
        var fixed = PerfectSearch.upTo(200);
        System.out.println("Perfect numbers between 1 and 200: " + fixed.numbers());

        // Part 2: let the user pick the upper limit.
        long limit = readLimit(input, 200);
        var search = PerfectSearch.upTo(limit);

        System.out.println(switch (search.numbers().size()) {
            case 0 -> "No perfect numbers between 1 and " + limit + ".";
            case 1 -> "1 perfect number between 1 and " + limit + ": " + search.numbers();
            default -> search.numbers().size()
                    + " perfect numbers between 1 and " + limit + ": " + search.numbers();
        });

        input.close();
    }

    /**
     * Reads a positive whole number from the console, falling back to
     * fallback on blank or invalid input.
     */
    static long readLimit(Scanner input, long fallback) {
        System.out.print("Enter the upper limit for the search [" + fallback + "]: ");
        if (!input.hasNextLine()) {
            return fallback;
        }
        String raw = input.nextLine().trim();
        if (raw.isEmpty()) {
            return fallback;
        }
        try {
            long value = Long.parseLong(raw);
            if (value < 1) {
                System.out.println("The limit must be at least 1 — using " + fallback + " instead.");
                return fallback;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("\"" + raw + "\" is not a whole number — using " + fallback + " instead.");
            return fallback;
        }
    }
}
