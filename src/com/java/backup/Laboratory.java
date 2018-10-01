package com.java.backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Laboratory implements Runnable {
	private long time1;
	private ArrayList<Integer> realNumber;
	private Object lock;
	private int coun;
	private int capacity;

	public Laboratory() {
		lock = new Object();
		capacity = 5000000;
		coun = 0;
		time1 = System.currentTimeMillis();
		realNumber = new ArrayList<Integer>(capacity);
	}

	@Override
	public void run() {
		synchronized (lock) {
			while (coun < capacity) {
				lock.notify();
				realNumber.add(coun);
				coun++;
			}
		}
		System.out.println(Thread.currentThread().getName() + ", "
				+ (System.currentTimeMillis() - time1));
		System.out.println(realNumber.get(realNumber.size() - 1));
	}

	public static void main(String[] args) {
		try {
			Scanner sc = new Scanner(new BufferedReader(new InputStreamReader(
					new FileInputStream("D:" + File.separator + "FLAC Library"
							+ File.separator + "Bazzi - Mine" + File.separator
							+ "Bazzi - Mine.lrc"), "UTF-8")));
			// while (sc.hasNextLine()) {
			// System.out.println(sc.nextLine());
			// }
			sc.close();
			LinkedList<String> lists = new LinkedList<String>();
			lists.addFirst("Nicotine");
			lists.addLast("Morphine");
			ListIterator<String> iterator = lists.listIterator();
			iterator.next();
			iterator.add("Heroine");
			System.out.println(lists);

			ExecutorService executor = Executors.newFixedThreadPool(10);
			for (int i = 0; i < 10; i++) {
				executor.execute(new Laboratory());
			}
			executor.shutdown();
			
			
//			Thread adder1 = new Thread(new Laboratory(), "A");
//			Thread adder2 = new Thread(new Laboratory(), "B");
//			Thread adder3 = new Thread(new Laboratory(), "C");
//			Thread adder4 = new Thread(new Laboratory(), "D");
//			Thread adder5 = new Thread(new Laboratory(), "E");
//			Thread adder6 = new Thread(new Laboratory(), "F");
//			Thread adder7 = new Thread(new Laboratory(), "G");
//			Thread adder8 = new Thread(new Laboratory(), "H");
//			Thread adder9 = new Thread(new Laboratory(), "I");
//			Thread adder10 = new Thread(new Laboratory(), "J");
//			adder1.start();
//			adder2.start();
//			adder3.start();
//			adder4.start();
//			adder5.start();
//			adder6.start();
//			adder7.start();
//			adder8.start();
//			adder9.start();
//			adder10.start();
			// How about notify/notifyAll/wait?
			// long justnow = System.currentTimeMillis();
			// Laboratory lab = new Laboratory();
			// for (int i = 0; i < 5000000; i++) {
			// lab.realNumber.add(i);
			// }
			// System.out.println(System.currentTimeMillis() - justnow);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
