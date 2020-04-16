package edu.upenn.cis455.hw1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.*;

class HttpServer extends Thread {
   
   private ServerSocket serverSocket;
   private String relativeDirectory;
   private int port;

   private BlockingQueue queue;
   ArrayList<HttpWorker> pool;
   private int poolSize = 10;
   private volatile boolean shutdown;
   private Thread dispatcher;
   
   
   public HttpServer(int port, String relativeDirectory) {
	   try {
		serverSocket = new ServerSocket(port);
		 this.relativeDirectory = relativeDirectory;
		 this.port = port;
		queue = new BlockingQueue(poolSize);
		pool = new ArrayList<HttpWorker>();
		this.shutdown = false;
		} catch (IOException e) {
			
			e.printStackTrace();
		}
   }
   
   public BlockingQueue getQueue() {
	  return this.queue;
   }
   
   
   public int getPort() {
	   return this.port;
   }
   
   public String getRoot() {
	   return this.relativeDirectory;
   }
   

   public Thread getDispatcher() {
	   return this.dispatcher;
   }
   
   public ArrayList<HttpWorker> getPool() {
	   return this.pool;
   }
     
   public synchronized void  sendShutdown(HttpWorker worker) {
	   this.shutdown = true;
	   System.out.println("RECEIVED FROM: " + worker.getStateId());
   }
   
   public boolean getShutdown() {
	   return this.shutdown;
   }
   
   public ServerSocket getSocket() {
	   return this.serverSocket;
   }
   
   public String getControlPanel() {
	  StringBuilder builder = new StringBuilder();
	  builder.append("<!DOCTYPE html>");
	  builder.append("<html>");
	  builder.append("<head>");
	  builder.append("</head>");
	  builder.append("<body>");
	  builder.append("<h1>Control Panel</h1>");
	  builder.append("<h2> *** Author: Ransford Antwi (ransford) ***</h2>");
	 
		for(HttpWorker worker: this.getPool()) {
		  builder.append(String.format("<ul><li>%s: %s</li></ul>", worker.getThreadState(), worker.getState()));
	  }
	  builder.append(String.format("<button onclick=\"window.location.href = 'http://localhost:%d/shutdown';\">Shutdown</button>", this.port));
	  builder.append("</body>");
	  builder.append("</html>");
		
	  return builder.toString();
   }
   
   public static void main(String args[])
  {
	  	if(args.length != 2) {
	  		System.out.println(" *** Author: Ransford Antwi (ransford)");
	  		System.exit(0);
	  	}
	  	int port = 0;
	  	String relativeDirectory;
	    try {
	        port = Integer.parseInt(args[0]);
	    } catch (NumberFormatException e) {
	        System.err.println("Port" + args[0] + " must be an integer.");
	        System.exit(1);
	    }  	
	  	relativeDirectory = args[1];
	  	HttpServer server = new HttpServer(port, relativeDirectory);
	  	
	  	int size = server.poolSize;
	  	ArrayList<HttpWorker> pool = server.getPool();
	  	
	  	/*initialize Workers  */
	  	for(int i = 0; i < size; i++) {
	  		HttpWorker worker = new HttpWorker(i,server);
	  		pool.add(worker);
	  		worker.start();
	  	}
	  		  	
	  	/*Dispatcher Thread continuously listens for requests and adds to queue  */
	  	server.dispatcher = Thread.currentThread();
	  	System.out.println("dispatcher thread ID: " + server.dispatcher.getId());
	  	while(!server.getShutdown()) {
	  		try {
				Socket client = server.serverSocket.accept();
				server.getQueue().enqueue(client);			
			} catch (IOException e) {
				if(server.getShutdown()) {
					System.out.println("caught socket close exception");
					break;
				}else {
					e.printStackTrace();
					continue;
				}
				
			}
	  	}
	  	
	  		System.out.println("Server received Shutdown command");	  		  		
	  	return;
	  }   
}
  
