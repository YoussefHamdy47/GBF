export enum Grade {
  A_PLUS = "A+",
  A = "A",
  A_MINUS = "A-",
  B_PLUS = "B+",
  B = "B",
  B_MINUS = "B-",
  C_PLUS = "C+",
  C = "C",
  C_MINUS = "C-",
  D_PLUS = "D+",
  D = "D",
  F = "F",
  W = "Withdraw",
  P = "Pass",
}

export const gradeToGPA: { [key in Grade]: number } = {
  [Grade.A_PLUS]: 4.0,
  [Grade.A]: 4.0,
  [Grade.A_MINUS]: 3.7,
  [Grade.B_PLUS]: 3.3,
  [Grade.B]: 3.0,
  [Grade.B_MINUS]: 2.7,
  [Grade.C_PLUS]: 2.3,
  [Grade.C]: 2.0,
  [Grade.C_MINUS]: 1.7,
  [Grade.D_PLUS]: 1.3,
  [Grade.D]: 1.0,
  [Grade.F]: 0.0,
  [Grade.W]: 0.0,
  [Grade.P]: 0.0,
};

/**
 * Represents a subject that a student studies
 */
export interface Subject {
  /**
   * The name of the subject
   */
  subjectName: string;

  /**
   * The subject's code
   */
  subjectCode: string;

  /**
   * The number of times the subject has been studied
   */
  timesStudied: number;

  /**
   * The grade achieved in the subject
   * This field is optional
   */
  grade?: Grade;

  /**
   * The number of marks lost in the subject
   * This field is optional
   */
  marksLost?: number;

  /**
   * The number of credit hours for the subject
   * This field is optional
   */
  creditHours: number;
}

export function calculateGPA(subjects: Subject[]): number {
  let totalPoints = 0;
  let totalCreditHours = 0;

  for (const subject of subjects) {
    const { grade, creditHours } = subject;

    // Ensure grade and credit hours are defined
    if (grade && grade !== Grade.W && grade !== Grade.P && creditHours > 0) {
      // Calculate weighted points
      totalPoints += gradeToGPA[grade] * creditHours;
      totalCreditHours += creditHours;
    }
  }

  // Avoid division by zero
  return totalCreditHours === 0 ? 0 : totalPoints / totalCreditHours;
}
