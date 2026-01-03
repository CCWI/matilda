package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.recomcard.ResultCard;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ResultCardRepository extends MongoRepository<ResultCard, String> {

    default ResultCard getResultCardByMatildaId(String matildaId) {
        if (this.existsById(matildaId)) {
            Optional<ResultCard> resultCardOpt = this.findById(matildaId);
            if (resultCardOpt.isPresent()) {
                return resultCardOpt.get();
            }
        }
        return null;
    }

}


