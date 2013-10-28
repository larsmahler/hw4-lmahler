package edu.cmu.lti.f13.hw4.hw4_lmahler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.util.XMLInputSource;

/**
 * Implements a vector space retrieval system using the UIMA framework. 
 * It calls the following components in order:
 * 1. DocumentReader (extracts the query id, "relevant" value, and text from each document).
 * 2. DocumentVectorAnnotator (creates word feature vectors - lemmatized term and frequency - for each document).
 * 3. RetrievalEvaluator (computes cosine similarity, ranks sentences based on cosine similarity, 
 * and evaluates performance using the MRR metric).
 */
public class VectorSpaceRetrieval {
	
	public static void main(String [] args) 
			throws Exception {
			
		String sLine;
		long startTime=System.currentTimeMillis();
		
		URL descUrl = VectorSpaceRetrieval.class.getResource("/descriptors/retrievalsystem/VectorSpaceRetrieval.xml");
	   if (descUrl == null) {
	      throw new IllegalArgumentException("Error opening VectorSpaceRetrieval.xml");
	   }
		// create AnalysisEngine		
		XMLInputSource input = new XMLInputSource(descUrl);
		AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(input);
		AnalysisEngine anAnalysisEngine = UIMAFramework.produceAnalysisEngine(desc);
		CAS aCas = anAnalysisEngine.newCAS();

	  URL docUrl = VectorSpaceRetrieval.class.getResource("/data/documents.txt");
    if (docUrl == null) {
       throw new IllegalArgumentException("Error opening data/documents.txt");
    }
		BufferedReader br = new BufferedReader(new InputStreamReader(docUrl.openStream()));
		while ((sLine = br.readLine()) != null)   {
			aCas.setDocumentText(sLine);
			anAnalysisEngine.process(aCas);
			aCas.reset();
		}
		br.close();
		br=null;
		anAnalysisEngine.collectionProcessComplete();
		anAnalysisEngine.destroy();	
		long endTime=System.currentTimeMillis();
		
		double totalTime=(endTime-startTime)/1000.0;
		System.out.println("Total time taken: "+totalTime);
		

	}

}
