package pc.quicksort;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class QuickSort {
	private static final int THRESHOLD = 10_000;

	public static int partition(int[] array, int low, int high) {
		int pivot = array[low];
		int i = low - 1;
		int j = high + 1;
		while (true) {
			do {
				i++;
			} while (array[i] < pivot);

			do {
				j--;
			} while (array[j] > pivot);

			if (i >= j) {
				return j;
			}
			swap(array, i, j);
		}
	}

	public static void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}

	public static void quickSort(int[] array, int low, int high) {
		if (low < high) {
			int pi = partition(array, low, high);
			quickSort(array, low, pi);
			quickSort(array, pi + 1, high);
		}
	}

	public static void parQuickSort(int[] array) {
		if (array == null || array.length <= 1) {
			return;
		}
		ForkJoinPool.commonPool().invoke(new QuickSortTask(array, 0, array.length - 1));
	}

	public static void parQuickSort(int[] array, int parallelism) {
		if (array == null || array.length <= 1) {
			return;
		}
		ForkJoinPool pool = new ForkJoinPool(parallelism);
		try {
			pool.invoke(new QuickSortTask(array, 0, array.length - 1));
		} finally {
			pool.shutdown();
		}
	}

	private static class QuickSortTask extends RecursiveAction {
		private static final long serialVersionUID = 1L;

		private final int[] array;
		private final int low;
		private final int high;

		QuickSortTask(int[] array, int low, int high) {
			this.array = array;
			this.low = low;
			this.high = high;
		}

		@Override
		protected void compute() {
			if (low >= high) {
				return;
			}

			int size = high - low + 1;
			if (size <= THRESHOLD) {
				quickSort(array, low, high);
				return;
			}

			int pivotIndex = partition(array, low, high);
			invokeAll(
					new QuickSortTask(array, low, pivotIndex),
					new QuickSortTask(array, pivotIndex + 1, high));
		}
	}

	public static int[] generateRandomArray(int size) {
		Random rand = new Random();
		int[] result = new int[size];
		for (int i = 0; i < size; i++) {
			result[i] = rand.nextInt();
		}
		return result;
	}
}
