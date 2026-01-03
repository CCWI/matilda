package edu.hm.ccwi.matilda.korpus.service;

import edu.hm.ccwi.matilda.korpus.model.GeneralKorpusResults;

public interface KorpusService {

    GeneralKorpusResults analyzeGeneralStats();

    String qsAnalyses();
}
