package com.giga.nexas.transfer.jinki2bsdx.v2.output;

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

/**
 * 输出目录/解包目录的最终验收比较器。
 *
 * <p>比较粒度刻意使用相对路径，而不是文件名：
 * 最终打包对象里路径层级也是资源身份的一部分，目录层级变了即使文件名相同也算失败。</p>
 */
public class OutputManifestComparator {

    public ComparisonResult compareDirectories(Path expectedRoot, Path actualRoot, boolean compareBytes) {
        if (expectedRoot == null || actualRoot == null) {
            throw new IllegalArgumentException("比较目录不能为空");
        }

        try {
            // 先比较集合，再比较内容。
            // 集合差异比 byte 差异更优先定位：少/多文件通常说明输出规则错了，
            // 同名 byte 不同则说明某个 step 的生成逻辑或序列化逻辑错了。
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
                    // 统一成 /，避免 Windows 路径分隔符污染最终 diff。
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

        /**
         * expectedRoot 中存在、actualRoot 中缺失的相对路径。
         *
         * <p>这类差异通常说明 V2 少复制了 sidecar、PNG、音频或某个生成文件。</p>
         */
        private List<String> missingFiles = new ArrayList<>();

        /**
         * actualRoot 中额外多出的相对路径。
         *
         * <p>这类差异通常说明 V2 输出集合扩大了，例如多复制了整套 SPM imageData 图。</p>
         */
        private List<String> extraFiles = new ArrayList<>();

        /**
         * 两边都有同名相对路径，但文件 bytes 不一致的路径。
         *
         * <p>这类差异说明输出集合对了，但某个生成/patch/序列化逻辑仍和旧 pipeline 不一致。</p>
         */
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
