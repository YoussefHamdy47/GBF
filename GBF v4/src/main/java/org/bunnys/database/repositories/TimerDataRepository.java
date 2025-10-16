package org.bunnys.database.repositories;

import org.bunnys.database.models.timer.TimerData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimerDataRepository extends MongoRepository<TimerData, String> {
}
