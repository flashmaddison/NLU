import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import groovy.json.*;
//import java.util.*;
//import javax.swing.JFileChooser

/*  Show file chooser to pick the log file to analyse
JFileChooser chooser = new JFileChooser()
chooser.setCurrentDirectory(new java.io.File("."))
chooser.setDialogTitle("Select log file to process")
// Only allow selection of files
chooser.setFileSelectionMode(JFileChooser.FILES_ONLY)
// get the user action
int returnVal = chooser.showOpenDialog()
// if the user selects a valid file outcome
if(returnVal == JFileChooser.APPROVE_OPTION) {
   File selectFile = chooser.getSelectedFile()
   println(' -- Selected file: ' + selectFile.getAbsolutePath())
}
*/

// put the path to the log file here
// TODO make file chooser work OR integrate to Log Explorer API to fetch on demand?
def filepath = 'C:\\Users\\s21181\\Desktop\\downloaded-logs-20210818-104908.json';

String fileContents = new File(filepath).text

def jsonSlurper = new JsonSlurper()
def parsedJSON = jsonSlurper.parseText(fileContents);

// switch to "true" to write output to CSV file
generateFile = true;

println("Conversation #,Session ID, Request ID,Date,Time,Utterance,Intent")

def requestMap = [:];
def responseMap = [:];
def sentimentMap = [:];
def sessionMap = [:];
def timestampMap = [:]

def conversationMap = [:];

// attempt to extract useful info from each log entry
parsedJSON.each { 
    // process and print requests
	if(it.textPayload.startsWith("Dialogflow Request : ")) {
		// Request object text payload is JSON, so we can parse that reliably.
		def parsed_textPayload = jsonSlurper.parseText(it.textPayload.replaceAll("Dialogflow Request : ",""));
		def parsed_query_input = jsonSlurper.parseText(parsed_textPayload.query_input);
		def parsed_text = parsed_query_input.text.textInputs.text;
		def sessionID = parsed_textPayload.session;
		
		// add request ID and utterance to map
		def parsedInput = parsed_text[0].replaceAll(",",". ");
		requestMap.put(it.labels.request_id, parsedInput);
		sessionMap.put(it.labels.request_id, it.trace);
		timestampMap.put(it.labels.request_id, it.timestamp);
	}
	
	if(it.textPayload.startsWith("Dialogflow Response : ")) {
		//println it
		
		// for some reason, the Response object text payload isn't JSON, it's just plain-text. Argh!
		// This regex extracts proximate content to the required text key
		def parsedString = (it.textPayload =~ /intent_name: \"(.*)\"/)[0][1];
		//def parsedSentiment = (it.textPayload =~ /query_text_sentiment: \"(.*)\"/)[0][1];

		// add request ID and intent to map	
		parsedString = parsedString.replaceAll(",", ". ");
		parsedString = parsedString.replaceAll("\\n",". ");
		responseMap.put(it.labels.request_id, parsedString);
		sessionMap.put(it.labels.request_id, it.trace);	
	}
}

//Date todayDate = new Date();
Date todayDate = Calendar.getInstance().getTime();
DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh.mm.ss");
String strDate = dateFormat.format(todayDate);

def filename = strDate + " - NLU Utterance log.csv"

if(generateFile) {
	File logFile = new File('C:\\Users\\s21181\\Desktop\\', filename);
	logFile.createNewFile();
	logFile.append("Conversation #,Session ID,Request ID,Date,Time,Utterance,Intent\n");
}

sessionMap = sessionMap.sort { a, b -> a.value <=> b.value }

def firstValue = true
def previousSession = "";

def conversationNumber = 1;

sessionMap.each() {
	if(firstValue) {
		previousSession = it.value;
	}
	firstValue = false;
	
	if(it.value != previousSession) {
		conversationNumber++;
	}
	
	conversationMap.put(it.key, conversationNumber.toString());
	
	previousSession = it.value;
}

requestMap.each { 
	def lineItem = conversationMap.get(it.key) + "," + sessionMap.get(it.key) + "," + it.key + "," + timestampMap.get(it.key).substring(0,10) + "," + timestampMap.get(it.key).substring(11,19) + "," + it.value + "," + responseMap.get(it.key);
	//println(timestampMap.get(it.key).substring(11,19))
	def logDate = timestampMap.get(it.key).substring(0,10)
	println(lineItem);
	
	if(generateFile) {
		File logFile = new File('C:\\Users\\s21181\\Desktop\\', filename);		
		logFile.append(lineItem + "\n");
	}
}


println(" -- File processing completed!")
if(generateFile) {
  println(" -- Log file saved as " + filename);
} else {
  println(" -- Log file not saved, please change generateFile to true")
}
