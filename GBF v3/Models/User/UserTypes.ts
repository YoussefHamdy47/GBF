import { Subject } from "../../GBF/Timers/GradeEngine";

interface GBFUser {
  userID: string;
  friends: string[];
  privateProfile: boolean;
  Rank: number;
  RP: number;
  Subjects: Subject[];
}

export { GBFUser };
