import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Matpower {
    // Массивы для двойной буферизации
    private static long[] rRead;
    private static long[] rWrite;

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 3) {
            System.err.println("Usage: java Matpower <N> <power> <threads>");
            return;
        }

        int N = Integer.parseInt(args[0]);
        int P = Integer.parseInt(args[1]);
        int numThreads = Integer.parseInt(args[2]);

        long[] A = new long[N * N];
        for (int i = 0; i < A.length; i++) A[i] = (i % 10) + 1;

        rRead = Arrays.copyOf(A, A.length);
        rWrite = new long[N * N];

        // Барьер с фазовым действием смены буферов
        CyclicBarrier barrier = new CyclicBarrier(numThreads, () -> {
            long[] tmp = rRead;
            rRead = rWrite;
            rWrite = tmp;
        });

        long startTime = System.nanoTime();
        Thread[] threads = new Thread[numThreads];
        int rowsPerTh = (N + numThreads - 1) / numThreads;

        for (int t = 0; t < numThreads; t++) {
            final int tid = t;
            threads[t] = new Thread(() -> {
                int rStart = tid * rowsPerTh;
                int rEnd = Math.min(N, rStart + rowsPerTh);

                try {
                    for (int step = 2; step <= P; step++) {
                        // Очистка и вычисление текущего блока строк
                        for (int i = rStart; i < rEnd; i++) {
                            int rWriteOffset = i * N;
                            for (int j = 0; j < N; j++) {
                                rWrite[rWriteOffset + j] = 0;
                            }

                            int rReadOffset = i * N;
                            for (int k = 0; k < N; k++) {
                                long rVal = rRead[rReadOffset + k];
                                int aOffset = k * N;
                                for (int j = 0; j < N; j++) {
                                    rWrite[rWriteOffset + j] += rVal * A[aOffset + j];
                                }
                            }
                        }

                        // Синхронизация всех потоков на шаге степени
                        barrier.await();
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
            threads[t].start();
        }

        for (Thread th : threads) {
            th.join();
        }

        long endTime = System.nanoTime();
        double elapsedSec = (endTime - startTime) / 1e9;

        long checksum = 0;
        for (long val : rRead) checksum += val;

        System.out.println("Время возведения в степень: " + elapsedSec + " сек.");
        System.out.println("Контрольная сумма: " + checksum);
    }
}