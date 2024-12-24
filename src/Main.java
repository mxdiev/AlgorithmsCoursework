import java.util.concurrent.*;

import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;

public class Main {

    private static final int THREAD_THRESHOLD = 1000; // Порог для многопоточности
    //private static final int[] SIZES = {0, 1_000_000, 2_000_000, 3_000_000, 4_000_000, 5_000_000, 6_000_000, 7_000_000, 8_000_000, 9_000_000, 10_000_000}; // Размеры массивов
    private static final int[] SIZES = {0, 1_000_000, 4_000_000, 7_000_000, 10_000_000, 13_000_000, 16_000_000, 19_000_000, 22_000_000, 25_000_000, 28_000_000};
    private static final String[] CASES = {"Sorted", "Random", "Reversed"}; // Типы массивов

    private static final String[] ALGORITHMS = {"SequentialQuickSort", "ParallelQuickSort",
            "SequentialMergeSort", "ParallelMergeSort", "SequentialShellSort", "ParallelShellSort"};



    // ------------------ Quicksort ------------------
    public static void quickSort(int[] array) {
        quickSortSequential(array, 0, array.length - 1);
    }

    private static void quickSortSequential(int[] array, int left, int right) {
        if (left < right) {
            int pivotIndex = partition(array, left, right);
            quickSortSequential(array, left, pivotIndex - 1);
            quickSortSequential(array, pivotIndex + 1, right);
        }
    }

    public static void parallelQuickSort(int[] array) {
        ExecutorService executor = Executors.newWorkStealingPool();
        try {
            parallelQuickSort(array, 0, array.length - 1, executor);
        } finally {
            executor.shutdown();
        }
    }

    private static void parallelQuickSort(int[] array, int left, int right, ExecutorService executor) {
        if (left >= right) {
            return;
        }

        if (right - left < THREAD_THRESHOLD) {
            quickSortSequential(array, left, right);
            return;
        }

        int pivotIndex = partition(array, left, right);
        Future<?> leftTask = executor.submit(() -> parallelQuickSort(array, left, pivotIndex - 1, executor));
        Future<?> rightTask = executor.submit(() -> parallelQuickSort(array, pivotIndex + 1, right, executor));

        try {
            leftTask.get();
            rightTask.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private static int medianOfThree(int[] array, int left, int right) {
        int mid = left + (right - left) / 2;
        if (array[left] > array[mid]) swap(array, left, mid);
        if (array[left] > array[right]) swap(array, left, right);
        if (array[mid] > array[right]) swap(array, mid, right);
        return mid;
    }

    private static int partition(int[] array, int left, int right) {
        int pivotIndex = medianOfThree(array, left, right);
        swap(array, pivotIndex, right); // Переместить pivot в конец
        int pivot = array[right];
        int i = left - 1;
        for (int j = left; j < right; j++) {
            if (array[j] <= pivot) {
                i++;
                swap(array, i, j);
            }
        }
        swap(array, i + 1, right);
        return i + 1;
    }

    // ------------------ MergeSort ------------------
    public static void mergeSort(int[] array) {
        mergeSortSequential(array, 0, array.length - 1);
    }

    private static void mergeSortSequential(int[] array, int left, int right) {
        if (left < right) {
            int middle = left + (right - left) / 2;
            mergeSortSequential(array, left, middle);
            mergeSortSequential(array, middle + 1, right);
            merge(array, left, middle, right);
        }
    }

    public static void parallelMergeSort(int[] array) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invoke(new MergeSortTask(array, 0, array.length - 1));
    }

    static class MergeSortTask extends RecursiveAction {
        private final int[] array;
        private final int left, right;

        MergeSortTask(int[] array, int left, int right) {
            this.array = array;
            this.left = left;
            this.right = right;
        }

        @Override
        protected void compute() {
            if (right - left < THREAD_THRESHOLD) {
                mergeSortSequential(array, left, right);
                return;
            }
            int middle = left + (right - left) / 2;
            MergeSortTask leftTask = new MergeSortTask(array, left, middle);
            MergeSortTask rightTask = new MergeSortTask(array, middle + 1, right);
            invokeAll(leftTask, rightTask);
            merge(array, left, middle, right);
        }
    }

    private static void merge(int[] array, int left, int middle, int right) {
        int n1 = middle - left + 1;
        int n2 = right - middle;

        int[] leftArray = new int[n1];
        int[] rightArray = new int[n2];

        System.arraycopy(array, left, leftArray, 0, n1);
        System.arraycopy(array, middle + 1, rightArray, 0, n2);

        int i = 0, j = 0, k = left;
        while (i < n1 && j < n2) {
            if (leftArray[i] <= rightArray[j]) {
                array[k++] = leftArray[i++];
            } else {
                array[k++] = rightArray[j++];
            }
        }

        while (i < n1) {
            array[k++] = leftArray[i++];
        }

        while (j < n2) {
            array[k++] = rightArray[j++];
        }
    }

    // ------------------ ShellSort ------------------
    public static void shellSort(int[] array) {
        int n = array.length;
        for (int gap = n / 2; gap > 0; gap /= 2) {
            for (int i = gap; i < n; i++) {
                int key = array[i];
                int j = i;
                while (j >= gap && array[j - gap] > key) {
                    array[j] = array[j - gap];
                    j -= gap;
                }
                array[j] = key;
            }
        }
    }

    public static void parallelShellSort(int[] array) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invoke(new ShellSortTask(array, array.length));
    }

    static class ShellSortTask extends RecursiveAction {
        private final int[] array;
        private final int n;

        public ShellSortTask(int[] array, int n) {
            this.array = array;
            this.n = n;
        }

        @Override
        protected void compute() {
            for (int gap = n / 2; gap > 0; gap /= 2) {
                if (gap > n / 3) {
                    ParallelGapSortTask gapTask = new ParallelGapSortTask(array, gap);
                    invokeAll(gapTask);
                } else {
                    sequentialSort(array, gap);
                }
            }
        }

        private void sequentialSort(int[] array, int gap) {
            int n = array.length;
            for (int i = gap; i < n; i++) {
                int key = array[i];
                int j = i;
                while (j >= gap && array[j - gap] > key) {
                    array[j] = array[j - gap];
                    j -= gap;
                }
                array[j] = key;
            }
        }
    }

    static class ParallelGapSortTask extends RecursiveAction {
        private final int[] array;
        private final int gap;

        public ParallelGapSortTask(int[] array, int gap) {
            this.array = array;
            this.gap = gap;
        }

        @Override
        protected void compute() {
            int n = array.length;
            int numTasks = Runtime.getRuntime().availableProcessors();
            int step = n / numTasks;

            for (int i = 0; i < numTasks; i++) {
                int start = i * step;
                int end = (i == numTasks - 1) ? n : start + step;

                for (int j = start + gap; j < end; j++) {
                    int key = array[j];
                    int k = j;
                    while (k >= gap && array[k - gap] > key) {
                        array[k] = array[k - gap];
                        k -= gap;
                    }
                    array[k] = key;
                }
            }
        }
    }




    // ------------------ Utility Functions ------------------
    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public static boolean isSorted(int[] array) {
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i - 1]) {
                return false;
            }
        }
        return true;
    }

    private static void generateArray(int[] array, String caseType) {
        int n = array.length;
        switch (caseType) {
            case "Sorted" -> {
                for (int i = 0; i < n; i++) array[i] = i;
            }
            case "Random" -> {
                for (int i = 0; i < n; i++) array[i] = (int) (Math.random() * n);
            }
            case "Reversed" -> {
                for (int i = 0; i < n; i++) array[i] = n - i;
            }
        }
    }

    //------------------ Main Function ------------------

    public static void main(String[] args) {
        String outputFile = "C:\\Users\\maximus\\Desktop\\results.csv";


        System.out.println(Runtime.getRuntime().availableProcessors());

        try (FileWriter writer = new FileWriter(outputFile)) {
            // Заголовок CSV-файла
            writer.write("Case,Size,Algorithm,Time(ms)\n");

            for (String caseType : CASES) {
                System.out.println("\nCase: " + caseType);

                for (int size : SIZES) {
                    System.out.println("Array size: " + size);
                    int[] baseArray = new int[size];

                    // Генерация массива
                    generateArray(baseArray, caseType);

                    // Клонируем массив для каждой сортировки
                    for (int i = 0; i < ALGORITHMS.length; i++) {
                        int[] array = baseArray.clone();
                        long startTime = System.currentTimeMillis();

                        // Выполнение нужной сортировки
                        switch (i) {
                            case 0 -> quickSort(array);
                            case 1 -> parallelQuickSort(array);
                            case 2 -> mergeSort(array);
                            case 3 -> parallelMergeSort(array);
                            case 4 -> shellSort(array);
                            case 5 -> parallelShellSort(array);
                        }

                        long endTime = System.currentTimeMillis();
                        long timeElapsed = endTime - startTime;

                        // Запись результата в файл
                        if (isSorted(array)) {
                            writer.write(String.format("%s,%d,%s,%d\n", caseType, size, ALGORITHMS[i], timeElapsed));
                            System.out.printf("%s -> Time: %d ms\n", ALGORITHMS[i], timeElapsed);
                        } else {
                            writer.write(String.format("%s,%d,%s,%d\n", caseType, size, ALGORITHMS[i], timeElapsed));
                            System.out.printf("%s -> Time: %d ms\n", ALGORITHMS[i], timeElapsed);
                            System.out.println("NOTSORTED");
                        }
                    }
                }
            }
            System.out.println("Results written to " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
