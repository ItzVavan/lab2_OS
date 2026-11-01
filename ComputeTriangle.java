import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ComputeTriangle {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 2) {
            System.err.println("Usage: java ComputeTriangle <threads> <mode: static|dynamic> [chunk_size]");
            return;
        }

        int numThreads = Integer.parseInt(args[0]);
        String mode = args[1];
        int chunkSize = (args.length >= 3) ? Integer.parseInt(args[2]) : 4;

        long startIpc = System.nanoTime();
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in));

        int N = in.readInt();
        int M = in.readInt();

        int triangleSize = (int) ((long) N * (N + 1) / 2);
        long[] APacked = new long[triangleSize];
        for (int i = 0; i < triangleSize; i++) {
            APacked[i] = in.readLong();
        }

        long[] B = new long[N * M];
        for (int i = 0; i < B.length; i++) {
            B[i] = in.readLong();
        }
        long endIpc = System.nanoTime();
        double ipcSec = (endIpc - startIpc) / 1e9;

        long[] C = new long[N * M];
        AtomicInteger currentRow = new AtomicInteger(0);
        AtomicInteger processedRows = new AtomicInteger(0);

        long startCompute = System.nanoTime();
        Thread[] threads = new Thread[numThreads];

        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            threads[t] = new Thread(() -> {
                if ("static".equalsIgnoreCase(mode)) {
                    int rowsPerTh = (N + numThreads - 1) / numThreads;
                    int rStart = tid * rowsPerTh;
                    int rEnd = Math.min(N, rStart + rowsPerTh);

                    for (int i = rStart; i < rEnd; i++) {
                        int rowOffsetA = (int) ((long) i * (i + 1) / 2);
                        int cOffset = i * M;
                        for (int k = 0; k <= i; k++) {
                            long aVal = APacked[rowOffsetA + k];
                            int bOffset = k * M;
                            for (int j = 0; j < M; j++) {
                                C[cOffset + j] += aVal * B[bOffset + j];
                            }
                        }
                        processedRows.incrementAndGet();
                    }
                } else {
                    while (true) {
                        int rStart = currentRow.getAndAdd(chunkSize);
                        if (rStart >= N) break;
                        int rEnd = Math.min(N, rStart + chunkSize);

                        for (int i = rStart; i < rEnd; i++) {
                            int rowOffsetA = (int) ((long) i * (i + 1) / 2);
                            int cOffset = i * M;
                            for (int k = 0; k <= i; k++) {
                                long aVal = APacked[rowOffsetA + k];
                                int bOffset = k * M;
                                for (int j = 0; j < M; j++) {
                                    C[cOffset + j] += aVal * B[bOffset + j];
                                }
                            }
                        }
                        processedRows.addAndGet(rEnd - rStart);
                    }
                }
            });
            threads[t].start();
        }

        for (Thread th : threads) {
            th.join();
        }

        long endCompute = System.nanoTime();
        double computeSec = (endCompute - startCompute) / 1e9;

        long checksum = 0;
        for (long val : C) checksum += val;

        System.out.println("Время передачи (IPC): " + ipcSec + " сек.");
        System.out.println("Время умножения: " + computeSec + " сек.");
        System.out.println("Контрольная сумма: " + checksum);
    }
}