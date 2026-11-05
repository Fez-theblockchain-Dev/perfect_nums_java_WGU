/**
 * Unit tests for Requirement 3: what happens when the search limit, or the
 * arithmetic inside the search, overflows the {@code int} data type?
 *
 * <p>These tests use no external libraries so they compile and run with a plain
 * JDK -- no JUnit, Maven, or Gradle required:
 *
 * <pre>
 *   javac OverflowTests.java Main.java
 *   java OverflowTests
 * </pre>
 *
 * <p>Each test documents one concrete failure mode of {@code int} and shows
 * that {@code long} (used throughout {@link Main}) handles the same case.
 *
 * <p><b>The three ways int fails, in the order a user would meet them:</b>
 * <ol>
 *   <li><b>Loud:</b> parsing a limit above 2,147,483,647 throws.</li>
 *   <li><b>Silent:</b> arithmetic past the ceiling wraps to a negative number
 *       with no exception at all -- the dangerous one.</li>
 *   <li><b>Fatal:</b> a loop counter that wraps never satisfies its exit
 *       condition, so the program hangs forever.</li>
 * </ol>
 *
 * <p>Sources: OEIS A000396 (https://oeis.org/A000396); Java Language
 * Specification 15.18.2, "Additive Operators for Numeric Types" -- integer
 * overflow wraps silently and never throws.
 */
public class OverflowTests {

    private static final int INT_MAX = Integer.MAX_VALUE;   // 2,147,483,647

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Requirement 3 -- integer overflow test suite");
        System.out.println("int  range: " + Integer.MIN_VALUE + " .. " + INT_MAX);
        System.out.println("long range: " + Long.MIN_VALUE + " .. " + Long.MAX_VALUE);
        System.out.println("---------------------------------------------------------------");

        scenario1_parsingALimitTooBigForInt();
        scenario2_intArithmeticWrapsSilently();
        scenario3_intLoopCounterNeverTerminates();
        scenario4_intDivisorSumGivesWrongAnswer();
        scenario5_longHandlesPerfectNumbersBeyondIntRange();
        scenario6_longRaisesTheCeilingButDoesNotRemoveIt();

        System.out.println("---------------------------------------------------------------");
        System.out.println("passed: " + passed + "   failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // Scenario 1: the user types a limit larger than int can hold.
    // ------------------------------------------------------------------
    private static void scenario1_parsingALimitTooBigForInt() {
        section("1. User enters a limit above int max (2147483648)");

        String typed = "2147483648";   // exactly one past Integer.MAX_VALUE

        try {
            Integer.parseInt(typed);
            check("Integer.parseInt should reject it", false, "no exception thrown");
        } catch (NumberFormatException e) {
            check("Integer.parseInt rejects it", true, "NumberFormatException: " + e.getMessage());
        }

        long asLong = Long.parseLong(typed);
        checkEquals("Long.parseLong accepts the same text", 2147483648L, asLong);
    }

    // ------------------------------------------------------------------
    // Scenario 2: int arithmetic past the ceiling -- the silent failure.
    // ------------------------------------------------------------------
    private static void scenario2_intArithmeticWrapsSilently() {
        section("2. int arithmetic overflows with NO exception");

        int wrapped = INT_MAX + 1;
        checkEquals("INT_MAX + 1 wraps to the most negative int",
                Integer.MIN_VALUE, wrapped);
        check("the wrapped value is negative", wrapped < 0,
              INT_MAX + " + 1 == " + wrapped);

        // Math.addExact is the opt-in way to make the same overflow loud.
        try {
            Math.addExact(INT_MAX, 1);
            check("Math.addExact should throw", false, "no exception thrown");
        } catch (ArithmeticException e) {
            check("Math.addExact turns it into an error", true,
                  "ArithmeticException: " + e.getMessage());
        }

        long safe = (long) INT_MAX + 1L;
        checkEquals("the same sum in long is correct", 2147483648L, safe);
    }

    // ------------------------------------------------------------------
    // Scenario 3: an int loop counter can never pass Integer.MAX_VALUE,
    // so "for (int i = 2; i <= limit; i++)" runs forever.
    // Demonstrated safely with a bounded iteration cap.
    // ------------------------------------------------------------------
    private static void scenario3_intLoopCounterNeverTerminates() {
        section("3. int loop counter with limit = INT_MAX never exits");

        int limit = INT_MAX;
        int counter = INT_MAX - 2;
        int guard = 0;
        boolean wrappedToNegative = false;

        // The real loop would be: for (int i = 2; i <= limit; i++)
        // We start near the ceiling and cap iterations so the test terminates.
        while (counter <= limit && guard < 10) {
            counter++;
            guard++;
            if (counter < 0) {
                wrappedToNegative = true;
                break;
            }
        }

        check("counter wrapped past INT_MAX to a negative value",
              wrappedToNegative, "counter == " + counter);
        check("condition 'counter <= limit' is STILL true after wrapping",
              counter <= limit,
              counter + " <= " + limit + " is " + (counter <= limit) + " -> infinite loop");

        // long counters have no such problem at any limit a user could type.
        long longLimit = INT_MAX;
        long longCounter = (long) INT_MAX - 2;
        int steps = 0;
        while (longCounter <= longLimit && steps < 10) {
            longCounter++;
            steps++;
        }
        check("a long counter exits the same loop normally",
              longCounter > longLimit,
              "exited at counter == " + longCounter + " after " + steps + " steps");
    }

    // ------------------------------------------------------------------
    // Scenario 4: the divisor sum itself overflows and returns a WRONG
    // answer -- no crash, no warning, just a bad result.
    // ------------------------------------------------------------------
    private static void scenario4_intDivisorSumGivesWrongAnswer() {
        section("4. int divisor sum silently returns a wrong answer");

        // 735134400 = 2^6 * 3^3 * 5^2 * 7 * 11 * 13 * 17
        // It fits in an int, but the sum of its proper divisors does not.
        int n = 735134400;
        long trueSum = 3074621760L;

        check("the candidate itself fits in an int", n <= INT_MAX, "n == " + n);
        check("but its true divisor sum does NOT", trueSum > INT_MAX,
              "true sum == " + trueSum + ", int max == " + INT_MAX);

        int broken = intAliquotSum(n);
        long correct = longAliquotSum(n);

        checkEquals("long version computes the correct sum", trueSum, correct);
        checkEquals("int version wraps to a negative number", -1220345536, broken);
        check("the int result is wrong and gives no error at all",
              broken != trueSum, "int said " + broken + ", truth is " + trueSum);
    }

    // ------------------------------------------------------------------
    // Scenario 5: the payoff -- long finds perfect numbers int cannot hold.
    // ------------------------------------------------------------------
    private static void scenario5_longHandlesPerfectNumbersBeyondIntRange() {
        section("5. long reaches perfect numbers int cannot even represent");

        long fifth = 33550336L;      // fits in an int
        long sixth = 8589869056L;    // does NOT fit in an int

        check("5th perfect number fits in an int", fifth <= INT_MAX, "" + fifth);
        check("6th perfect number does NOT fit in an int", sixth > INT_MAX,
              sixth + " > " + INT_MAX);

        check("Main.isPerfect(33550336) is true", Main.isPerfect(fifth), "5th perfect number");
        check("Main.isPerfect(8589869056) is true", Main.isPerfect(sixth), "6th perfect number");
        check("Main.isPerfect(735134400) is false", Main.isPerfect(735134400L), "abundant, not perfect");
    }

    // ------------------------------------------------------------------
    // Scenario 6: honesty -- long moved the ceiling, it did not remove it.
    // ------------------------------------------------------------------ 
    private static void scenario6_longRaisesTheCeilingButDoesNotRemoveIt() {
        section("6. long raises the ceiling but does not remove it");

        long wrapped = Long.MAX_VALUE + 1;
        checkEquals("Long.MAX_VALUE + 1 wraps too", Long.MIN_VALUE, wrapped);

        try {
            Math.addExact(Long.MAX_VALUE, 1L);
            check("Math.addExact should throw for long as well", false, "no exception");
        } catch (ArithmeticException e) {
            check("Math.addExact catches long overflow too", true,
                  "ArithmeticException: " + e.getMessage());
        }

        double headroom = (double) Long.MAX_VALUE / (double) INT_MAX;
        check("long gives about 4.3 billion times more headroom than int",
              headroom > 4.2e9, String.format("factor == %.3e", headroom));
        System.out.println("      note: java.math.BigInteger removes the ceiling entirely,");
        System.out.println("            at the cost of much slower arithmetic.");
    }

    // ==================================================================
    // Divisor-sum implementations: identical logic, different data type.
    // ==================================================================

    /** Sum of proper divisors using int -- overflows above 2,147,483,647. */
    static int intAliquotSum(int n) {
        int sum = 1;
        for (int d = 2; d * d <= n; d++) {
            if (n % d != 0) {
                continue;
            }
            sum += d;
            int paired = n / d;
            if (paired != d) {
                sum += paired;
            }
        }
        return sum;
    }

    /** Identical logic using long -- the fix. */
    static long longAliquotSum(long n) {
        long sum = 1;
        for (long d = 2; d * d <= n; d++) {
            if (n % d != 0) {
                continue;
            }
            sum += d;
            long paired = n / d;
            if (paired != d) {
                sum += paired;
            }
        }
        return sum;
    }

    // ==================================================================
    // Minimal assertion helpers (no external test library needed).
    // ==================================================================

    private static void section(String title) {
        System.out.println();
        System.out.println("SCENARIO " + title);
    }

    private static void check(String name, boolean condition, String detail) {
        if (condition) {
            passed++;
            System.out.println("   [PASS] " + name);
        } else {
            failed++;
            System.out.println("   [FAIL] " + name);
        }
        System.out.println("          " + detail);
    }

    private static void checkEquals(String name, long expected, long actual) {
        check(name, expected == actual, "expected " + expected + ", got " + actual);
    }
}

