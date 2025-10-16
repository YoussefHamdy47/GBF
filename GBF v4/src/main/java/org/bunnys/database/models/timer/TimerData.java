package org.bunnys.database.models.timer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@SuppressWarnings("unused")
@Document(collection = "Timer Data")
public class TimerData {

    @Id
    private String id;

    private Account account;
    private Semester currentSemester;
    private Session sessionData;

    public TimerData() {
    }

    public TimerData(Account account, Semester currentSemester, Session sessionData) {
        this.account = account;
        this.currentSemester = currentSemester;
        this.sessionData = sessionData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Semester getCurrentSemester() {
        return currentSemester;
    }

    public void setCurrentSemester(Semester currentSemester) {
        this.currentSemester = currentSemester;
    }

    public Session getSessionData() {
        return sessionData;
    }

    public void setSessionData(Session sessionData) {
        this.sessionData = sessionData;
    }

    @Override
    public String toString() {
        return "TimerData{" +
                "id='" + id + '\'' +
                ", account=" + account +
                ", currentSemester=" + currentSemester +
                ", sessionData=" + sessionData +
                '}';
    }
}