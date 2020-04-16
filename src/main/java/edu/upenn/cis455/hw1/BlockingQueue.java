package edu.upenn.cis455.hw1;

import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueue {

	private Queue<Socket> queue;
	private int size;
	
	public BlockingQueue(int size) {
		queue = new LinkedList<Socket>();
		this.size = size;
	}
	
	public synchronized void enqueue(Socket client) {
		
		while(this.queue.size() == this.size) { //pool is full
			try {
				wait();
			} catch (InterruptedException e) { //TODO:
							
				System.out.println("Server received shutdown command");
			}
		}
		this.queue.add(client);
		if(this.queue.size() == 1) {
			notifyAll();
		}
				
	}
	
	public synchronized Socket dequeue() throws InterruptedException {
		while(queue.isEmpty()) {
				wait();								
			}
		
		if(queue.size() == size) {
			notifyAll();
		}
		
		Socket client = queue.remove();
		return client;
	}

	
}
