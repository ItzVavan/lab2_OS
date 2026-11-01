import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Generator {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java Generator <rows> <cols>");
            return;
        }

        int rows = Integer.parseInt(args[0]);
        int cols = Integer.parseInt(args[1]);

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(System.out));

        // Метаданные
        out.writeInt(rows);
        out.writeInt(cols);

        // Нижнетреугольная матрица A
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j <= i; j++) {
                long val = ((i + j) % 50) + 1;
                out.writeLong(val);
            }
        }

        // Прямоугольная матрица B
        for (int i = 0; i < rows * cols; i++) {
            long val = (i % 50) + 1;
            out.writeLong(val);
        }

        out.flush();
    }
}