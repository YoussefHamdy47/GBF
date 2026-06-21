package org.bunnys.nexus.timers.services;

import com.mongodb.client.model.Filters;
import org.bunnys.database.models.timers.Subject;
import org.bunnys.database.models.timers.TimerData;
import org.bunnys.database.models.user.BunnyUser;
import org.bunnys.handler.database.DB;

public class TimerSubjectService {

    public static void addSubjectToAccount(String userId, Subject subject) {
        BunnyUser user = DB.findOne(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId));
        if (user == null)
            throw new IllegalStateException("User account not found.");

        boolean exists = user.getSubjects().stream()
                .anyMatch(s -> s.getSubjectCode().equalsIgnoreCase(subject.getSubjectCode()));

        if (exists)
            throw new IllegalArgumentException(
                    "Subject '" + subject.getSubjectCode() + "' is already registered in your account.");

        user.getSubjects().add(subject);
        DB.save(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId), user);
    }

    public static void removeSubjectFromAccount(String userId, String subjectCode) {
        BunnyUser user = DB.findOne(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId));
        if (user == null)
            throw new IllegalStateException("User account not found.");

        boolean removed = user.getSubjects().removeIf(s -> s.getSubjectCode().equalsIgnoreCase(subjectCode.trim()));

        if (!removed)
            throw new IllegalArgumentException("You haven't registered '" + subjectCode + "' in your account.");

        DB.save(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId), user);
    }

    public static void addSubjectToSemester(String userId, Subject subject) {
        TimerData timerData = DB.findOne(TimerData.class, "TimerData", Filters.eq("account.userID", userId));
        if (timerData == null || timerData.getCurrentSemester() == null)
            throw new IllegalStateException("No active semester found.");

        boolean exists = timerData.getCurrentSemester().getSemesterSubjects().stream()
                .anyMatch(s -> s.getSubjectCode().equalsIgnoreCase(subject.getSubjectCode()));

        if (exists)
            throw new IllegalArgumentException(
                    "Subject '" + subject.getSubjectCode() + "' is already in the current semester.");

        timerData.getCurrentSemester().getSemesterSubjects().add(subject);
        DB.save(TimerData.class, "TimerData", Filters.eq("account.userID", userId), timerData);
    }

    public static void removeSubjectFromSemester(String userId, String subjectCode) {
        TimerData timerData = DB.findOne(TimerData.class, "TimerData", Filters.eq("account.userID", userId));
        if (timerData == null || timerData.getCurrentSemester() == null)
            throw new IllegalStateException("No active semester found.");

        boolean removed = timerData.getCurrentSemester().getSemesterSubjects()
                .removeIf(s -> s.getSubjectCode().equalsIgnoreCase(subjectCode.trim()));

        if (!removed)
            throw new IllegalArgumentException("You haven't registered '" + subjectCode + "' for this semester.");

        DB.save(TimerData.class, "TimerData", Filters.eq("account.userID", userId), timerData);
    }
}