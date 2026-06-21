package org.bunnys.nexus.timers.services;

import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import org.bunnys.database.models.timers.Account;
import org.bunnys.database.models.timers.Semester;
import org.bunnys.database.models.timers.TimerData;
import org.bunnys.database.models.user.BunnyUser;
import org.bunnys.handler.database.DB;
import org.bunnys.nexus.events.custom.AccountLevelUpEvent;
import org.bunnys.nexus.events.custom.RecordBrokenEvent;
import org.bunnys.nexus.timers.engine.LevelEngine;
import org.bunnys.utils.Utils;

public class TimerAccountService {

    public static void registerAccount(String userId) {
        BunnyUser existingUser = DB.findOne(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId));

        if (existingUser != null)
            throw new IllegalStateException("An account already exists for this user.");

        BunnyUser newUser = new BunnyUser();
        newUser.setUserID(userId);

        DB.save(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId), newUser);

        TimerData timerData = new TimerData();
        Account account = new Account();
        account.setUserID(userId);
        timerData.setAccount(account);

        DB.save(TimerData.class, "TimerData", Filters.eq("account.userID", userId), timerData);
    }

    public static void registerSemester(String userId, String semesterName) {
        if (semesterName == null || semesterName.trim().isEmpty())
            throw new IllegalArgumentException("Semester name cannot be empty.");

        TimerData timerData = DB.findOne(TimerData.class, "TimerData", Filters.eq("account.userID", userId));
        if (timerData == null)
            throw new IllegalStateException("No timer account found. Please register first.");

        if (timerData.getCurrentSemester() != null && timerData.getCurrentSemester().getSemesterName() != null) {
            throw new IllegalStateException("Semester '" + timerData.getCurrentSemester().getSemesterName()
                    + "' is already active. End it first.");
        }

        Semester newSemester = new Semester();
        newSemester.setSemesterName(semesterName);
        timerData.setCurrentSemester(newSemester);

        DB.save(TimerData.class, "TimerData", Filters.eq("account.userID", userId), timerData);
    }

    public static String endSemester(String userId, IReplyCallback interaction) {
        TimerData timerData = DB.findOne(TimerData.class, "TimerData", Filters.eq("account.userID", userId));
        BunnyUser userData = DB.findOne(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId));

        if (timerData == null || timerData.getCurrentSemester() == null
                || timerData.getCurrentSemester().getSemesterName() == null)
            throw new IllegalStateException("No active semester found to end.");

        Semester currentSemester = timerData.getCurrentSemester();

        // Check for longest semester record
        Semester longestSemester = timerData.getAccount().getLongestSemester();
        if (longestSemester == null || currentSemester.getSemesterTime() > longestSemester.getSemesterTime()) {
            timerData.getAccount().setLongestSemester(currentSemester);

            interaction.getJDA().getEventManager().handle(
                    new RecordBrokenEvent(
                            interaction.getJDA(),
                            interaction,
                            RecordBrokenEvent.RecordType.SEMESTER,
                            null,
                            currentSemester));
        }

        long convertedXP = LevelEngine.convertSeasonLevel(currentSemester.getSemesterLevel())
                + (long) currentSemester.getSemesterXP();

        String totalTimeStr = currentSemester.getSemesterTime() > 0
                ? Utils.msToTime((long) (currentSemester.getSemesterTime() * 1000)).orElse("0s")
                : "0s";
        String longestSessionStr = currentSemester.getLongestSession() > 0
                ? Utils.msToTime((long) (currentSemester.getLongestSession() * 1000)).orElse("0s")
                : "0s";
        String totalBreakTimeStr = currentSemester.getTotalBreakTime() > 0
                ? Utils.msToTime((long) (currentSemester.getTotalBreakTime() * 1000)).orElse("0s")
                : "0s";

        long semesterXP = (long) currentSemester.getSemesterXP();
        long totalSemesterXP = LevelEngine.calculateTotalSeasonXP(currentSemester.getSemesterLevel()) + semesterXP;

        StringBuilder recap = new StringBuilder();
        recap.append("**• Total Time:** ").append(totalTimeStr).append("\n")
                .append("**• Number of Sessions:** ").append(currentSemester.getSessionStartTimes().size()).append("\n")
                .append("**• Total Break Time:** ").append(totalBreakTimeStr).append("\n")
                .append("**• Longest Session:** ").append(longestSessionStr).append("\n")
                .append("**• Semester Level:** ").append(currentSemester.getSemesterLevel()).append("\n")
                .append("**• Semester XP:** ").append(String.format("%,d", totalSemesterXP)).append("\n")
                .append("**• Account XP Converted:** ").append(String.format("%,d", convertedXP));

        timerData.getAccount()
                .setLifetimeTime(timerData.getAccount().getLifetimeTime() + currentSemester.getSemesterTime());

        LevelEngine.RankResult rankCheck = LevelEngine.checkRank(userData.getRank(), userData.getRp(), convertedXP);
        if (rankCheck.hasRankedUp()) {
            userData.setRank(userData.getRank() + rankCheck.addedLevels());
            userData.setRp((int) rankCheck.remainingRP());
            recap.append("\n🎉 **RANK UP!** You are now Rank ").append(userData.getRank());

            interaction.getJDA().getEventManager().handle(new AccountLevelUpEvent(
                    interaction.getJDA(),
                    interaction,
                    rankCheck.addedLevels(),
                    rankCheck.remainingRP(),
                    userData));
        } else
            userData.setRp(userData.getRp() + (int) convertedXP);

        timerData.setCurrentSemester(new Semester());

        DB.save(BunnyUser.class, "BunnyUsers", Filters.eq("userID", userId), userData);
        DB.save(TimerData.class, "TimerData", Filters.eq("account.userID", userId), timerData);

        return recap.toString();
    }
}