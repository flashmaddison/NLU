import groovy.json.*;

import groovy.io.*;

def listIntents = [];
def intentCount = 0;
def phraseCount = 0;

def prodEonCoreIntents = "C:\\Users\\s21181\\Desktop\\ProdEonCore\\intents\\";

def dir = new File(prodEonCoreIntents);

def intentMap= [:];
def subIntentList = [:];
def trainingPhrases = [:];

def headerString = "Intent name,Sub-intent of,Training phrases";

File logFile = new File('C:\\Users\\s21181\\Desktop\\ProdEonCore extract.csv');
logFile.createNewFile();
logFile.append(headerString + "\n");

println(headerString); 

dir.eachFileRecurse (FileType.FILES) { file ->
  // list << file
	if(!file.name.contains("_usersays_en")) {
		intentCount++;
		
		// open file and log intent ID		
		def intentFile = prodEonCoreIntents + file.name;
		
		String fileContents = new File(intentFile).text
		
		def jsonSlurper = new JsonSlurper()
		def parsedJSON = jsonSlurper.parseText(fileContents);
		
		intentMap.put(parsedJSON.id, parsedJSON.name);
		if(parsedJSON.parentId) {
			subIntentList.put(parsedJSON.id, parsedJSON.parentId);
		}
	} else {
		def intentName = file.name.replaceAll("_usersays_en.json", "");
		def trainingString = "";
		
		// open phrases file
		def intentFile = prodEonCoreIntents + file.name;
		
		String fileContents = new File(intentFile).text
		
		def jsonSlurper = new JsonSlurper()
		def parsedJSON = jsonSlurper.parseText(fileContents);
		
		parsedJSON.data.text.each() {
			trainingString = trainingString + it[0] + "\n";
			phraseCount++;
		}
		
		trainingPhrases.put(intentName, trainingString);
	}
}

intentMap.each() {
	def thisLine = ""; 
	if(!subIntentList.getAt(it.key)) {
		// print normal intent		
		thisLine = it.value + "," + ",\"" + trainingPhrases.getAt(it.value.toString()) + "\"";
	} else {
		// print sub-intent
		def subIntentName = intentMap.getAt(subIntentList.getAt(it.key));
		thisLine = it.value + "," + subIntentName + ",\"" + trainingPhrases.getAt(it.value.toString()) + "\"";
	}
	//println(thisLine);
	
	//File logFile = new File('C:\\Users\\s21181\\Desktop\\ProdEonCore extract.csv');
	logFile.append(thisLine + "\n");
	
	println(it.value)
}

println " -- list complete! ${intentCount} intents trained"
println " -- ${phraseCount} training phrases"
