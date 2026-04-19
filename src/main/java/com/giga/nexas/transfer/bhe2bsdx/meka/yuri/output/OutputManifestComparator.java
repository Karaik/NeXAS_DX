package com.giga.nexas.transfer.bhe2bsdx.meka.yuri.output;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


public class OutputManifestComparator {

    public ComparisonResult compareDirectories(Path expectedRoot, Path actualRoot, boolean compareBytes) {
        if (expectedRoot == null || actualRoot == null) {
            throw new IllegalArgumentException("比较目录不能为空");
        }

        try {
            Set<String> expectedFiles = collectRelativeFiles(expectedRoot);
            Set<String> actualFiles = collectRelativeFiles(actualRoot);
            ComparisonResult result = new ComparisonResult();

            for (String expected : expectedFiles) {
                if (!actualFiles.contains(expected)) {
                    result.getMissingFiles().add(expected);
                }
            }
            for (String actual : actualFiles) {
                if (!expectedFiles.contains(actual)) {
                    result.getExtraFiles().add(actual);
                }
            }
            if (compareBytes) {
                for (String relativePath : expectedFiles) {
                    if (!actualFiles.contains(relativePath)) {
                        continue;
                    }
                    if (!bytesEqual(expectedRoot.resolve(relativePath), actualRoot.resolve(relativePath))) {
                        result.getDifferentFiles().add(relativePath);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("比较输出目录失败", e);
        }
    }

    public Set<String> manifestFileSet(OutputManifest manifest) {
        Set<String> fileSet = new LinkedHashSet<>();
        if (manifest == null || manifest.getEntries() == null) {
            return fileSet;
        }
        manifest.getEntries().stream()
                .map(OutputResourceEntry::getRelativePath)
                .filter(path -> path != null && !path.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(fileSet::add);
        return fileSet;
    }

    public Set<String> collectRelativeFiles(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            Set<String> files = new LinkedHashSet<>();
            stream.filter(Files::isRegularFile)
                    .map(path -> OutputManifestSupport.relativePath(root, path))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(files::add);
            return files;
        } catch (IOException e) {
            throw new IllegalStateException("收集输出目录文件失败: " + root, e);
        }
    }

    private boolean bytesEqual(Path expected, Path actual) throws IOException {
        if (Files.size(expected) != Files.size(actual)) {
            return false;
        }
        return Files.mismatch(expected, actual) < 0;
    }

    @Data
    public static class ComparisonResult {

        
        private List<String> missingFiles = new ArrayList<>();

        
        private List<String> extraFiles = new ArrayList<>();

        
        private List<String> differentFiles = new ArrayList<>();

        public boolean isIdentical() {
            return missingFiles.isEmpty() && extraFiles.isEmpty() && differentFiles.isEmpty();
        }

        public List<String> allDifferences() {
            List<String> differences = new ArrayList<>();
            missingFiles.stream()
                    .sorted(Comparator.naturalOrder())
                    .map(path -> "missing: " + path)
                    .forEach(differences::add);
            extraFiles.stream()
                    .sorted(Comparator.naturalOrder())
                    .map(path -> "extra: " + path)
                    .forEach(differences::add);
            differentFiles.stream()
                    .sorted(Comparator.naturalOrder())
                    .map(path -> "different: " + path)
                    .forEach(differences::add);
            return differences;
        }
    }
}
