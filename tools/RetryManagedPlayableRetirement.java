import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/** Single authorized recovery of the named pending retirement; never starts a build/run. */
public class RetryManagedPlayableRetirement {
    private static final Path PROJECT = Path.of("D:/Code/NeXAS_DX");
    private static final Path BUILDS = PROJECT.resolve("target/bhe-playable-builds");
    private static final Path OLD = BUILDS.resolve("run_20261103_154924_549_070a8f21");
    private static final Path CURRENT = BUILDS.resolve("run_20261105_123342_656_c9e63039");
    private static final Path CASE = PROJECT.resolve("target/bhe-playable-runtime/sou-direction-fix-20261105-09");
    private static final Path PENDING = PROJECT.resolve("target/.bhe-playable-retirement.json");
    private static final Path LATEST = PROJECT.resolve("target/bhe-playable-build-latest.json");
    private static final Path OUTPUT = PROJECT.resolve("target/bhe-combined-fixes-20261105/retirement-recovery.json");
    private static final String CLASS_SHA = "20a58da6b3a3d24ab3582d8d9f85ff9d01be8c2935df10242a27b4882fdb4eb4";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static void require(boolean value, String message) throws IOException {
        if (!value) throw new IOException(message);
    }

    private static Path real(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        for (Path cursor = absolute; cursor != null; cursor = cursor.getParent()) {
            if (!Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) continue;
            BasicFileAttributes attrs = Files.readAttributes(cursor, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            require(!attrs.isSymbolicLink() && !attrs.isOther() && cursor.toRealPath().equals(cursor),
                    "Alias/special path refused: " + cursor);
        }
        return absolute;
    }

    private static String sha(Path path) throws Exception {
        real(path);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] bytes = new byte[4 * 1024 * 1024];
            for (int count; (count = stream.read(bytes)) >= 0;) digest.update(bytes, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Method method(Class<?> type, String name, Class<?>... parameters) throws Exception {
        Method method = type.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static Object invoke(Method method, Object target, Object... args) throws Exception {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }

    private static JsonNode read(Path path) throws Exception {
        return JSON.readTree(real(path).toFile());
    }

    private static TreeSet<String> runDirectories() throws Exception {
        TreeSet<String> result = new TreeSet<>();
        try (var paths = Files.list(BUILDS)) {
            for (Path path : paths.toList()) {
                real(path);
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) result.add(path.getFileName().toString());
            }
        }
        return result;
    }

    private static Map<String, String> currentIdentity() throws Exception {
        JsonNode receipt = read(CURRENT.resolve("build.json"));
        require(CURRENT.toString().equals(receipt.path("buildRoot").asText()), "New receipt root mismatch");
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : List.of(".bhe-playable-owned.json", ".bhe-playable-files.json", "build.json"))
            result.put(name, sha(CURRENT.resolve(name)));
        for (String kind : List.of("exe", "pac")) {
            Path payload = real(Path.of(receipt.path(kind).asText()));
            require(payload.startsWith(CURRENT) && !payload.equals(CURRENT), "New payload escapes current run");
            String digest = sha(payload);
            require(digest.equals(receipt.path(kind + "Sha256").asText()), "New payload hash mismatch");
            result.put(kind + "Path", payload.toString());
            result.put(kind + "Sha256", digest);
        }
        JsonNode owner = read(CURRENT.resolve(".bhe-playable-owned.json"));
        require("COMPLETE".equals(owner.path("state").asText()), "New owner is not COMPLETE");
        require(result.get("build.json").equals(owner.path("receiptSha256").asText()), "New receipt owner hash mismatch");
        require(result.get(".bhe-playable-files.json").equals(owner.path("inventorySha256").asText()), "New inventory owner hash mismatch");
        result.put("latestSha256", sha(LATEST));
        return result;
    }

    private static void checkCaseUsesCurrent() throws Exception {
        real(CASE);
        JsonNode manifest = read(CASE.resolve("manifest.json"));
        JsonNode state = read(CASE.resolve("state.json"));
        require(CASE.toString().equals(manifest.path("case").asText()), "Case identity mismatch");
        require(List.of("prepared", "launching", "suspended_installing_hooks", "running")
                .contains(state.path("status").asText()), "Case09 is no longer prepared/running");
        require(state.path("process").path("pid").asLong() == 811768, "Case09 PID mismatch");
        ProcessHandle game = ProcessHandle.of(811768).orElseThrow();
        require(game.isAlive(), "Case09 game no longer alive");
        require(real(Path.of(game.info().command().orElseThrow())).equals(CASE.resolve("game/BaldrSky.exe")), "Case09 process image mismatch");
        require(real(Path.of(manifest.path("exe_source").asText())).startsWith(CURRENT), "Case09 EXE is not from new run");
        for (JsonNode pac : manifest.path("pac_sources"))
            require(real(Path.of(pac.asText())).startsWith(CURRENT), "Case09 PAC is not from new run");
        var fields = manifest.path("source_files").fieldNames();
        while (fields.hasNext()) require(real(Path.of(fields.next())).startsWith(CURRENT), "Case09 source_files contains another run");
        require(!manifest.toString().contains(OLD.toString().replace("\\", "\\\\")), "Case09 manifest still refers to old run");
    }

    private static void noTrackedOldFiles() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("git", "--literal-pathspecs", "ls-files", "-z", "--",
                PROJECT.relativize(OLD).toString().replace('\\', '/')).directory(PROJECT.toFile()).redirectErrorStream(true);
        builder.environment().put("GIT_OPTIONAL_LOCKS", "0");
        Process git = builder.start();
        byte[] output = git.getInputStream().readAllBytes();
        require(git.waitFor() == 0 && output.length == 0, "Tracked Git file or Git verification error inside old run");
    }

    private static void save(Map<String, Object> receipt) throws Exception {
        Path temporary = real(OUTPUT.resolveSibling("retirement-recovery.json.new"));
        require(!Files.exists(temporary, LinkOption.NOFOLLOW_LINKS), "Unresolved recovery receipt temporary file");
        String text = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(receipt)
                .replace("\r\n", "\n").replace('\r', '\n').stripTrailing().replace("\n", "\r\n") + "\r\n";
        Files.writeString(temporary, text, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW, WRITE);
        Files.move(temporary, OUTPUT, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 0, "This authorized recovery accepts no alternate paths or arguments");
        for (Path directory : List.of(PROJECT, BUILDS, OLD, CURRENT, OUTPUT.getParent()))
            require(Files.isDirectory(real(directory), LinkOption.NOFOLLOW_LINKS), "Missing real recovery directory");
        require(OLD.getParent().equals(BUILDS) && CURRENT.getParent().equals(BUILDS) && BUILDS.startsWith(PROJECT.resolve("target")), "Recovery escaped approved builds root");
        require(!Files.exists(real(OUTPUT), LinkOption.NOFOLLOW_LINKS), "Recovery receipt already exists; no repeat operation");
        require(real(Path.of(System.getProperty("java.io.tmpdir"))).startsWith(PROJECT.resolve("target/bhe-combined-fixes-20261105")), "JVM temporary root escapes recovery target");
        Path compiled = PROJECT.resolve("target/test-classes/com/giga/nexas/bhe2bsdx/byJinki/ManagedPlayableBuild.class");
        require(CLASS_SHA.equals(sha(compiled)), "Compiled Managed class differs from root-verified GREEN class");
        Class<?> managed = Class.forName("com.giga.nexas.bhe2bsdx.byJinki.ManagedPlayableBuild");
        Method compare = method(managed, "inventoryForComparison", JsonNode.class);
        Method cleanup = method(managed, "finishPendingRetirement");
        Method snapshot = method(managed, "snapshot", Path.class);
        Method latestRun = method(managed, "latestRun");
        Constructor<?> constructor = managed.getDeclaredConstructor(Path.class, Path.class, Consumer.class, FileChannel.class, FileLock.class);
        constructor.setAccessible(true);
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("schema", 1); receipt.put("startedAt", Instant.now().toString()); receipt.put("oldRun", OLD.toString());
        receipt.put("currentRun", CURRENT.toString()); receipt.put("compiledManagedClassSha256", CLASS_SHA);
        receipt.put("entry", "private constructor + finishPendingRetirement only; never start/createRun/build");
        receipt.put("originalSourceInputsDeleted", false);
        List<String> warnings = new ArrayList<>();
        FileChannel channel = null; FileLock lock = null; Object handle = null;
        try {
            Path lockPath = real(PROJECT.resolve("target/.bhe-playable-build.lock"));
            require(Files.isRegularFile(lockPath, LinkOption.NOFOLLOW_LINKS), "Existing project lock missing");
            channel = FileChannel.open(lockPath, READ, WRITE, LinkOption.NOFOLLOW_LINKS);
            lock = channel.tryLock(0, 1, false);
            require(lock != null, "Another build holds the project lock");
            Consumer<String> warning = warnings::add;
            handle = constructor.newInstance(PROJECT, BUILDS, warning, channel, lock);
            require(BUILDS.equals(invoke(method(managed, "validateRoot", Path.class, String.class), null, PROJECT, BUILDS.toString())), "Managed root validation mismatch");
            JsonNode pending = read(PENDING);
            require("nexas.bhe-playable-managed/v1".equals(pending.path("schema").asText()) && PROJECT.toString().equals(pending.path("projectRoot").asText()), "Pending ownership mismatch");
            require(pending.path("runs").isArray() && pending.path("runs").size() == 1, "Pending must contain exactly the approved old run");
            require(OLD.toString().equals(pending.path("runs").get(0).path("path").asText()) && pending.path("runs").get(0).path("partial").isBoolean() && !pending.path("runs").get(0).path("partial").asBoolean(), "Pending old run or partial flag mismatch");
            require(CURRENT.toString().equals(pending.path("replacementRun").asText()) && CURRENT.equals(invoke(latestRun, handle)), "Pending replacement/latest mismatch");
            checkCaseUsesCurrent(); noTrackedOldFiles();
            receipt.put("case09Pid", 811768); receipt.put("case09UsesOnlyCurrentRun", true); receipt.put("oldContainsGitTrackedFiles", false);
            TreeSet<String> beforeRuns = runDirectories(); receipt.put("runDirectoriesBefore", beforeRuns);
            Map<String, String> newIdentity = currentIdentity(); receipt.put("newIdentityBefore", newIdentity);
            String caseManifestHash = sha(CASE.resolve("manifest.json"));
            JsonNode oldOwner = (JsonNode) invoke(method(managed, "readOwner", Path.class), handle, OLD);
            require(oldOwner != null && "COMPLETE".equals(oldOwner.path("state").asText()), "Old ownership/COMPLETE check failed");
            require(sha(OLD.resolve("build.json")).equals(oldOwner.path("receiptSha256").asText()), "Old receipt changed");
            Path inventoryPath = OLD.resolve(".bhe-playable-files.json");
            require(sha(inventoryPath).equals(oldOwner.path("inventorySha256").asText()), "Old inventory changed");
            JsonNode expected = read(inventoryPath);
            System.out.println("{\"phase\":\"read_only_full_old_inventory_verification\"}");
            Object tree = invoke(snapshot, handle, OLD);
            Field rowsField = tree.getClass().getDeclaredField("rows"); rowsField.setAccessible(true);
            JsonNode actual = JSON.valueToTree(rowsField.get(tree));
            require(invoke(compare, null, actual).equals(invoke(compare, null, expected)), "Old inventory differs beyond directory modified metadata");
            require(actual.size() == 25192, "Old snapshot row count differs from audited tree");
            long logicalBytes = 0; int files = 0; List<String> directoryTimeDifferences = new ArrayList<>();
            for (int index = 0; index < actual.size(); index++) {
                JsonNode row = actual.get(index), original = expected.get(index);
                if (row.path("kind").asText().equals("file")) { logicalBytes += Long.parseLong(row.path("size").asText()); files++; }
                else if (!row.path("modified").equals(original.path("modified"))) directoryTimeDifferences.add(row.path("path").asText());
            }
            for (String name : List.of(".bhe-playable-files.json", ".bhe-playable-owned.json", ".artifact-use.lock", "build.json")) logicalBytes += Files.size(OLD.resolve(name));
            receipt.put("oldInventorySha256", oldOwner.path("inventorySha256").asText()); receipt.put("verifiedPayloadFiles", files);
            receipt.put("directoryModifiedOnlyDifferences", directoryTimeDifferences); receipt.put("oldTotalLogicalBytes", logicalBytes);
            receipt.put("usableSpaceBefore", Files.getFileStore(BUILDS).getUsableSpace()); receipt.put("status", "validated_before_retirement");
            save(receipt);
            noTrackedOldFiles(); checkCaseUsesCurrent();
            System.out.println("{\"phase\":\"invoke_existing_pending_retirement_with_all_internal_guards\"}");
            invoke(cleanup, handle);
            require(!Files.exists(OLD, LinkOption.NOFOLLOW_LINKS) && !Files.exists(PENDING, LinkOption.NOFOLLOW_LINKS), "Retirement/pending not fully removed");
            require(CURRENT.equals(invoke(latestRun, handle)), "Current latest changed during recovery");
            require(currentIdentity().equals(newIdentity), "Current new package identity changed during recovery");
            require(sha(CASE.resolve("manifest.json")).equals(caseManifestHash), "Current case manifest changed during recovery");
            TreeSet<String> afterRuns = runDirectories(); TreeSet<String> expectedRuns = new TreeSet<>(beforeRuns); expectedRuns.remove(OLD.getFileName().toString());
            require(afterRuns.equals(expectedRuns), "Unexpected run creation/removal during recovery");
            receipt.put("status", "complete"); receipt.put("finishedAt", Instant.now().toString()); receipt.put("oldDirectoryRemoved", true);
            receipt.put("pendingRemoved", true); receipt.put("newIdentityUnchanged", true); receipt.put("case09ManifestUnchanged", true);
            receipt.put("newRunCreated", false); receipt.put("runDirectoriesAfter", afterRuns); receipt.put("removedLogicalBytes", logicalBytes);
            receipt.put("spaceAccounting", "logical bytes of removed owned files; observed free-space change is not attributed solely to this operation");
            receipt.put("usableSpaceAfter", Files.getFileStore(BUILDS).getUsableSpace()); receipt.put("warnings", warnings);
            save(receipt); System.out.println(JSON.writeValueAsString(Map.of("status", "complete", "receipt", OUTPUT.toString(), "removedLogicalBytes", logicalBytes)));
        } catch (Exception error) {
            receipt.put("status", "failed_retained_for_review"); receipt.put("failedAt", Instant.now().toString()); receipt.put("error", error.toString());
            receipt.put("oldDirectoryStillExists", Files.exists(OLD, LinkOption.NOFOLLOW_LINKS)); receipt.put("pendingStillExists", Files.exists(PENDING, LinkOption.NOFOLLOW_LINKS));
            receipt.put("warnings", warnings); save(receipt); throw error;
        } finally {
            if (handle != null) invoke(method(managed, "close"), handle);
            if (lock != null && lock.isValid()) lock.release();
            if (channel != null && channel.isOpen()) channel.close();
        }
    }
}
