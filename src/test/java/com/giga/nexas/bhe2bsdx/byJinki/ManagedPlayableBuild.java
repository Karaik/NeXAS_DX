package com.giga.nexas.bhe2bsdx.byJinki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/** Test-runner output policy; the transfer pipeline itself remains unchanged. */
final class ManagedPlayableBuild implements AutoCloseable {
    static final String MARKER_NAME = ".bhe-playable-owned.json";
    static final String USAGE_LOCK_NAME = ".artifact-use.lock";
    private static final String SCHEMA = "nexas.bhe-playable-managed/v1";
    private static final String INVENTORY = ".bhe-playable-files.json";
    private static final Set<String> CONTROL = Set.of(MARKER_NAME, USAGE_LOCK_NAME, INVENTORY, "build.json");
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path project, target, root, latest, pending;
    private final Consumer<String> warning;
    private final FileChannel rootChannel;
    private final FileLock rootLock;
    private Path run;
    private ObjectNode owner;
    private FileChannel usageChannel;
    private FileLock usageLock;
    private boolean committed, failureHandled, closed;

    private ManagedPlayableBuild(Path project, Path root, Consumer<String> warning,
                                 FileChannel rootChannel, FileLock rootLock) {
        this.project = project; this.target = project.resolve("target"); this.root = root;
        this.latest = target.resolve("bhe-playable-build-latest.json");
        this.pending = target.resolve(".bhe-playable-retirement.json");
        this.warning = warning; this.rootChannel = rootChannel; this.rootLock = rootLock;
    }

    static Path validateRoot(Path project, String override) throws IOException {
        Path base = project.toAbsolutePath().normalize();
        if (!Files.isDirectory(base) || !base.toRealPath().equals(base))
            throw new IOException("Repository root must be a real directory: " + base);
        noAliases(base);
        Path target = base.resolve("target");
        Path requested = override == null ? target.resolve("bhe-playable-builds") : Path.of(override);
        Path root = (requested.isAbsolute() ? requested : base.resolve(requested)).toAbsolutePath().normalize();
        if (root.equals(target) || !root.startsWith(target))
            throw new IOException("Build root must be a strict child of this repository's target: " + root);
        noAliases(root);
        if (root.startsWith(target.resolve("bhe-playable-runtime")))
            throw new IOException("Build output cannot occupy the protected runtime-case namespace");
        Path ancestor = base;
        for (Path part : base.relativize(root)) {
            ancestor = ancestor.resolve(part);
            if (Set.of(".git", "save", "saves", "replay").contains(part.toString().toLowerCase(java.util.Locale.ROOT)))
                throw new IOException("Protected data cannot be a build output ancestor: " + ancestor);
            if (Files.exists(ancestor.resolve(MARKER_NAME), LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Build roots cannot be nested inside an existing managed run: " + ancestor);
        }
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Build root is not a directory: " + root);
        return root;
    }

    static ManagedPlayableBuild start(Path project, String override, Consumer<String> warning) throws IOException {
        Path base = project.toAbsolutePath().normalize();
        Path root = validateRoot(base, override); // No mkdir or lock creation before this validation.
        Files.createDirectories(base.resolve("target"));
        noAliases(base.resolve("target"));
        Path lockPath = base.resolve("target/.bhe-playable-build.lock");
        noAliases(lockPath);
        FileChannel channel = FileChannel.open(lockPath, CREATE, READ, WRITE, LinkOption.NOFOLLOW_LINKS);
        FileLock lock;
        try {
            lock = channel.tryLock(0, 1, false);
            if (lock == null) throw new IOException("Another playable build holds the project build lock");
        } catch (IOException | OverlappingFileLockException error) {
            channel.close();
            throw new IOException("Cannot acquire project playable-build lock", error);
        }
        ManagedPlayableBuild build = new ManagedPlayableBuild(base, root, warning, channel, lock);
        try {
            validateRoot(base, override);
            Files.createDirectories(root);
            noAliases(root);
            build.finishPendingRetirement();
            for (Path old : build.ownedRuns(root)) {
                ObjectNode marker = build.readOwner(old);
                if (marker == null) continue;
                if (marker.path("state").asText().equals("FAILED")) build.retire(old);
                else if (marker.path("state").asText().equals("BUILDING"))
                    throw new IOException("Unfinished managed run retained; producer exit is not proved: " + old);
            }
            build.createRun();
            return build;
        } catch (IOException | RuntimeException error) {
            try { build.close(); } catch (IOException closeError) { error.addSuppressed(closeError); }
            throw error;
        }
    }

    Path runRoot() { return run; }

    private void createRun() throws IOException {
        String id = UUID.randomUUID().toString();
        String name = "run_" + LocalDateTime.now(ZoneId.of("Asia/Tokyo"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")) + "_" + id.substring(0, 8);
        run = root.resolve(name);
        Files.createDirectory(run);
        Files.write(run.resolve(USAGE_LOCK_NAME), new byte[]{0}, CREATE_NEW, WRITE);
        usageChannel = FileChannel.open(run.resolve(USAGE_LOCK_NAME), READ, WRITE, LinkOption.NOFOLLOW_LINKS);
        usageLock = usageChannel.tryLock(0, 1, false);
        if (usageLock == null) throw new IOException("New run lease is unexpectedly in use");
        owner = JSON.createObjectNode();
        owner.put("schema", SCHEMA); owner.put("projectRoot", project.toString());
        owner.put("buildsRoot", root.toString()); owner.put("runName", name); owner.put("runId", id);
        owner.put("state", "BUILDING"); owner.put("ownerPid", ProcessHandle.current().pid());
        owner.put("ownerStartedAt", ProcessHandle.current().info().startInstant().orElseThrow().toString());
        owner.put("createdAt", Instant.now().toString());
        writeJson(run.resolve(MARKER_NAME), owner);
    }

    void complete(Map<String, Object> receipt) throws IOException {
        requireOpen();
        if (committed || failureHandled) throw new IOException("Run already ended");
        rejectLiveProducers();
        ObjectNode document = JSON.valueToTree(receipt);
        validateReceipt(document, run);
        Path receiptFile = run.resolve("build.json");
        if (Files.exists(receiptFile, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Refuse existing build receipt");
        writeJson(receiptFile, document);
        owner.put("receiptSha256", sha256(receiptFile));
        sealInventory(run, owner);
        owner.put("state", "COMPLETE"); owner.put("endedAt", Instant.now().toString());
        writeJson(run.resolve(MARKER_NAME), owner);

        Set<Path> previous = new LinkedHashSet<>();
        Path oldLatest = latestRun();
        if (oldLatest != null && !oldLatest.equals(run)) previous.add(oldLatest);
        for (Path candidate : ownedRuns(root)) {
            ObjectNode marker = readOwner(candidate);
            if (!candidate.equals(run) && marker != null && marker.path("state").asText().equals("COMPLETE"))
                previous.add(candidate);
        }
        if (!previous.isEmpty()) {
            ObjectNode retirement = JSON.createObjectNode();
            retirement.put("schema", SCHEMA); retirement.put("projectRoot", project.toString());
            retirement.put("replacementRun", run.toString());
            ArrayNode rows = retirement.putArray("runs");
            for (Path old : previous) rows.addObject().put("path", old.toString()).put("partial", false);
            if (Files.exists(pending, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unresolved retirement already exists");
            writeJson(pending, retirement); // Durable intent before replacing the latest pointer.
        }
        writeJson(latest, document);
        committed = true;
        releaseUsage();
        finishPendingRetirement(); // A blocked retirement is a visible error; the new receipt remains valid.
    }

    void failed(Throwable failure) throws IOException {
        if (committed || failureHandled || run == null || owner == null) return;
        failureHandled = true;
        String text = "run=" + run + "\nfailedAt=" + Instant.now() + "\n" + failure + "\n";
        text = text.replace("\r\n", "\n").replace('\r', '\n').stripTrailing().replace("\n", "\r\n");
        while ((text + "\r\n").getBytes(StandardCharsets.UTF_8).length > 8192)
            text = text.substring(0, text.offsetByCodePoints(text.length(), -1));
        writeText(root.resolve("last-failure.txt"), text);
        owner.put("state", "FAILED"); owner.put("endedAt", Instant.now().toString());
        ArrayNode producers = owner.putArray("producerProcesses");
        ProcessHandle.current().descendants().filter(ProcessHandle::isAlive).forEach(p -> {
            ObjectNode row = producers.addObject().put("pid", p.pid());
            row.put("startedAt", p.info().startInstant().map(Instant::toString).orElse("unknown"));
        });
        if (producers.isEmpty()) sealInventory(run, owner);
        writeJson(run.resolve(MARKER_NAME), owner);
        releaseUsage();
        if (!producers.isEmpty()) throw new IOException("Failed run retained: native producers still alive: " + run);
        retire(run);
        if (Files.exists(pending, LinkOption.NOFOLLOW_LINKS)) {
            ObjectNode p = object(pending);
            if (run.toString().equals(p.path("replacementRun").asText())) Files.delete(pending);
        }
    }

    private void requireOpen() throws IOException {
        if (closed || !rootLock.isValid() || usageLock == null || !usageLock.isValid())
            throw new IOException("Managed build no longer holds its build/run lease");
    }

    private void rejectLiveProducers() throws IOException {
        if (ProcessHandle.current().descendants().anyMatch(ProcessHandle::isAlive))
            throw new IOException("Native producer completion is unproved; build cannot be published");
    }

    private List<Path> ownedRuns(Path parent) throws IOException {
        List<Path> runs = new ArrayList<>();
        try (var children = Files.newDirectoryStream(parent)) {
            for (Path child : children) if (child.getFileName().toString().startsWith("run_")) {
                if (readOwner(child) == null) warning.accept("Unmanaged/unknown run retained: " + child);
                else runs.add(child);
            }
        }
        return runs;
    }

    private ObjectNode readOwner(Path directory) {
        try {
            Path path = directory.toAbsolutePath().normalize();
            noAliases(path);
            if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) return null;
            ObjectNode marker = object(path.resolve(MARKER_NAME));
            if (!SCHEMA.equals(marker.path("schema").asText()) || !project.toString().equals(marker.path("projectRoot").asText())) return null;
            Path builds = validateRoot(project, marker.path("buildsRoot").asText());
            String id = UUID.fromString(marker.path("runId").asText()).toString();
            if (!builds.equals(path.getParent()) || !path.getFileName().toString().equals(marker.path("runName").asText())
                    || !marker.path("runName").asText().startsWith("run_") || !marker.path("runName").asText().endsWith(id.substring(0, 8))) return null;
            String state = marker.path("state").asText();
            if (!Set.of("BUILDING", "COMPLETE", "FAILED").contains(state)) return null;
            if (!marker.path("ownerPid").isIntegralNumber() || marker.path("ownerPid").asLong() <= 0) return null;
            Instant.parse(marker.path("ownerStartedAt").asText());
            Instant.parse(marker.path("createdAt").asText());
            if (!state.equals("BUILDING")) Instant.parse(marker.path("endedAt").asText());
            if (state.equals("COMPLETE") && (!marker.path("inventorySha256").asText().matches("[0-9a-f]{64}")
                    || !marker.path("receiptSha256").asText().matches("[0-9a-f]{64}"))) return null;
            return marker;
        } catch (IOException | RuntimeException invalid) { return null; }
    }

    private Path latestRun() throws IOException {
        if (!Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return null;
        ObjectNode document = object(latest);
        Path candidate = absolute(document.path("buildRoot").asText());
        ObjectNode marker = readOwner(candidate);
        if (marker == null) { warning.accept("Unmanaged previous latest retained: " + candidate); return null; }
        if (!marker.path("state").asText().equals("COMPLETE")) throw new IOException("Latest points to unfinished run: " + candidate);
        validateReceipt(document, candidate);
        if (!sha256(candidate.resolve("build.json")).equals(marker.path("receiptSha256").asText()))
            throw new IOException("Latest receipt identity changed: " + candidate);
        return candidate;
    }

    private void finishPendingRetirement() throws IOException {
        if (!Files.exists(pending, LinkOption.NOFOLLOW_LINKS)) return;
        ObjectNode document = object(pending);
        if (!SCHEMA.equals(document.path("schema").asText()) || !project.toString().equals(document.path("projectRoot").asText())
                || !document.path("runs").isArray()) throw new IOException("Unknown retirement record retained: " + pending);
        Path replacement = absolute(document.path("replacementRun").asText());
        if (!replacement.equals(latestRun())) throw new IOException("Interrupted publication retained for review: " + pending);
        for (JsonNode row : document.path("runs")) {
            if (row.path("partial").asBoolean(true)) throw new IOException("Partial retirement requires review, no further build: " + row.path("path"));
            Path previous = absolute(row.path("path").asText());
            if (previous.equals(replacement)) throw new IOException("Refuse to retire latest run");
            if (!Files.exists(previous, LinkOption.NOFOLLOW_LINKS)) continue;
            try { retire(previous); }
            catch (CleanupFailure error) {
                ((ObjectNode) row).put("partial", error.partial).put("error", error.getMessage());
                writeJson(pending, document);
                warning.accept("Build retained, previous retirement blocked: " + previous + " : " + error.getMessage());
                throw error;
            } catch (IOException error) {
                ((ObjectNode) row).put("error", error.getMessage()); writeJson(pending, document);
                warning.accept("Build retained, previous retirement blocked: " + previous + " : " + error.getMessage());
                throw error;
            }
        }
        Files.delete(pending);
    }

    private void retire(Path directory) throws IOException {
        ObjectNode marker = readOwner(directory);
        if (marker == null || marker.path("state").asText().equals("BUILDING"))
            throw new IOException("Refuse unowned or unfinished cleanup: " + directory);
        if (directory.equals(latestRun())) throw new IOException("Refuse deleting current latest");
        for (JsonNode producer : marker.path("producerProcesses")) {
            ProcessHandle live = ProcessHandle.of(producer.path("pid").asLong(-1)).orElse(null);
            if (live != null && live.isAlive() && (producer.path("startedAt").asText().equals("unknown")
                    || live.info().startInstant().map(Instant::toString).orElse("unknown").equals(producer.path("startedAt").asText())))
                throw new IOException("Failed producer still alive: " + directory);
        }
        protectConsumers(directory);
        noAliases(directory.resolve(USAGE_LOCK_NAME));
        if (!Files.isRegularFile(directory.resolve(USAGE_LOCK_NAME), LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Missing existing managed usage lease: " + directory);
        boolean deleting = false;
        try (FileChannel channel = FileChannel.open(directory.resolve(USAGE_LOCK_NAME), READ, WRITE, LinkOption.NOFOLLOW_LINKS)) {
            FileLock lease;
            try { lease = channel.tryLock(0, 1, false); }
            catch (OverlappingFileLockException locked) { throw new IOException("Run is in use: " + directory, locked); }
            if (lease == null) throw new IOException("Run is in use: " + directory);
            try (lease) {
                protectConsumers(directory); // Includes the prepared→launch window, independently of the lease.
                if (!marker.has("inventorySha256"))
                    throw new IOException("No final owned inventory; failed producer/output requires review: " + directory);
                Path inventory = directory.resolve(INVENTORY);
                if (!sha256(inventory).equals(marker.path("inventorySha256").asText())) throw new IOException("Inventory identity changed");
                Tree tree = snapshot(directory);
                if (!inventoryForComparison(JSON.valueToTree(tree.rows))
                        .equals(inventoryForComparison(JSON.readTree(inventory.toFile()))))
                    throw new IOException("Uninventoried or changed output retained: " + directory);
                if (marker.path("state").asText().equals("COMPLETE")) {
                    if (!sha256(directory.resolve("build.json")).equals(marker.path("receiptSha256").asText())) throw new IOException("Receipt changed");
                    validateReceipt(object(directory.resolve("build.json")), directory);
                }
                List<Path> probes = new ArrayList<>(tree.files);
                for (String control : List.of(MARKER_NAME, INVENTORY, "build.json"))
                    if (Files.exists(directory.resolve(control), LinkOption.NOFOLLOW_LINKS)) probes.add(directory.resolve(control));
                for (Path file : probes) probeUnlocked(file); // Finish every probe before deleting any payload.
                deleting = true;
                for (Path file : tree.files) { noAliases(file); Files.delete(file); }
                for (Path dir : tree.directories) Files.delete(dir);
            }
        } catch (IOException error) { throw new CleanupFailure("Managed cleanup blocked at " + directory + ": " + error.getMessage(), deleting, error); }
        try {
            for (String control : List.of(INVENTORY, "build.json", MARKER_NAME, USAGE_LOCK_NAME))
                Files.deleteIfExists(directory.resolve(control));
            Files.delete(directory);
        } catch (IOException error) { throw new CleanupFailure("Managed metadata cleanup incomplete: " + directory, true, error); }
    }

    private void sealInventory(Path directory, ObjectNode marker) throws IOException {
        Tree tree = snapshot(directory);
        writeJson(directory.resolve(INVENTORY), JSON.valueToTree(tree.rows));
        marker.put("inventorySha256", sha256(directory.resolve(INVENTORY)));
    }

    private static JsonNode inventoryForComparison(JsonNode inventory) {
        if (inventory == null) return null;
        JsonNode comparable = inventory.deepCopy();
        if (comparable.isArray()) {
            for (JsonNode row : comparable) {
                if (row.isObject() && "directory".equals(row.path("kind").asText()))
                    ((ObjectNode) row).remove("modified");
            }
        }
        return comparable;
    }

    private Tree snapshot(Path directory) throws IOException {
        noAliases(directory);
        Tree tree = new Tree();
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            private void check(Path path, BasicFileAttributes attrs) throws IOException {
                if (!path.normalize().startsWith(directory) || Files.isSymbolicLink(path) || attrs.isOther())
                    throw new IOException("Alias or special output retained: " + path);
                noAliases(path);
                String name = path.getFileName().toString();
                if (name.equalsIgnoreCase(".git") || attrs.isDirectory() && Set.of("save", "saves", "replay").contains(name.toLowerCase(java.util.Locale.ROOT)))
                    throw new IOException("Protected data retained: " + path);
            }
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                check(dir, attrs);
                if (!dir.equals(directory)) { tree.directories.add(dir); add(dir, attrs, "directory"); }
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                check(file, attrs);
                if (!attrs.isRegularFile()) throw new IOException("Not a regular output: " + file);
                if (!file.getParent().equals(directory) || !CONTROL.contains(file.getFileName().toString())) {
                    tree.files.add(file); add(file, attrs, "file");
                }
                return FileVisitResult.CONTINUE;
            }
            private void add(Path path, BasicFileAttributes attrs, String kind) throws IOException {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("path", directory.relativize(path).toString()); row.put("kind", kind);
                row.put("size", kind.equals("file") ? Long.toString(attrs.size()) : "0");
                row.put("modified", attrs.lastModifiedTime().toString()); row.put("fileKey", String.valueOf(attrs.fileKey()));
                row.put("sha256", kind.equals("file") ? sha256(path) : "");
                tree.rows.add(row);
            }
        });
        tree.rows.sort(Comparator.comparing(r -> r.get("path")));
        tree.files.sort(Comparator.comparing(Path::toString));
        tree.directories.sort(Comparator.comparingInt(Path::getNameCount).reversed());
        return tree;
    }

    private void protectConsumers(Path directory) throws IOException {
        Path cases = target.resolve("bhe-playable-runtime");
        if (!Files.exists(cases, LinkOption.NOFOLLOW_LINKS)) return;
        noAliases(cases);
        try (var entries = Files.newDirectoryStream(cases)) {
            for (Path consumer : entries) {
                noAliases(consumer);
                if (!Files.isDirectory(consumer, LinkOption.NOFOLLOW_LINKS)) continue;
                ObjectNode state = object(consumer.resolve("state.json"));
                boolean terminal = Set.of("stopped", "failed", "exited").contains(state.path("status").asText())
                        && state.path("cleanup_pending").isBoolean() && !state.path("cleanup_pending").booleanValue();
                if (terminal) continue; // The shared usage lease still covers final source verification.
                ObjectNode manifest = object(consumer.resolve("manifest.json"));
                List<Path> references = new ArrayList<>();
                for (String key : List.of("exe_source", "pac_sources", "source_files", "base_dir",
                        "original_save", "save_dir", "seed_source")) collectPaths(manifest.get(key), references);
                if (references.isEmpty()) throw new IOException("Unknown active/prepared consumer schema: " + consumer);
                if (references.stream().anyMatch(p -> p.startsWith(directory)))
                    throw new IOException("Prepared/active/cleanup consumer retains build: " + consumer);
            }
        }
    }

    private static void collectPaths(JsonNode node, List<Path> paths) {
        if (node == null) return;
        if (node.isTextual()) {
            try { Path path = Path.of(node.textValue()); if (path.isAbsolute()) paths.add(path.toAbsolutePath().normalize()); }
            catch (RuntimeException ignored) { /* Not a path; unknown schema is checked by the caller. */ }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(e -> { collectPaths(JSON.getNodeFactory().textNode(e.getKey()), paths); collectPaths(e.getValue(), paths); });
        } else if (node.isArray()) node.forEach(n -> collectPaths(n, paths));
    }

    private void validateReceipt(ObjectNode receipt, Path directory) throws IOException {
        if (!absolute(receipt.path("buildRoot").asText()).equals(directory)) throw new IOException("Receipt run path mismatch");
        Path assets = outputPath(receipt.path("assetRoot").asText(), directory);
        if (!Files.isDirectory(assets, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing final asset directory");
        for (String kind : List.of("exe", "pac")) {
            Path file = outputPath(receipt.path(kind).asText(), directory);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) == 0
                    || !sha256(file).equals(receipt.path(kind + "Sha256").asText()))
                throw new IOException("Missing/changed final " + kind + ": " + file);
        }
    }

    private static Path outputPath(String value, Path directory) throws IOException {
        Path path = absolute(value);
        if (path.equals(directory) || !path.startsWith(directory)) throw new IOException("Output escapes owned run: " + path);
        noAliases(path); return path;
    }

    private static Path absolute(String value) throws IOException {
        try {
            if (value == null || value.isBlank() || !Path.of(value).isAbsolute()) throw new IOException("Expected absolute path");
            return Path.of(value).toAbsolutePath().normalize();
        } catch (RuntimeException bad) { throw new IOException("Invalid path", bad); }
    }

    private static void noAliases(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        Path current = normalized.getRoot();
        for (Path part : normalized) {
            current = current.resolve(part);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) continue;
            BasicFileAttributes attrs = Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attrs.isSymbolicLink() || attrs.isOther() || !current.toRealPath().equals(current))
                throw new IOException("Symlink/junction/special path is forbidden: " + current);
            if (!current.equals(normalized) && !attrs.isDirectory())
                throw new IOException("Non-directory output ancestor: " + current);
        }
    }

    private static void probeUnlocked(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, READ, WRITE, LinkOption.NOFOLLOW_LINKS)) {
            FileLock lock;
            try { lock = channel.tryLock(); }
            catch (OverlappingFileLockException inUse) { throw new IOException("Locked output: " + file, inUse); }
            if (lock == null) throw new IOException("Locked output: " + file);
            lock.release();
        }
    }

    private static ObjectNode object(Path file) throws IOException {
        noAliases(file);
        JsonNode node = JSON.readTree(file.toFile());
        if (!(node instanceof ObjectNode object)) throw new IOException("Expected object metadata: " + file);
        return object;
    }

    private static void writeJson(Path file, JsonNode value) throws IOException {
        writeText(file, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value));
    }

    private static void writeText(Path file, String value) throws IOException {
        noAliases(file);
        Path temporary = file.resolveSibling(".bhe-write-" + UUID.randomUUID() + ".tmp");
        String text = value.replace("\r\n", "\n").replace('\r', '\n').stripTrailing().replace("\n", "\r\n") + "\r\n";
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8, CREATE_NEW, WRITE);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            try { Files.deleteIfExists(temporary); } catch (IOException cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] block = new byte[1024 * 1024]; int size;
                while ((size = input.read(block)) != -1) digest.update(block, 0, size);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private void releaseUsage() throws IOException {
        if (usageLock != null) { usageLock.release(); usageLock = null; }
        if (usageChannel != null) { usageChannel.close(); usageChannel = null; }
    }

    @Override public void close() throws IOException {
        if (closed) return;
        IOException error = null;
        try { if (!committed && !failureHandled && owner != null) failed(new IOException("Build scope ended before publication")); }
        catch (IOException failure) { error = failure; }
        try { releaseUsage(); } catch (IOException failure) { if (error == null) error = failure; else error.addSuppressed(failure); }
        try { rootLock.release(); } catch (IOException failure) { if (error == null) error = failure; else error.addSuppressed(failure); }
        try { rootChannel.close(); } catch (IOException failure) { if (error == null) error = failure; else error.addSuppressed(failure); }
        closed = true;
        if (error != null) throw error;
    }

    private static final class Tree {
        final List<Path> files = new ArrayList<>(), directories = new ArrayList<>();
        final List<Map<String, String>> rows = new ArrayList<>();
    }

    private static final class CleanupFailure extends IOException {
        final boolean partial;
        CleanupFailure(String message, boolean partial, Throwable cause) { super(message, cause); this.partial = partial; }
    }
}
