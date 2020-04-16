package edu.upenn.cis455.hw1;
import java.util.*;
import java.nio.file.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class HttpResponse {
	
		
		private final String server = "Server: 555WebServer/1.0dev_Ransford";
		private String statusLine;
		private String body;
		private String contentType;
		private String fileType;
		private String contentLength;
		private byte[] message;
		private int result;
	//private StringBuilder list_dir;
		
		private boolean hasMessage;
		private boolean isDirectory = true;
		
		public HttpResponse(int code) { //default Not Found
			
			switch(code){
				case 404:
					statusLine = "HTTP/1.1 404 NOT FOUND\r\n";
					body = "<HTML>" + "<HEAD><TITLE>404</TITLE></HEAD>" + "<BODY>404 Not Found"+"</BODY></HTML>";
					break;
				case 400:
					statusLine = "HTTP/1.1 400 BAD REQUEST\r\n";
					body = "<HTML>" + "<HEAD><TITLE>400</TITLE></HEAD>" + "<BODY>400 BAD REQUEST"+"</BODY></HTML>";
					break;
				case 304:
					statusLine = "HTTP/1.1 304 NOT Modified\r\n";
					body = "<HTML>" + "<HEAD><TITLE>304</TITLE></HEAD>" + "<BODY>304 Not Modified"+"</BODY></HTML>";
					break;
				case 301:
					statusLine = "HTTP/1.1 301 Moved Permanently\r\n";
					body = "<HTML>" + "<HEAD><TITLE>301</TITLE></HEAD>" + "<BODY>301 RESOURCE MOVED"+"</BODY></HTML>";
					break;
				case 405:
					statusLine = "HTTP/1.1 405 Method Not Allowed\r\n";
					body = "<HTML>" + "<HEAD><TITLE>301</TITLE></HEAD>" + "<BODY>301 RESOURCE MOVED"+"</BODY></HTML>";
					break;
				default:
					statusLine = "HTTP/1.1 500 Server Error\r\n";
					body = "<HTML>" + "<HEAD><TITLE>500</TITLE></HEAD>" + "<BODY>500 SERVER ERROR"+"</BODY></HTML>";
					
			}
			
			contentType = "text/html";
			hasMessage = false;
			result = 0;
			
		}
		
		public HttpResponse(File file, String resource, String home, int port) { // Found
			
			statusLine = "HTTP/1.1 200 OK\r\n"; //FILE FOUND
						
			StringBuilder list_dir = new StringBuilder();
			list_dir.append("<!DOCTYPE html>");
			list_dir.append("<html>");
			list_dir.append("<head>");
			//list_dir.append("Index of " + resource + '/');
			list_dir.append("</head>");
			list_dir.append("<body>");
			list_dir.append("<h1>" + "Index of " + resource + "</h1>");
			
			if(file.isDirectory()) {
				
				System.out.println("Root Directory Path is: " + file.getPath());
				System.out.println("Home is: " + home);
				if(!(file.getPath().equals(home)) & file.getParentFile() != null) {
					File parent = file.getParentFile();
					String link = String.format("<ul><li><a href=\"http://localhost:%d%s/\" target=\"_blank\">%s</a></li></ul>", port, parent.getPath(), "../");
					list_dir.append(link);
				}
				
				File[] files = file.listFiles();
				//num_files = files.length;
				if(files == null) { //forbidden
					statusLine = "HTTP/1.1 403 FORBIDDEN \r\n";
					hasMessage = false;
					result = -1;
					return;
				}else {
					for(File f : files) {
						String linkedFile = resource+f.getName();
						String display = (f.isDirectory()) ? f.getName() + '/' : f.getName();
						String link = String.format("<ul><li><a href=\"http://localhost:%d%s/\" target=\"_blank\">%s</a></li></ul>", port, linkedFile, display);
						list_dir.append(link);
					}				
				}
				
				list_dir.append("</body>");
				list_dir.append("</html>");
				this.contentType = "Content-Type: text/html \r\n";
				this.body = list_dir.toString();
				this.contentLength = "Content-Length: " + Integer.toString(body.length()) + "\r\n";
				isDirectory = true;
				hasMessage = true;
			}
			else {
				isDirectory = false;
				int len = (int) file.length();
				System.out.println("File length is " + len);
				message = new byte[(int) len];
				try {
					 fileType = Files.probeContentType(file.toPath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (fileType == null) {
					fileType = "application/octet-stream"; //default						
				}
				
				this.contentType = String.format("Content-type: %s \r\n", fileType);
				this.contentLength = "Content-Length: " + Integer.toString(len) + "\r\n";
				
				FileInputStream inStream = null;
				try {
					inStream = new FileInputStream(file);
					inStream.read(message);
					hasMessage = true;
					result = 1;
				} catch (FileNotFoundException e) {
					statusLine = "HTTP/1.1 404 NOT FOUND\r\n\r\n";
					body = "<HTML>" + "<HEAD><TITLE>404</TITLE></HEAD>" + "<BODY>404 Not Found"+"</BODY></HTML>";
					hasMessage = false;
					
				} catch(IOException a) {
					statusLine = "HTTP/1.1 500 Server Error\r\n\r\n";
					body = "<HTML>" + "<HEAD><TITLE>500</TITLE></HEAD>" + "<BODY>500 SERVER ERROR"+"</BODY></HTML>";
					hasMessage = false;
				}
				
				try {
					inStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
									
		}	
		
		public String getStatusline() {
			return this.statusLine;
		}
		
		public byte[] getBody() {
			return (!isDirectory) ? message : this.body.getBytes();
		}
		
		public String getServer() {
			return this.server;
		}
		public String getType() {
			return this.contentType;
		}
		
		public String getContentLength() {
			return this.contentLength;
		}
		public boolean hasMessage() {
			return this.hasMessage;
		}
		public int getResult() {
			return this.result;
		}
		
}
