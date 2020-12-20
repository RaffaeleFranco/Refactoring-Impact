package com.group;

import com.group.csv.CSVService;
import com.group.csv.Smell;
import com.group.pojo.Commit;
import com.group.pojo.InfoCommit;
import com.group.pojo.ProcessResult;
import com.group.worker.DesigniteWorker;
import com.group.worker.RefactoringMinerWorker;
import com.group.worker.SonarQubeWorker;

import java.util.*;

public class Process {

    static final String RESULTS_PROCESS_FILENAME = "datasets.csv";

    public static void main(String[] args) throws Exception {

        // region Load Configurations
        Configuration conf = Configuration.getInstance();
        String repoDir = conf.getRepoDir();
        boolean refactoringMinerDetectBetweenCommits = conf.isRefactoringMinerDetectBetweenCommits();
        String refactoringMinerStartCommitId = conf.getRefactoringMinerStartCommitId();
        String refactoringMinerEndCommitId = conf.getRefactoringMinerEndCommitId();
        boolean writeRefactoringMinerOutputOnFile = conf.isWriteRefactoringMinerOutputOnFile();
        String designiteDir = conf.getDesigniteDir();
        String sonarQubeServerBaseUrl = conf.getSonarQubeServerBaseUrl();
        String sonarQubeScannerBinDir = conf.getSonarQubeScannerBinDir();
        String resultsDir = conf.getResultsDir();
        // endregion

        List<ProcessResult> resultList = new ArrayList<>();

        RefactoringMinerWorker refactoringMinerWorker =
                new RefactoringMinerWorker(repoDir, resultsDir, writeRefactoringMinerOutputOnFile);
        DesigniteWorker designiteWorker = new DesigniteWorker(designiteDir, repoDir, resultsDir);
        SonarQubeWorker sonarQubeWorker = new SonarQubeWorker(sonarQubeServerBaseUrl, sonarQubeScannerBinDir,repoDir);

        System.out.println("<Start process>");

        ArrayList<Commit> commitList;
        if (refactoringMinerDetectBetweenCommits) {
            commitList = refactoringMinerWorker.getRefactoringsForCommitsWithRange(
                    refactoringMinerStartCommitId, refactoringMinerEndCommitId);
        } else {
            commitList = refactoringMinerWorker.getRefactoringsForCommits();
        }

        for (Commit commit : commitList) {

            String commitHashId = commit.getHash();
            String previousCommitHashId = refactoringMinerWorker.checkoutPreviousCommit(commitHashId);

            if (previousCommitHashId != null) {

                List<Smell> smellListPreviousCommit = designiteWorker.execute(previousCommitHashId);

                if (smellListPreviousCommit.size() > 0) {

                    refactoringMinerWorker.checkoutToCommit(commitHashId);
                    InfoCommit infoCommit = refactoringMinerWorker.getInformationCommit(commitHashId);
                    List<Smell> smellListActualCommit = designiteWorker.execute(commitHashId);

                    for (Smell s0 : smellListPreviousCommit) {
                        ProcessResult pr = new ProcessResult();
                        pr.setCommitHash(commitHashId);
                        pr.setClassName(s0.getClassName());
                        pr.setMethodName(s0.getMethodName());
                        pr.setCommitterName(infoCommit.getAuthor());
                        pr.setCommitterEmail(infoCommit.getEmail());
                        pr.setSmellType(s0.getCodeSmell());
                        if (smellListActualCommit.contains(s0)) {
                            pr.setSmellRemoved(false);
                        } else {
                            pr.setSmellRemoved(true);
                            // TODO s0 smell resolved: investigate what the reference refactoring type is
                            // TODO run sonar-scanner only once (at most) per commit
                            sonarQubeWorker.executeScanning(commitHashId);
                        }
                        resultList.add(pr);
                    }
                }
            }
            System.out.println("-----------------------------------------");
        }
        System.out.println("Generating " + RESULTS_PROCESS_FILENAME);
        CSVService.writeCsvFile(resultsDir + "\\" + RESULTS_PROCESS_FILENAME, resultList, ProcessResult.class);
        System.out.println("Process finished!");
    }
}
