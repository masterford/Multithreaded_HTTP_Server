package edu.upenn.cis455.hw1;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
public class HttpWorker extends Thread{
	
	   private String relativeDirectory;
	   private int port;
	   private  BlockingQueue queue;
	   private String CLRF = "\r\n";
	   private volatile boolean isShutdown;
	   private Socket client;
	   private String shutdownCommand;
	   private String control;
	   private HttpServer server;
	   private String state;
	   private String invalidHeader = "Not Found";
	   private String closeHeader = "Connection:  close\r\n";
	   
	   private int id;
   
	   public HttpWorker(int id, HttpServer server) {
		   this.server = server;
		   this.id = id;
		   port = server.getPort();
		   relativeDirectory = server.getRoot();
		   isShutdown = false;
		   queue = server.getQueue();
		   shutdownCommand = "/shutdown";
		   control = "/control";
	   }
	   
	   public void run() {
		   while(!isShutdown) {
			   
			   BufferedReader in;
			  // try {
				    this.state = String.format("Thread %d : waiting", this.id); 
				    try {
				    	
						client = queue.dequeue();
					} catch (InterruptedException e1) {
						System.out.println("Thread received shutdown Interrupt");
						server.sendShutdown(this);
						this.finish();
						break;
					}
				    this.state = String.format("Thread %d : Received Connection", this.id); 
					// Read HTTP request from the client socket
			try {
					in = new BufferedReader(new InputStreamReader(client.getInputStream()));
					OutputStream clientStream = client.getOutputStream();
					String line = in.readLine(); 
					if(line == null) {
						//while(!client.)
						if(!client.isClosed()) {
							in = new BufferedReader(new InputStreamReader(client.getInputStream()));
							clientStream = client.getOutputStream();
							line = in.readLine(); 
							if(line == null) {
								in.close();
								client.close();							
								continue;
							}
						}else {
							continue;
						}											
					}
					String date = "Date: " + new Date().toString() + "\r\n";
					HttpRequest request = new HttpRequest(line, relativeDirectory);
					this.state = String.format("Thread %d : %s", this.id, request.getRequest());
					while (!line.isEmpty()) { 
						System.out.println(line);				
						line = in.readLine();
						request.parseHeader(line);
					}
					if(request.getResource().equals(shutdownCommand)) { //shutdown
						System.out.println("Thread received shutdown Command");
						String response = "HTTP/1.1 200 OK\r\n"; //FILE FOUND
						clientStream.write(response.getBytes());
						clientStream.write(date.getBytes());
						String body = "<HTML>" + "<HEAD><h1 style=\"color: #5e9ca0;\"><span style=\"color: #2b2301;\">Shutting Down Server- Goodbye :)</span></h1></HEAD></HTML>";
						String length =  "Content-Length: " + Integer.toString(body.length()) + "\r\n";
						String type = "Content-type: text/html \r\n";
						clientStream.write(type.getBytes());
						clientStream.write(length.getBytes());
						clientStream.write(closeHeader.getBytes());
						clientStream.write(this.CLRF.getBytes());
						clientStream.write(body.getBytes());
						
						in.close();
					    clientStream.close();
						client.close();
						isShutdown = true;
						//this.finish();
											
						this.server.sendShutdown(this);	
						this.shutdown(server.getPool());
						
						break;
					}
					
					if(request.getResource().equals(control)) { //control
						String response = "HTTP/1.1 200 OK\r\n"; 
						clientStream.write(response.getBytes());
						clientStream.write(date.getBytes());
						String body = server.getControlPanel();
						String length =  "Content-Length: " + Integer.toString(body.length()) + "\r\n";
						String type = "Content-type: text/html \r\n";
						clientStream.write(type.getBytes());
						clientStream.write(length.getBytes());
						clientStream.write(closeHeader.getBytes());
						clientStream.write(this.CLRF.getBytes());
						clientStream.write(body.getBytes());
							
						in.close();
						clientStream.close();
						client.close();
						continue;
					}
					
					
					Method method = request.getRequestType();
					if(method == null) { //Invalid Request
						HttpResponse response = new HttpResponse(405);
						clientStream.write(response.getStatusline().getBytes());
						clientStream.write(date.getBytes());
						clientStream.write(closeHeader.getBytes());
						clientStream.write(this.CLRF.getBytes());
						
						clientStream.close();
						client.close();
						in.close();
						continue;		
						
					}                                                      
					String resource = relativeDirectory + request.getResource();
					System.out.println("Resource is " + resource);
					File file = new File(resource);
					
					
					if(!file.exists()) {
						HttpResponse response = new HttpResponse(404);
						clientStream.write(response.getStatusline().getBytes());
						clientStream.write(date.getBytes());
						clientStream.write(closeHeader.getBytes());
						clientStream.write(this.CLRF.getBytes());
						
						clientStream.close();
						client.close();
						in.close();
						continue;				
					} 
					if(!request.getIfModified().equals("Not Found")) { //check for presence of If Modified Header
						DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
						try {
							Date dateIf = format.parse(request.getIfModified());
							//String comp = new Date().toString();
							Date ref = format.parse("00:00:00 GMT, January 1, 1970");
							
							long last_modified = file.lastModified();
							long difference = dateIf.getTime() - ref.getTime();
							
							if(last_modified <= difference) { //File has not been modified since given date
								HttpResponse response = new HttpResponse(304);
								clientStream.write(response.getStatusline().getBytes());
								clientStream.write(date.getBytes());
								clientStream.write(closeHeader.getBytes());
								clientStream.write(this.CLRF.getBytes());
								
								client.close();
								clientStream.close();
								in.close();
								continue;
							}
						} catch (ParseException e) {
							e.printStackTrace();
							HttpResponse response = new HttpResponse(400); //couldn't parse date.
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(closeHeader.getBytes());
							clientStream.write(this.CLRF.getBytes());
							
							client.close();
							in.close();
							continue;							
						}
						System.out.println(date); // Sat Jan 02 00:00:00 GMT 2010
					}
					
					if(request.getSpecification() == 1 && request.getHost().equals(invalidHeader)) { //No Host Header found in this http1.1 request
						HttpResponse response = new HttpResponse(404);
						clientStream.write(response.getStatusline().getBytes());
						clientStream.write(date.getBytes());
						clientStream.write(closeHeader.getBytes());
						clientStream.write(this.CLRF.getBytes());
						
						client.close();
						in.close();
						clientStream.close();
						continue;
					}

						HttpResponse response = new HttpResponse(file, resource, relativeDirectory, port);
						
						if(response.getResult() < 0) { //error occurred
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(closeHeader.getBytes());
							clientStream.write(this.CLRF.getBytes());
						}
						else if(response.hasMessage()) { //reading a file
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(response.getType().getBytes());
							clientStream.write(response.getContentLength().getBytes());
							clientStream.write(closeHeader.getBytes());
							clientStream.write(this.CLRF.getBytes());
							
							if(method == Method.GET) {
							clientStream.write(response.getBody());
							}
						} else {
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(this.CLRF.getBytes());
							if(method == Method.GET) {
								clientStream.write(response.getBody());
							}
						}									
						client.close();
						in.close();
						clientStream.close();
															  
			   } catch (SocketTimeoutException s) {					
					System.out.println("Socket Timed Out");	
			   } catch (IOException e) {
				   e.printStackTrace();
				    this.finish();
				    break;
					//e.printStackTrace();					
				}
		   }  
		   return;
	   }
	   
	   public synchronized void shutdown(ArrayList<HttpWorker> threadPool) {
		   
		   for(HttpWorker worker : threadPool) {
			   
			   if(this.equals(worker)) {
				   System.out.println("WHATTA DOO");
				   continue;
			   }
			   worker.finish(); //Stop All threads
			  if(!worker.isInterrupted()) {
				  worker.interrupt();
			  }
		   }
		   server.getDispatcher().interrupt();
		   try {
			server.getSocket().close();
		} catch (IOException e) {
			System.out.println("Failed to close socket");
			e.printStackTrace();
		}
		   server.sendShutdown(this);
		   this.finish();
	   }
	   
	   public String getThreadState() {
		   return this.state;
	   }
	   
	   public int getStateId() {
		   return this.id;
	   }
	   
	   public void finish() {
		   this.isShutdown = true;
		   try {
			   if(client != null && !client.isClosed()) {
				   client.close();
			   }			
		} catch (IOException e) {
			System.out.println("Error Closing client");
			e.printStackTrace();
		}	   		   
		   if(!this.isInterrupted()) {
			   this.interrupt();
		   }		   
	   }
}
			  
