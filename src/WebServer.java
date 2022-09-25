import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class WebServer extends Thread {

	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "index.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	// port to listen connection
	static final int PORT = 8080;
	
	// verbose mode
	static final boolean verbose = true;
	
	// Client Connection via Socket class
	private Socket connection;
	
	public WebServer(Socket c) {
		connection = c;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			ServerSocket serverSocket = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + "...\n");
			// We listen until user halts server execution
			while(true) {
				WebServer webServer = new WebServer(serverSocket.accept());
				if(verbose) {
					System.out.println("Connection opened." + new Date() + ")");
				}
				
				// create dedicated thread to morage the client connection
				Thread thread = new Thread(webServer);
				thread.start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			System.err.println("Server Connection error : " + e.getMessage());
		}

	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;
		String fileRequested = null;
		
		try {
			//We read characters from the client via input stream the socket
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			// we get characters output stream to client(for header)
			out = new PrintWriter(connection.getOutputStream());
			// get binary output stream to client (for requester data)
			dataOut = new BufferedOutputStream(connection.getOutputStream());
			
			// get first line of the requester from client
			String input = in.readLine();
			// we parse the request with a string  tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
//			System.out.println(method+"\n");
			// we get file requested 
			fileRequested = parse.nextToken().toLowerCase();
//			System.out.println(fileRequested+"\n");
			
			// we support only GET and HEAD methods, we check
			if(!method.equals("GET") && !method.equals("HEAD")) {
				if(verbose) {
					System.out.println("501 Not Implemented : " + method + "method.");
				}
				// we return the not supported file to the client
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int)file.length();
				String contentMimeType = "text/html";
				// read content to return to client
				byte[] fileData = readFileData(file, fileLength);
				// We send HTTP Header with data to client
				out.println("HTTP/1.1 501 Not Impemented");
				out.println("Server: Java HTTP Server from SSourel: 1.0");
				out.println("Date: " + new Date());
				out.println("Contect-type: " + contentMimeType);
				out.println("Contect-length: " + fileLength);
				out.println(); //blank line between header and content, very important!
				out.flush(); // flush character output stream buffer
				// file
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			}else {
				// GET or HEAD method
				if(fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}
				
				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int)file.length();
				String content = getContentType(fileRequested);
				
				if(method.equals("GET")) { //GET method so we return content
					byte[] fileData = readFileData(file, fileLength);
					// send HTTP Header
					out.println("HTTP/1.1 200 Ok");
					out.println("Server: Java HTTP Server from SSourel: 1.0");
					out.println("Date: " + new Date());
					out.println("Contect-type: " + content);
					out.println("Contect-length: " + fileLength);
					out.println(); //blank line between header and content, very important!
					out.flush(); // flush character output stream buffer
					
					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}
				
				if(verbose) {
					System.out.println("File" + fileRequested + "of Type" + content + "returned");
				}
			}
		}catch(FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (Exception e) {
				// TODO: handle exception
				System.err.println("Error with file not found exception : " + e.getMessage());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connection.close(); // we close socket connection
				
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}
			
			if(verbose) {
				System.out.println("Connection closed.\n");
			}
		}
		
	}
	
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if(fileIn != null) {
				fileIn.close();
			}
		}
		return fileData;
	}
	
	// return supported MINE Types
	private String getContentType(String fileRequested) {
		if(fileRequested.endsWith(".htm") || fileRequested.endsWith(".html")) {
			return "text/html";
		}else {
			return "text/plain";
		}
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int)file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		// send HTTP Header
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server from SSourel: 1.0");
		out.println("Date: " + new Date());
		out.println("Contect-type: " + content);
		out.println("Contect-length: " + fileLength);
		out.println(); //blank line between header and content, very important!
		out.flush(); // flush character output stream buffer
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
		if(verbose) {
			System.out.println("file" + fileRequested + "not found");
		}
	}
	
	
}
