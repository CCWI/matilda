package edu.hm.ccwi.matilda.korpus;

import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DependencyClassificationEvaluatorTest {

    @Test
    void mapMvnRepoLabelsToMatildaLabels() throws Exception {
        GACategoryTag gact = new GACategoryTag("a", "b", "Cache Clients", new ArrayList<>(), false);
        List<GACategoryTag> gaCategoryTagList = new ArrayList<>();
        gaCategoryTagList.add(gact);
        DependencyClassificationEvaluator.mapMvnRepoLabelsToMatildaLabels(gaCategoryTagList);

        assertThat(gact.getCategory()).isEqualTo("Caching");
    }
}