package edu.upenn.cis455.hw1;
import java.util.*;

enum Method
{
	GET,
	HEAD;
}

public class HttpRequest {
	
	private Map<String, String> headerMap; 
	private Method method;
	private String invalidHeader = "Not Found";
	private String resource;
	private int specification; //HTTP specification, e.g. 1.0 or 1.1
	private String request;
	
	public HttpRequest(String requestLine, String relativeDirectory) {
		
		headerMap = new HashMap<String, String>();
		
		String [] requests = requestLine.split(" ");
		if (requests[0].equals("GET")) {
			this.method = Method.GET;
		} else if(requests[0].equals("HEAD")) {
			this.method = Method.HEAD;
		}else {
			this.method = null;
		}
		
		if (requests.length == 3) {
			this.request = requests[1];
			resource = requests[1].startsWith(relativeDirectory) ? requests[1].substring(relativeDirectory.length(), requests[1].length()) : requests[1];
			specification = (requests[2].equals("HTTP/1.1")) ? 1 : 0;			
		} else {
			this.method = null;
		}
		
	}
	
	public boolean isValidRequestMethod() {
		return this.method != null;
	}
	
	public void parseHeader(String line) {
		String[] split = line.split(":", 2); //split once
		if(split.length == 2) {
			headerMap.put(split[0], split[1]);
		}
	}
	
	public Method getRequestType() {
		return this.method;
	}
	
	public String getRequest() {
		return this.request;
	}
	public String getHost() {
		return headerMap.getOrDefault("Host", invalidHeader);
	}
	
	public String getDate() {
		return headerMap.getOrDefault("Date", invalidHeader);
	}
	
	public String getIfModified() {
		return headerMap.getOrDefault("If-Modified-Since", invalidHeader);
	}
	
	public String getIfUnModified() {
		return headerMap.getOrDefault("If-Unmodified-Since", invalidHeader);
	}
	
	public String getContentLength() {
		return headerMap.getOrDefault("Content-Length", invalidHeader);
	}
	
	public String getContentType() {
		return headerMap.getOrDefault("Content-Type", invalidHeader);
	}
	public String getConnection() {
		return headerMap.getOrDefault("Connection", invalidHeader);
	}
	
	public String getResource() {
		
		if(this.resource.startsWith("http")) {
			int index = resource.indexOf('/', 7);
			return this.resource.substring(index, this.resource.length());
		}
		return this.resource;
	}
	
	public int getSpecification() {
		return this.specification;
	}
	
	public String printHeaders() {
		return headerMap.keySet().toString();
	}
	
	public String printAll() {
		StringBuffer result = new StringBuffer();
		for (String key : headerMap.keySet()) {
			result.append(key + ": " + headerMap.get(key) + "\n");		
		}
		return result.toString();
	}
	
	
}
