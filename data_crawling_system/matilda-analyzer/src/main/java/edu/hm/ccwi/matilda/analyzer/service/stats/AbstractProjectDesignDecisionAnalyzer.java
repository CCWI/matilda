package edu.hm.ccwi.matilda.analyzer.service.stats;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AbstractProjectDesignDecisionAnalyzer {

    int getSummaryInRange(int[][] mapOfCommitsAndMigrations, int min, int max) {
        int sum = 0;
        for (int[] commitsAndMigration : mapOfCommitsAndMigrations) {
            for (int i = 0; i < commitsAndMigration.length; i++) {
                if(i >= min && i <= max) {
                    sum += commitsAndMigration[i];
                }
            }
        }
        return sum;
    }

    int getMaxDDCount(List<ExtractedDesignDecisionEntity> ddList) {
        Map<String, Long> ddCountMap = ddList.stream()
                .filter(distinctByKey(x -> x.getRepository() + x.getProject() + x.getInitial() + x.getTarget() + x.getDecisionSubject()))
                .collect(Collectors.groupingBy(x -> x.getRepository() + x.getProject(), Collectors.counting()));

        int max = 0;
        for (Map.Entry<String, Long> entry : ddCountMap.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue().intValue();
            }
        }
        return max;
    }

    Set<String> findUniqueMigrationDecisionsForRepository(List<ExtractedDesignDecisionEntity> ddList,
                                                          String subjectToFilter,
                                                          CrawledRepository crawledRepo) {
        Set<String> uniqueMigrationDecision = new HashSet<>();
        for (ExtractedDesignDecisionEntity dd : ddList) {
            if(StringUtils.equals(dd.getRepository(), crawledRepo.getRepositoryName()) &&
                    StringUtils.equals(dd.getProject(), crawledRepo.getProjectName()) && isMigration(dd) &&
                    !uniqueMigrationDecision.contains(dd.getRepository() + dd.getProject() + dd.getInitial() + dd.getTarget())) {
                if(subjectToFilter == null || StringUtils.equals(dd.getDecisionSubject(), subjectToFilter)) {
                    uniqueMigrationDecision.add(dd.getRepository() + dd.getProject() + dd.getInitial() + dd.getTarget());
                }
            }
        }
        return uniqueMigrationDecision;
    }

    Set<String> findUniqueMigrationDecisionsForRepository(List<ExtractedDesignDecisionEntity> ddList, CrawledRepository crawledRepo) {
        Set<String> uniqueMigrationDecision = new HashSet<>();
        for (ExtractedDesignDecisionEntity dd : ddList) {
            if(dd.getInitial() != null && dd.getTarget() != null) {
                if (StringUtils.equals(dd.getRepository(), crawledRepo.getRepositoryName()) &&
                        StringUtils.equals(dd.getProject(), crawledRepo.getProjectName()) && isMigration(dd) &&
                        !uniqueMigrationDecision.contains(dd.getRepository() + dd.getProject() + dd.getInitial() + dd.getTarget())) {
                    uniqueMigrationDecision.add(dd.getRepository() + dd.getProject() + dd.getInitial() + dd.getTarget());
                }
            }
        }
        return uniqueMigrationDecision;
    }

    Set<String> retrieveAllSubjects(List<ExtractedDesignDecisionEntity> ddList) {
        Set<String> ddSubjectSet = new HashSet<>();
        for (ExtractedDesignDecisionEntity eDD : ddList) {
            ddSubjectSet.add(eDD.getDecisionSubject());
        }
        return ddSubjectSet;
    }

    private boolean isMigration(ExtractedDesignDecisionEntity dd) {
        return dd.getTarget() != null && dd.getInitial() != null;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
