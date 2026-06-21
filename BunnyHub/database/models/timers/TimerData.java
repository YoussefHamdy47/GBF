package org.bunnys.database.models.timers;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

public class TimerData {
    @BsonId
    private ObjectId id;

    private Account account;
    private Semester currentSemester = new Semester();
    private Session sessionData = new Session();

    public TimerData() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
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
}