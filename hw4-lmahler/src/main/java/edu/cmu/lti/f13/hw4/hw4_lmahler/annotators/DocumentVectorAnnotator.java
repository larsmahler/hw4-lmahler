package edu.cmu.lti.f13.hw4.hw4_lmahler.annotators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.fit.util.*;

import edu.cmu.lti.f13.hw4.hw4_lmahler.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_lmahler.typesystems.Token;


public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		
		//Count each token and its frequency
    String [] stringList  = doc.getText().toLowerCase().split(" ");
    Map<String, Integer> tokenCount = new HashMap<String, Integer>();
    for (int i = 0; i < stringList.length; i++) {
        int count = tokenCount.containsKey(stringList[i]) ? tokenCount.get(stringList[i]) : 0;
        tokenCount.put(stringList[i], count + 1);
    }

    //Create Token annotations and create a temp list of Tokens
    List<Token> tokenList = new ArrayList<Token>();
    for (Map.Entry<String, Integer> entry : tokenCount.entrySet()) {
      String term = entry.getKey();
      Integer freq = entry.getValue();
      //Create Token object
      Token token = new Token(jcas);
      token.setText(term);
      token.setFrequency(freq);
      token.addToIndexes(jcas);
      //Update tokenList
      tokenList.add(token);
    }
    
    //Update Document.tokenList
    FSList fsTokenList = FSCollectionFactory.createFSList(jcas, tokenList);
    doc.setTokenList(fsTokenList);

	}

}
