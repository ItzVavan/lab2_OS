import java.util.concurrent.atomic.AtomicInteger;

public class Matmul {
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 4) {
            System.err.println("Usage: java Matmul <M> <K> <N> <threads>");
            return;
        }

        int M = Integer.parseInt(args[0]);
        int K = Integer.parseInt(args[1]);
        int N = Integer.parseInt(args[2]);
        int numThreads = Integer.parseInt(args[3]);

        long[] A = new long[M * K];
        long[] B = new long[K * N];
        long[] C = new long[M * N];

        for (int i = 0; i < A.length; i++) A[i] = (i % 100) + 1;
        for (int i = 0; i < B.length; i++) B[i] = ((i * 2) % 100) + 1;

        AtomicInteger processedRows = new AtomicInteger(0);
        long startTime = System.nanoTime();

        Thread[] threads = new Thread[numThreads];
        int rowsPerThread = (M + numThreads - 1) / numThreads;

        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            threads[t] = new Thread(() -> {
                int rowStart = tid * rowsPerThread;
                int rowEnd = Math.min(M, rowStart + rowsPerThread);

                // Оптимизированный порядок циклов
                for (int i = rowStart; i < rowEnd; i++) {
                    int aRowOffset = i * K;
                    int cRowOffset = i * N;
                    for (int k = 0; k < K; k++) {
                        long aVal = A[aRowOffset + k];
                        int bRowOffset = k * N;
                        for (int j = 0; j < N; j++) {
                            C[cRowOffset + j] += aVal * B[bRowOffset + j];
                        }
                    }
                    processedRows.incrementAndGet();
                }
            });
            threads[t].start();
        }

        // Поток-наблюдатель прогресса
        Thread progressThread = new Thread(() -> {
            try {
                while (true) {
                    int current = processedRows.get();
                    int percent = (int) ((current * 100L) / M);
                    System.out.print("\rПрогресс: " + percent + "%");
                    if (current >= M) break;
                    Thread.sleep(50);
                }
            } catch (InterruptedException ignored) {}
        });
        progressThread.start();

        for (Thread th : threads) {
            th.join();
        }
        progressThread.join();

        long endTime = System.nanoTime();
        double elapsedSec = (endTime - startTime) / 1e9;

        long checksum = 0;
        for (long val : C) {
            checksum += val;
        }

        System.out.println("\nВремя умножения: " + elapsedSec + " сек.");
        System.out.println("Контрольная сумма: " + checksum);
    }
}