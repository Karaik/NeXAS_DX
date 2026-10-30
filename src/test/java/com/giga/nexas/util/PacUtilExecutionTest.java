package com.giga.nexas.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Controlled Process objects exercise the real lifecycle code without launching a native process. */
class PacUtilExecutionTest {
    @TempDir Path owned;

    @Test
    void commandUsesTheActualExecutableDirectoryAndOwnedTempEnvironment() throws Exception {
        Path executable = owned.resolve("operation/.nexas-tools/NexasPack.exe").toAbsolutePath().normalize();
        Path session = executable.getParent().resolve("session-example");
        Path input = owned.resolve("operation/flat").toAbsolutePath().normalize();
        ProcessBuilder builder = (ProcessBuilder) invoke("createProcessBuilder", new Class<?>[]{List.class, Path.class},
                List.of(executable.toString(), input.toString()), session);
        assertEquals(executable.getParent().toFile(), builder.directory());
        assertEquals(session.toString(), builder.environment().get("TEMP"));
        assertEquals(session.toString(), builder.environment().get("TMP"));
        assertTrue(builder.environment().get("PATH").startsWith(executable.getParent().toString() + java.io.File.pathSeparator));
        assertEquals(input.toString(), builder.command().get(1));
        assertTrue(builder.redirectErrorStream());
    }

    @Test
    void ownedToolsCannotBePlacedInsideTheFolderBeingPacked() throws Exception {
        Path flat = owned.resolve("flat").toAbsolutePath().normalize();
        assertThrows(IOException.class, () -> invoke("validatePackToolDirectory", new Class<?>[]{Path.class, Path.class},
                flat, flat.resolve(".nexas-tools")));
        invoke("validatePackToolDirectory", new Class<?>[]{Path.class, Path.class}, flat, owned.resolve(".nexas-tools"));
    }

    @Test
    void stdoutEofDoesNotReturnBeforeTheOwnedProcessActuallyExits() throws Exception {
        ControlledProcess process = new ControlledProcess("native output\n", true, 0);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var result = executor.submit(() -> collect(process, false, null));
            assertTrue(process.waitEntered.await(2, TimeUnit.SECONDS));
            assertFalse(result.isDone(), "stdout is already at EOF, but the process has not exited");
            process.finish(0);
            assertEquals("native output" + System.lineSeparator(), result.get(2, TimeUnit.SECONDS));
            assertFalse(process.isAlive());
            assertTrue(process.confirmedWaits.get() > 0);
            assertEquals(0, process.destroyCalls.get());
        } finally {
            process.finish(0);
            executor.shutdownNow();
        }
    }

    @Test
    void nonzeroNativeExitIsReportedWithItsOutput() throws Exception {
        ControlledProcess process = new ControlledProcess("native failed\n", false, 17);
        IOException error = assertThrows(IOException.class, () -> collect(process, false, null));
        assertTrue(error.getMessage().contains("17"));
        assertTrue(error.getMessage().contains("native failed"));
        assertTrue(process.confirmedWaits.get() > 0);
        assertFalse(process.isAlive());
    }

    @Test
    void compressionInputIsClosedAndTheRealExitStillControlsCompletion() throws Exception {
        ControlledProcess process = new ControlledProcess("done\n", false, 0);
        assertEquals("done" + System.lineSeparator(), collect(process, true, "4"));
        assertEquals("4" + System.lineSeparator(), process.input.toString(StandardCharsets.UTF_8));
        assertTrue(process.input.closed);
        assertTrue(process.confirmedWaits.get() > 0);
    }

    @Test
    void stdinFailureTerminatesAndConfirmsTheOwnedProcessBeforeThrowing() throws Exception {
        ControlledProcess process = new ControlledProcess("", true, 0);
        process.failInput = true;
        assertThrows(IOException.class, () -> collect(process, true, "4"));
        assertFalse(process.isAlive());
        assertTrue(process.destroyCalls.get() > 0);
        assertTrue(process.confirmedWaits.get() > 0);
        assertTrue(process.output.closed);
    }

    @Test
    void stdoutFailureCannotLeaveTheNativeProcessRunning() throws Exception {
        ControlledProcess process = new ControlledProcess("", true, 0);
        process.failOutput = true;
        IOException error = assertThrows(IOException.class, () -> collect(process, false, null));
        assertTrue(error.getMessage().contains("read failure"));
        assertFalse(process.isAlive());
        assertTrue(process.forcedCalls.get() > 0);
        assertTrue(process.confirmedWaits.get() > 0);
    }

    @Test
    void interruptForcesAnUncooperativeOwnedProcessToExitAndRestoresInterruptStatus() throws Exception {
        ControlledProcess process = new ControlledProcess("", true, 0);
        process.ignoreDestroy = true;
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try { collect(process, false, null); }
            catch (Throwable failure) { error.set(failure); }
            finally { interrupted.set(Thread.currentThread().isInterrupted()); }
        });
        try {
            caller.start();
            assertTrue(process.waitEntered.await(2, TimeUnit.SECONDS));
            caller.interrupt();
            caller.join(3000);
            assertFalse(caller.isAlive());
            assertInstanceOf(InterruptedIOException.class, error.get());
            assertTrue(interrupted.get());
            assertFalse(process.isAlive());
            assertTrue(process.destroyCalls.get() > 0);
            assertTrue(process.forcedCalls.get() > 0);
            assertTrue(process.confirmedWaits.get() > 0);
        } finally {
            process.finish(0);
            caller.interrupt();
            caller.join(2000);
        }
    }

    private static String collect(Process process, boolean input, String text) throws Exception {
        return (String) invoke("collectProcess", new Class<?>[]{Process.class, boolean.class, String.class, java.nio.charset.Charset.class},
                process, input, text, StandardCharsets.UTF_8);
    }

    private static Object invoke(String name, Class<?>[] types, Object... arguments) throws Exception {
        var method = PacUtil.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        try { return method.invoke(null, arguments); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception exception) throw exception;
            if (error.getCause() instanceof Error failure) throw failure;
            throw error;
        }
    }

    private static final class ControlledProcess extends Process {
        final CountDownLatch exited = new CountDownLatch(1), waitEntered = new CountDownLatch(1);
        final AtomicInteger confirmedWaits = new AtomicInteger(), destroyCalls = new AtomicInteger(), forcedCalls = new AtomicInteger();
        final TrackingInput output;
        final TrackingOutput input = new TrackingOutput();
        volatile boolean alive, ignoreDestroy, failInput, failOutput;
        volatile int code;

        ControlledProcess(String text, boolean alive, int code) {
            this.output = new TrackingInput(text.getBytes(StandardCharsets.UTF_8)); this.alive = alive; this.code = code;
            if (!alive) exited.countDown();
        }

        void finish(int status) { code = status; alive = false; exited.countDown(); }
        @Override public OutputStream getOutputStream() {
            if (!failInput) return input;
            return new OutputStream() { @Override public void write(int value) throws IOException { throw new IOException("write failure"); } };
        }
        @Override public InputStream getInputStream() {
            if (!failOutput) return output;
            return new InputStream() { @Override public int read() throws IOException { throw new IOException("read failure"); } };
        }
        @Override public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        @Override public int waitFor() throws InterruptedException { waitEntered.countDown(); exited.await(); confirmedWaits.incrementAndGet(); return code; }
        @Override public boolean waitFor(long time, TimeUnit unit) throws InterruptedException {
            waitEntered.countDown(); boolean done = exited.await(time, unit); if (done) confirmedWaits.incrementAndGet(); return done;
        }
        @Override public int exitValue() { if (alive) throw new IllegalThreadStateException(); return code; }
        @Override public boolean isAlive() { return alive; }
        @Override public void destroy() { destroyCalls.incrementAndGet(); if (!ignoreDestroy) finish(143); }
        @Override public Process destroyForcibly() { forcedCalls.incrementAndGet(); finish(137); return this; }
    }

    private static final class TrackingInput extends ByteArrayInputStream {
        volatile boolean closed;
        TrackingInput(byte[] bytes) { super(bytes); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }

    private static final class TrackingOutput extends ByteArrayOutputStream {
        volatile boolean closed;
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
}
