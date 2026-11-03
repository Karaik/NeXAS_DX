package com.giga.nexas.bhe2bsdx.byJinki;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.*;

/** Text-only policy fixtures. Tests deliberately stay under this checkout's target, never OS temp/C. */
class ManagedPlayableBuildTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private Path sandbox, project;
    private final List<Path> links = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private String fixtureOwner;

    @BeforeEach
    void fixture() throws Exception {
        Path checkout = Path.of("").toAbsolutePath().normalize().toRealPath();
        Path target = checkout.resolve("target");
        Files.createDirectories(target);
        sandbox = Files.createTempDirectory(target, "managed-build-policy-");
        fixtureOwner = UUID.randomUUID().toString();
        Files.writeString(sandbox.resolve(".test-owned"), fixtureOwner);
        project = Files.createDirectory(sandbox.resolve("repo"));
        Files.writeString(project.resolve("pom.xml"), "policy fixture");
    }

    @AfterEach
    void discardOnlyThisOwnedTextFixture() throws Exception {
        if (sandbox == null) return;
        assertEquals(fixtureOwner, Files.readString(sandbox.resolve(".test-owned")));
        assertEquals(Path.of("").toAbsolutePath().normalize().resolve("target"), sandbox.getParent());
        for (Path link : links) Files.deleteIfExists(link); // Unlink the test-created alias before walking.
        Files.walkFileTree(sandbox, new SimpleFileVisitor<>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.normalize().startsWith(sandbox) || Files.isSymbolicLink(dir) || attrs.isOther())
                    throw new IOException("fixture alias/outside path: " + dir);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.normalize().startsWith(sandbox) || Files.isSymbolicLink(file) || attrs.isOther())
                    throw new IOException("fixture alias/outside file: " + file);
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    void rejectsExternalInputAndEscapingRootsBeforeCreatingAnyOutputDirectory() throws Exception {
        for (String override : List.of("C:/NeXAS_DX_runs/builds", project.toString(),
                project.resolve("src").toString(), project.resolve("target/../../outside").toString(),
                project.resolve("target").toString(), sandbox.resolve("foreign").toString(),
                project.resolve("target/bhe-playable-runtime/case/builds").toString(),
                project.resolve("target/Save/builds").toString())) {
            assertThrows(IOException.class, () -> ManagedPlayableBuild.start(project, override, warnings::add));
            assertFalse(Files.exists(project.resolve("target")), "root validation must precede mkdir/lock creation");
        }
        assertEquals(project.resolve("target/bhe-playable-builds"), ManagedPlayableBuild.validateRoot(project, null));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void realWindowsJunctionCannotRedirectTheOutputRoot() throws Exception {
        Files.createDirectories(project.resolve("target"));
        Path destination = Files.createDirectory(sandbox.resolve("junction-destination"));
        Path junction = project.resolve("target/alias");
        Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", junction.toString(), destination.toString())
                .directory(project.toFile()).redirectErrorStream(true).start();
        String detail = new String(process.getInputStream().readAllBytes(), java.nio.charset.Charset.defaultCharset());
        assertEquals(0, process.waitFor(), detail);
        links.add(junction);
        assertThrows(IOException.class, () -> ManagedPlayableBuild.start(project, junction.resolve("builds").toString(), warnings::add));
        assertFalse(Files.exists(destination.resolve("builds")));
    }

    @Test
    void oneProjectLockAlsoExcludesAnotherConfiguredBuildRoot() throws Exception {
        try (ManagedPlayableBuild first = open(null)) {
            assertThrows(IOException.class, () -> open(project.resolve("target/other-builds").toString()));
            assertFalse(Files.exists(project.resolve("target/other-builds")));
            first.failed(new IOException("intentional text-fixture failure"));
        }
    }

    @Test
    void anotherBuildRootCannotBeNestedInsideThePreviousOwnedRun() throws Exception {
        Path old = completed(null, "old");
        Path nested = old.resolve("new-build-root");
        assertThrows(IOException.class, () -> open(nested.toString()));
        assertFalse(Files.exists(nested));
        assertEquals("old exe", Files.readString(old.resolve("sou/game.exe")));
    }

    @Test
    void successPublishesValidatedReceiptThenRetiresOnlyThePreviousOwnedRun() throws Exception {
        Path old = completed(null, "old");
        Path fresh = completed(null, "new");
        assertFalse(Files.exists(old));
        assertTrue(Files.isRegularFile(fresh.resolve("build.json")));
        Map<?, ?> latest = JSON.readValue(project.resolve("target/bhe-playable-build-latest.json").toFile(), Map.class);
        assertEquals(fresh.toString(), latest.get("buildRoot"));
    }

    @Test
    void failedNewBuildKeepsOldCompleteAndOnlyOneBoundedFailureText() throws Exception {
        Path old = completed(null, "old");
        for (int i = 0; i < 2; i++) {
            Path partial;
            try (ManagedPlayableBuild run = open(null)) {
                partial = run.runRoot();
                text(partial.resolve("sou/intermediate.txt"), "partial");
                run.failed(new IOException("failure " + i));
            }
            assertFalse(Files.exists(partial));
            assertTrue(Files.exists(old));
        }
        Path failure = project.resolve("target/bhe-playable-builds/last-failure.txt");
        assertTrue(Files.size(failure) <= 8192);
        assertTrue(Files.readString(failure).contains("failure 1"));
    }

    @Test
    void wrongHashesOrOutsideArtifactPathsCannotPublishAndDoNotDeleteTheOldBuild() throws Exception {
        Path old = completed(null, "old");
        for (boolean outside : List.of(false, true)) {
            try (ManagedPlayableBuild run = open(null)) {
                Map<String, Object> receipt = receipt(run.runRoot(), "new");
                if (outside) receipt.put("pac", project.resolve("pom.xml").toString());
                else receipt.put("exeSha256", "0".repeat(64));
                assertThrows(IOException.class, () -> run.complete(receipt));
                run.failed(new IOException("invalid output receipt"));
            }
            assertTrue(Files.exists(old));
        }
    }

    @Test
    void unknownOrIncorrectlyOwnedRunIsPreservedWithAWarning() throws Exception {
        Path root = project.resolve("target/bhe-playable-builds");
        Path unknown = Files.createDirectories(root.resolve("run_old_unowned"));
        text(unknown.resolve("keep.txt"), "not ours");
        text(unknown.resolve(ManagedPlayableBuild.MARKER_NAME), "{\"schema\":\"different-tool\"}");
        completed(null, "new");
        assertEquals("not ours", Files.readString(unknown.resolve("keep.txt")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("run_old_unowned")));
    }

    @Test
    void sharedRuntimeLeasePreventsAnyOldPayloadDeletion() throws Exception {
        Path old = completed(null, "old");
        try (FileChannel usage = FileChannel.open(old.resolve(ManagedPlayableBuild.USAGE_LOCK_NAME), READ, WRITE);
             FileLock ignored = usage.lock(0, 1, true);
             ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
            assertEquals("old exe", Files.readString(old.resolve("sou/game.exe")));
            assertEquals("old pac", Files.readString(old.resolve("sou/Update3.pac")));
        }
    }

    @Test
    void preparedCaseReferenceProtectsBuildEvenWithoutAnActiveLeaseOrProcess() throws Exception {
        Path old = completed(null, "old");
        Path caseRoot = Files.createDirectories(project.resolve("target/bhe-playable-runtime/prepared-case"));
        text(caseRoot.resolve("manifest.json"), JSON.writeValueAsString(Map.of("exe_source", old.resolve("sou/game.exe").toString())));
        text(caseRoot.resolve("state.json"), "{\"status\":\"prepared\",\"cleanup_pending\":false}");
        try (ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
        }
        assertTrue(Files.exists(old.resolve("sou/game.exe")));
    }

    @Test
    void terminalCaseCanReleaseItsReference() throws Exception {
        Path old = completed(null, "old");
        Path caseRoot = Files.createDirectories(project.resolve("target/bhe-playable-runtime/previous-case"));
        text(caseRoot.resolve("manifest.json"), JSON.writeValueAsString(Map.of("source_files", Map.of(old.resolve("sou/Update3.pac").toString(), Map.of()))));
        text(caseRoot.resolve("state.json"), "{\"status\":\"stopped\",\"cleanup_pending\":false}");
        completed(null, "new");
        assertFalse(Files.exists(old));
    }

    @Test
    void lockedPayloadFailsPreflightWithoutPartiallyDeletingOtherFiles() throws Exception {
        Path old = completed(null, "old");
        Path protectedFile = old.resolve("sou/Update3.pac");
        try (ManagedPlayableBuild fresh = open(null)) {
            try (FileChannel file = FileChannel.open(protectedFile, READ, WRITE);
                 FileLock ignored = file.lock()) {
                assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
                assertTrue(Files.exists(old.resolve("sou/game.exe")), "all files are probed before any payload deletion");
                assertTrue(Files.exists(protectedFile));
            }
        }
        assertEquals("old pac", Files.readString(protectedFile));
        Path recovered = completed(null, "recovered");
        assertFalse(Files.exists(old), "unlocking the old payload permits a later build to retire it");
        assertEquals("recovered pac", Files.readString(recovered.resolve("sou/Update3.pac")));
    }

    @Test
    void changingTheConfiguredRootStillRetiresOnlyTheVerifiedPreviousLatest() throws Exception {
        Path old = completed(null, "old");
        completed(project.resolve("target/alternate-builds").toString(), "new");
        assertFalse(Files.exists(old));
    }

    @Test
    void addedUninventoriedFilesOrGitDataMakeCleanupFailClosed() throws Exception {
        Path old = completed(null, "old");
        text(old.resolve(".git/user-data.txt"), "unique");
        try (ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
        }
        assertEquals("unique", Files.readString(old.resolve(".git/user-data.txt")));
        assertTrue(Files.exists(old.resolve("sou/game.exe")));
    }

    @Test
    void sameSizeEditedIntermediateWithRestoredMtimeIsStillRetained() throws Exception {
        Path old = completed(null, "old");
        Path data = old.resolve("sou/intermediate.txt");
        var time = Files.getLastModifiedTime(data);
        long length = Files.size(data);
        Files.writeString(data, "new intermediate");
        Files.setLastModifiedTime(data, time);
        assertEquals(length, Files.size(data));
        try (ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
        }
        assertEquals("new intermediate", Files.readString(data));
        assertTrue(Files.exists(old.resolve("sou/game.exe")));
    }

    @Test
    void missingConsumerStateDoesNotPermitDeletingItsPreparedSource() throws Exception {
        Path old = completed(null, "old");
        Path consumer = Files.createDirectories(project.resolve("target/bhe-playable-runtime/missing-state"));
        text(consumer.resolve("manifest.json"), JSON.writeValueAsString(Map.of("pac_sources", List.of(old.resolve("sou/Update3.pac").toString()))));
        try (ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
        }
        assertTrue(Files.exists(old.resolve("sou/Update3.pac")));
    }

    @Test
    void preparedSeedDirectoryAlsoPinsTheManagedRun() throws Exception {
        Path old = completed(null, "old");
        Path consumer = Files.createDirectories(project.resolve("target/bhe-playable-runtime/prepared-seed"));
        text(consumer.resolve("manifest.json"), JSON.writeValueAsString(Map.of("seed_source", old.resolve("sou").toString())));
        text(consumer.resolve("state.json"), "{\"status\":\"prepared\",\"cleanup_pending\":false}");
        try (ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
        }
        assertTrue(Files.exists(old.resolve("sou/game.exe")));
    }

    @Test
    void malformedOrNonBooleanCleanupStateIsNotTreatedAsTerminal() throws Exception {
        Path old = completed(null, "old");
        Path consumer = Files.createDirectories(project.resolve("target/bhe-playable-runtime/wrong-state-type"));
        text(consumer.resolve("manifest.json"), JSON.writeValueAsString(Map.of("exe_source", old.resolve("sou/game.exe").toString())));
        text(consumer.resolve("state.json"), "{\"status\":\"stopped\",\"cleanup_pending\":\"false\"}");
        try (ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
        }
        assertTrue(Files.exists(old.resolve("sou/game.exe")));
    }

    @Test
    void malformedActiveConsumerManifestCannotBeAssumedUnrelated() throws Exception {
        Path old = completed(null, "old");
        Path consumer = Files.createDirectories(project.resolve("target/bhe-playable-runtime/malformed-manifest"));
        text(consumer.resolve("state.json"), "{\"status\":\"running\",\"cleanup_pending\":false}");
        text(consumer.resolve("manifest.json"), "{not valid JSON");
        try (ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
        }
        assertTrue(Files.exists(old.resolve("sou/game.exe")));
    }

    @Test
    void lastFailureTextHasARealUtf8ByteBudgetEvenForMultilineJapanese() throws Exception {
        try (ManagedPlayableBuild run = open(null)) {
            run.failed(new IOException("失敗\n".repeat(6000)));
        }
        Path failure = project.resolve("target/bhe-playable-builds/last-failure.txt");
        assertTrue(Files.size(failure) <= 8192);
        assertTrue(Files.readString(failure).contains("失敗"));
    }

    @Test
    void busyRetirementPreventsAnotherRunUntilTheLeaseIsReleased() throws Exception {
        Path old = completed(null, "old");
        try (FileChannel channel = FileChannel.open(old.resolve(ManagedPlayableBuild.USAGE_LOCK_NAME), READ, WRITE);
             FileLock ignored = channel.lock(0, 1, true)) {
            try (ManagedPlayableBuild fresh = open(null)) {
                assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
            }
            long before;
            try (var paths = Files.list(project.resolve("target/bhe-playable-builds"))) { before = paths.count(); }
            assertThrows(IOException.class, () -> open(null));
            try (var paths = Files.list(project.resolve("target/bhe-playable-builds"))) { assertEquals(before, paths.count()); }
        }
        try (ManagedPlayableBuild next = open(null)) { next.failed(new IOException("finish test")); }
        assertFalse(Files.exists(old));
    }

    @Test
    void failedRunWithoutFinalInventoryCannotClaimFilesAddedWhileItWasBlocked() throws Exception {
        Path old = completed(null, "old");
        try (FileChannel channel = FileChannel.open(old.resolve(ManagedPlayableBuild.USAGE_LOCK_NAME), READ, WRITE);
             FileLock ignored = channel.lock(0, 1, true);
             ManagedPlayableBuild fresh = open(null)) {
            assertThrows(IOException.class, () -> fresh.complete(receipt(fresh.runRoot(), "new")));
        }
        // Simulate a producer failure that never sealed an output inventory; new remains the valid latest.
        Path markerPath = old.resolve(ManagedPlayableBuild.MARKER_NAME);
        ObjectNode marker = (ObjectNode) JSON.readTree(markerPath.toFile());
        marker.put("state", "FAILED"); marker.put("endedAt", java.time.Instant.now().toString());
        marker.remove("inventorySha256");
        Files.writeString(markerPath, JSON.writeValueAsString(marker));
        Path inventory = old.resolve(".bhe-playable-files.json");
        Files.delete(inventory);
        Path sentinel = text(old.resolve("do-not-claim.txt"), "user evidence added after failure");

        assertThrows(IOException.class, () -> open(null));

        assertFalse(Files.exists(inventory), "cleanup must not create an inventory after the failure");
        assertEquals("user evidence added after failure", Files.readString(sentinel));
        assertEquals("old exe", Files.readString(old.resolve("sou/game.exe")));
        assertEquals("old pac", Files.readString(old.resolve("sou/Update3.pac")));
    }

    private ManagedPlayableBuild open(String root) throws IOException {
        return ManagedPlayableBuild.start(project, root, warnings::add);
    }

    private Path completed(String root, String label) throws Exception {
        try (ManagedPlayableBuild run = open(root)) {
            Path path = run.runRoot(); run.complete(receipt(path, label)); return path;
        }
    }

    private Map<String, Object> receipt(Path run, String label) throws Exception {
        Path exe = text(run.resolve("sou/game.exe"), label + " exe");
        Path pac = text(run.resolve("sou/Update3.pac"), label + " pac");
        text(run.resolve("sou/intermediate.txt"), label + " intermediate");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buildRoot", run.toString()); result.put("assetRoot", run.resolve("sou").toString());
        result.put("exe", exe.toString()); result.put("pac", pac.toString());
        result.put("exeSha256", ManagedPlayableBuild.sha256(exe)); result.put("pacSha256", ManagedPlayableBuild.sha256(pac));
        return result;
    }

    private static Path text(Path path, String value) throws IOException {
        Files.createDirectories(path.getParent()); Files.writeString(path, value); return path;
    }
}
