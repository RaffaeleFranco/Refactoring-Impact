package com.group.worker;

import com.group.Utils;
import com.group.csv.CSVService;
import com.group.csv.Refactoring;
import com.group.pojo.InfoCommit;
import org.apache.log4j.Logger;
import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.ProcResult;
import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import com.group.pojo.Commit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefactoringMinerWorker {

    private static final Logger logger = Logger.getLogger(RefactoringMinerWorker.class);
    private static final String REFACTORING_TYPE_FOUND_FILENAME = "refactoringFound.csv";

    private final GitService gitService = new GitServiceImpl();
    private final GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
    private final Repository repo;
    private final String resultsDir;
    private final boolean writeOutputOnFile;

    public RefactoringMinerWorker(String repoDir, String resultsDir, boolean writeOutputOnFile) throws Exception {
        repo = gitService.openRepository(repoDir);
        this.resultsDir = resultsDir;
        this.writeOutputOnFile = writeOutputOnFile;
    }

    public RefactoringMinerWorker(String pathToDirectory, String repoUrl, String resultsDir, boolean writeOutputOnFile) throws Exception {
        repo = gitService.cloneIfNotExists(pathToDirectory, repoUrl);
        this.resultsDir = resultsDir;
        this.writeOutputOnFile = writeOutputOnFile;
    }

    public ArrayList<Commit> getRefactoringsForCommits() throws Exception {
        return getRefactoringsForCommits("main");
    }

    public ArrayList<Commit> getRefactoringsForCommits(String branch) throws Exception {
        logger.info("Run Refactoring Miner...");
        ArrayList<Commit> commitArrayList = new ArrayList<>();
        miner.detectAll(repo, branch, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<org.refactoringminer.api.Refactoring> refactorings) {
                if (refactorings.size() > 0) {
                    commitArrayList.add(new Commit(commitId, refactorings));
                }
            }
        });
        logger.info("Refactoring Miner Done!");
        if (this.writeOutputOnFile) {
            parseCommitArrayListAndWriteOnFile(commitArrayList);
        }
        return commitArrayList;
    }

    public ArrayList<Commit> getRefactoringsForCommitsWithRange(String startCommitId, String endCommitId) throws Exception {
        logger.info("Run Refactoring Miner with detection between commits...");
        ArrayList<Commit> commitArrayList = new ArrayList<>();
        miner.detectBetweenCommits(repo, startCommitId, endCommitId, new RefactoringHandler() {
            @Override
            public void handle(String commitId, List<org.refactoringminer.api.Refactoring> refactorings) {
                if (refactorings.size() > 0) {
                    commitArrayList.add(new Commit(commitId, refactorings));
                }
            }
        });
        logger.info("Refactoring Miner Done!");
        if (this.writeOutputOnFile) {
            parseCommitArrayListAndWriteOnFile(commitArrayList);
        }
        return commitArrayList;
    }

    public String checkoutPreviousCommit(String commitHashId) {

        logger.info("Commit Hash: " + commitHashId);

        ProcResult result = new ProcBuilder("git")
                .withWorkingDirectory(new File(repo.getDirectory().getParent()))
                .withArg("rev-list")
                .withArg("--parents")
                .withArg("-n")
                .withArg("1")
                .withArg(commitHashId)
                .withNoTimeout()
                .run();

        Pattern pattern = Pattern.compile(" (.*)");
        Matcher matcher = pattern.matcher(result.getOutputString().trim());
        String previousCommit;

        if(matcher.find()) {
            previousCommit = matcher.group(1);
            checkoutToCommit(previousCommit);
            return previousCommit;
        }

        logger.info("Previous Commit hash not found: " + commitHashId);
        return null;
    }

    public boolean checkoutToCommit(String commitHashId) {
        ProcResult procResult = new ProcBuilder("git")
                .withWorkingDirectory(new File(repo.getDirectory().getParent()))
                .withArg("checkout")
                .withArg("-f")
                .withArg(commitHashId)
                .withNoTimeout()
                .run();

        logger.info("Checkout to commit hash: " + commitHashId);
        return true;
    }

    public InfoCommit getInformationCommit(String commitHashId) {
        ProcResult procResult = new ProcBuilder("git")
                .withWorkingDirectory(new File(repo.getDirectory().getParent()))
                .withArg("log")
                .withArg("--pretty=format:\"Commit:%n%H%n%cn%n%ce\"")
                .withArg(commitHashId)
                .withNoTimeout()
                .run();


        return parseInfoCommit(procResult);
    }

    private static InfoCommit parseInfoCommit(ProcResult procResult) {
        InfoCommit infoCommit = null;

        String[] results =  procResult.getOutputString() != null ? procResult.getOutputString().split("\n") : null;
        if (results != null && results.length > 3) {
            infoCommit = new InfoCommit(results[1], results[2], results[3]);
        }

        return infoCommit;
    }

    private void parseCommitArrayListAndWriteOnFile(List<Commit> commitList) {
        logger.info("Generating " + REFACTORING_TYPE_FOUND_FILENAME);
        List<Refactoring> refactoringList = new ArrayList<Refactoring>();
        for (Commit c : commitList) {
            for (org.refactoringminer.api.Refactoring r : c.getRefactoringList()) {
                refactoringList.add(
                        new Refactoring(
                                c.getHash(),
                                r.getRefactoringType().getDisplayName(),
                                r.toString().replace('\t', ' '))
                );
            }
        }
        CSVService.writeCsvFile(
                Utils.preparePathOsBased(false, resultsDir, REFACTORING_TYPE_FOUND_FILENAME),
                refactoringList,
                Refactoring.class);
    }

    private List<org.refactoringminer.api.Refactoring> getFilteredRefactorings(List<org.refactoringminer.api.Refactoring> refactorings) {
        List<org.refactoringminer.api.Refactoring> refactoringList = new ArrayList<>();
        for (org.refactoringminer.api.Refactoring r : refactorings) {
            if (Utils.refactoringsConsidered.containsKey(r.getRefactoringType())) {
                refactoringList.add(r);
            }
        }
        return refactoringList;
    }
}