package org.bunnys.executors.timer.engine;

import org.bunnys.database.models.timer.Subject;
import org.bunnys.database.models.users.GBFUser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * <p>
 * A robust and thread-safe utility class for calculating Grade Point Averages
 * (GPA)
 * and managing academic grade data. This class provides static methods to
 * handle
 * GPA calculations with high precision using {@link BigDecimal}, ensuring
 * accuracy
 * for financial and academic systems where fractional values are critical.
 * </p>
 *
 * <p>
 * The design of {@code GradeEngine} emphasizes immutability and functional
 * programming
 * principles where possible, particularly in its public API, to prevent side
 * effects
 * and ensure predictable behavior.
 * The class is {@code final} to prevent extension
 * and has a private constructor to enforce its use as a static utility class.
 * </p>
 */
@SuppressWarnings("unused")
public final class GradeEngine {
    // Grades that don't count towards GPA calculation
    private static final Set<Grade> NON_GPA_GRADES = EnumSet.of(Grade.W, Grade.P);

    /**
     * <p>
     * Represents the standard letter grades with their corresponding GPA values.
     * Each grade is defined with a display name and a precise {@link BigDecimal}
     * value for calculations.
     * </p>
     *
     * <p>
     * This enum is a cornerstone of the GPA calculation logic, providing
     * a single source of truth for grade-to-point mappings.
     * It also includes
     * a static factory method for parsing string representations of grades,
     * which handles various formats robustly.
     * </p>
     */
    public enum Grade {
        A_PLUS("A+", 4.0),
        A("A", 4.0),
        A_MINUS("A-", 3.7),
        B_PLUS("B+", 3.3),
        B("B", 3.0),
        B_MINUS("B-", 2.7),
        C_PLUS("C+", 2.3),
        C("C", 2.0),
        C_MINUS("C-", 1.7),
        D_PLUS("D+", 1.3),
        D("D", 1.0),
        F("F", 0.0),
        W("Withdraw", 0.0),
        P("Pass", 0.0);

        private final String displayName;
        private final BigDecimal gpaValue;

        Grade(String displayName, double gpaValue) {
            this.displayName = displayName;
            this.gpaValue = BigDecimal.valueOf(gpaValue);
        }

        /**
         * Returns the GPA value associated with this grade.
         *
         * @return A {@link BigDecimal} representing the GPA value.
         */
        public BigDecimal getGPAValue() {
            return gpaValue;
        }

        /**
         * Returns the human-readable display name of the grade.
         *
         * @return The display name string (e.g., "A+", "B-").
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * <p>
         * Parses a string representation of a grade and returns the corresponding
         * {@link Grade} enum.
         * This method is case-insensitive and can handle various common formats,
         * including those with hyphens or plus signs.
         * </p>
         *
         * @param gradeStr The string to parse, e.g., "A", "a+", "B-".
         * @return An {@link Optional} containing the parsed {@link Grade} if a match is
         *         found,
         *         or {@link Optional#empty()} otherwise.
         */
        public static Optional<Grade> fromString(String gradeStr) {
            if (gradeStr == null || gradeStr.trim().isEmpty())
                return Optional.empty();

            String normalized = gradeStr.trim().toUpperCase();
            for (Grade grade : Grade.values())
                if (grade.name().replace("_", "").equals(normalized.replace("-", "").replace("+", "PLUS")) ||
                        grade.displayName.equals(gradeStr.trim()))
                    return Optional.of(grade);

            return Optional.empty();
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * <p>
     * A simple, immutable record that encapsulates the result of a GPA calculation.
     * This record provides the calculated GPA, total credit hours, and total
     * quality points,
     * all essential components for reporting and further academic calculations.
     * </p>
     *
     * <p>
     * Using a record ensures that the data is immutable and self-describing,
     * promoting clean code and reliable data transfer.
     * </p>
     *
     * @param gpa              The calculated GPA as a {@link BigDecimal}.
     * @param totalCreditHours The sum of credit hours for all subjects included in
     *                         the calculation.
     * @param qualityPoints    The sum of quality points for all subjects.
     */
    public record GpaResult(BigDecimal gpa, int totalCreditHours, int qualityPoints) {

        /**
         * Returns the GPA value as a {@code double} for convenience.
         *
         * @return The GPA as a primitive double.
         */
        public double getGpaAsDouble() {
            return gpa.doubleValue();
        }

        /**
         * Checks if the GPA is valid, which is true if at least one credit hour was
         * included in the calculation.
         *
         * @return {@code true} if the total credit hours are greater than 0, otherwise
         *         {@code false}.
         */
        public boolean hasValidGpa() {
            return totalCreditHours > 0;
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private GradeEngine() {
    }

    /**
     * <p>
     * Calculates the Grade Point Average (GPA) from a list of {@link Subject}
     * objects.
     * This method uses {@link BigDecimal} for all calculations to maintain
     * precision
     * and prevent floating-point inaccuracies. The GPA is rounded to three decimal
     * places
     * using {@link RoundingMode#HALF_UP}.
     * </p>
     *
     * <p>
     * Subjects with non-GPA grades (e.g., "W" or "P") or zero credit hours are
     * automatically excluded from the calculation.
     * </p>
     *
     * @param subjects A {@link List} of {@link Subject} objects to calculate the
     *                 GPA for.
     *                 Must not be {@code null}.
     * @return A {@link GpaResult} containing the calculated GPA, total credit
     *         hours, and quality points.
     * @throws IllegalArgumentException if the {@code subjects} list is
     *                                  {@code null}.
     */
    public static GpaResult calculateGPA(List<Subject> subjects) {
        if (subjects == null)
            throw new IllegalArgumentException("Subjects list cannot be null");

        BigDecimal totalQualityPoints = BigDecimal.ZERO;
        int totalCreditHours = 0;

        for (Subject subject : subjects) {
            if (!isValidForGpaCalculation(subject))
                continue;

            BigDecimal gradePoints = subject.getGrade().getGPAValue();
            BigDecimal creditHours = BigDecimal.valueOf(subject.getCreditHours());
            BigDecimal qualityPoints = gradePoints.multiply(creditHours);

            totalQualityPoints = totalQualityPoints.add(qualityPoints);
            totalCreditHours += subject.getCreditHours();
        }

        BigDecimal gpa = totalCreditHours == 0
                ? BigDecimal.ZERO
                : totalQualityPoints.divide(BigDecimal.valueOf(totalCreditHours), 3, RoundingMode.HALF_UP);

        return new GpaResult(gpa, totalCreditHours, totalQualityPoints.intValue());
    }

    /**
     * <p>
     * Calculates the GPA with a user-specified rounding precision. This method
     * delegates to the standard {@link #calculateGPA(List)} method and then
     * rounds the resulting GPA to the specified number of decimal places.
     * </p>
     *
     * @param subjects      A {@link List} of {@link Subject} objects. Must not be
     *                      {@code null}.
     * @param decimalPlaces The number of decimal places to round the GPA to. Must
     *                      be
     *                      between 0 and 10, inclusive.
     * @return A {@link GpaResult} with the rounded GPA and other metadata.
     * @throws IllegalArgumentException if {@code subjects} is {@code null} or if
     *                                  {@code decimalPlaces} is outside the valid
     *                                  range [0, 10].
     */
    public static GpaResult calculateGPA(List<Subject> subjects, int decimalPlaces) {
        if (decimalPlaces < 0 || decimalPlaces > 10)
            throw new IllegalArgumentException("Decimal places must be between 0 and 10");

        GpaResult result = calculateGPA(subjects);
        BigDecimal roundedGpa = result.gpa().setScale(decimalPlaces, RoundingMode.HALF_UP);

        return new GpaResult(roundedGpa, result.totalCreditHours(), result.qualityPoints());
    }

    /**
     * <p>
     * Calculates the GPA from a list of {@link GBFUser.Subject} objects. This is a
     * convenience method for integrating with the {@code GBFUser} data model. It
     * streams the input list, converts each item to a {@link Subject} object, and
     * then
     * delegates the calculation to {@link #calculateGPA(List)}.
     * </p>
     *
     * <p>
     * Subjects with unrecognized or invalid grade strings are silently ignored,
     * ensuring the calculation is not disrupted by malformed data.
     * </p>
     *
     * @param subjects A {@link List} of {@link GBFUser.Subject} objects. Must not
     *                 be {@code null}.
     * @return A {@link GpaResult} containing the calculated GPA.
     * @throws IllegalArgumentException if the {@code subjects} list is
     *                                  {@code null}.
     */
    public static GpaResult calculateGPAFromGBF(List<GBFUser.Subject> subjects) {
        if (subjects == null)
            throw new IllegalArgumentException("Subjects list cannot be null");

        List<Subject> converted = subjects.stream()
                .map(s -> Grade.fromString(s.getGrade())
                        .map(g -> {
                            Subject subj = new Subject();
                            subj.setSubjectName(s.getSubjectName());
                            subj.setSubjectCode(s.getSubjectCode());
                            subj.setCreditHours(s.getCreditHours());
                            subj.setGrade(g);
                            return subj;
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();

        return calculateGPA(converted);
    }

    /**
     * A private helper method to determine if a given subject should be included in
     * GPA calculation.
     * It checks for {@code null} subjects, {@code null} grades, non-GPA grades, and
     * subjects with zero
     * credit hours.
     *
     * @param subject The {@link Subject} to validate.
     * @return {@code true} if the subject is valid for GPA calculation, otherwise
     *         {@code false}.
     */
    private static boolean isValidForGpaCalculation(Subject subject) {
        return subject != null
                && subject.getGrade() != null
                && !NON_GPA_GRADES.contains(subject.getGrade())
                && subject.getCreditHours() > 0;
    }

    /**
     * <p>
     * Provides a human-readable classification for a given letter grade.
     * This method is useful for displaying qualitative feedback based on a
     * student's performance.
     * </p>
     *
     * @param grade The {@link Grade} enum to classify. Can be {@code null}.
     * @return A {@link String} representing the grade classification (e.g.,
     *         "Excellent", "Failing"),
     *         or "Unknown" if the input grade is {@code null}.
     */
    public static String getGradeClassification(Grade grade) {
        if (grade == null)
            return "Unknown";

        return switch (grade) {
            case A_PLUS, A, A_MINUS -> "Excellent";
            case B_PLUS, B, B_MINUS -> "Good";
            case C_PLUS, C, C_MINUS -> "Satisfactory";
            case D_PLUS, D -> "Below Average";
            case F -> "Failing";
            case W -> "Withdrawn";
            case P -> "Pass";
        };
    }
}