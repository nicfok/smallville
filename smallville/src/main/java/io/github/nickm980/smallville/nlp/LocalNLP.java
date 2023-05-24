package io.github.nickm980.smallville.nlp;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.CoreMap;
import simplenlg.features.Feature;
import simplenlg.features.Tense;
import simplenlg.framework.InflectedWordElement;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.WordElement;
import simplenlg.lexicon.XMLLexicon;
import simplenlg.realiser.english.Realiser;

public class LocalNLP implements NLPCoreUtils {

    private static StanfordCoreNLP pipeline;

    private static StanfordCoreNLP getPipeline() {
	if (pipeline == null) {
	    Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, parse, pos, lemma, ner");
	    pipeline = new StanfordCoreNLP(props);
	}

	return pipeline;
    }

    public static void preLoad() {
	getPipeline();
    }

    enum Verbs {
	GERUND("VBG"), VERB("V");

	private String token;

	Verbs(String token) {
	    this.token = token;
	}

	String getToken() {
	    return token;
	}
    }

    @Override
    public String convertToPastTense(String sentence) {
	return convertSentence(sentence);
    }

    @Override
    public String extractLastOccurenceOfName(String observation) {
	String input = getNamedEntities(observation);

	String[] lines = input.split("\n");
	String result = null;

	for (int i = lines.length - 1; i >= 0; i--) {
	    if (lines[i].startsWith("Name:")) {
		result = lines[i].substring(6).trim();
		break;
	    }
	}

	return result;
    }

    /**
     * Find the named entities (people and locations) from a text
     * 
     * @param text
     */
    public String getNamedEntities(String text) {
	Annotation annotation = new Annotation(text);

	getPipeline().annotate(annotation);

	List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
	StringBuilder sb = new StringBuilder();
	for (CoreMap sentence : sentences) {
	    List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
	    for (int i = 0; i < tokens.size(); i++) {
		String ner = tokens.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class);
		if (ner.equals("PERSON")) {
		    if (i < tokens.size() - 1
			    && tokens.get(i + 1).get(CoreAnnotations.NamedEntityTagAnnotation.class).equals("PERSON")) {

			sb.append("Name: ");
			while (i < tokens.size()
				&& tokens.get(i).get(CoreAnnotations.NamedEntityTagAnnotation.class).equals("PERSON")) {
			    sb.append(tokens.get(i).word() + " ");
			    i++;
			}
			sb.append("\n");
		    } else {
			sb.append("Name: " + tokens.get(i).word() + "\n");
		    }
		} else if (ner.equals("LOCATION")) {
		    sb.append("Location: " + tokens.get(i).word() + "\n");
		}
	    }
	}

	return sb.toString();
    }

    public String inflectStem(String verb) {
	XMLLexicon lexicon = new XMLLexicon();
	WordElement word = lexicon.getWord(verb, LexicalCategory.VERB);
	InflectedWordElement infl = new InflectedWordElement(word);
	infl.setFeature(Feature.TENSE, Tense.PAST);
	Realiser realiser = new Realiser(lexicon);
	String result = realiser.realise(infl).getRealisation();

	return result;
    }

    private String convertSentence(String sentence) {
	if (sentence.contains(" am ")) {
	    return sentence.replace(" am ", " was ");
	}

	sentence = sentence.replaceAll("this", "the").replace(".", "").replaceAll("\\s+", " ").trim();

	CoreDocument document = getPipeline().processToCoreDocument(sentence);
	String modified = "";

	CoreSentence coreSentence = document.sentences().get(0);
	SemanticGraph semanticGraph = coreSentence.dependencyParse();

	List<Integer> xcomp = new ArrayList<Integer>();

	List<SemanticGraphEdge> edges = semanticGraph.edgeListSorted();
	for (SemanticGraphEdge edge : edges) {
	    IndexedWord dependent = edge.getDependent();

	    if (edge.getRelation().getShortName().equals("xcomp")) {
		int index = dependent.index();
		xcomp.add(index);
	    }
	}

	for (CoreLabel tok : document.tokens()) {
	    String word = tok.word();
	    String tag = tok.tag();

	    if (tag.startsWith(Verbs.VERB.getToken()) && !xcomp.contains(tok.index())) {
		String lemma = new Sentence(word).lemma(0);

		if (tok.tag().equals(Verbs.GERUND.getToken())) {
		    modified += word + " ";
		    continue;
		}

		modified += inflectStem(lemma) + " ";
	    } else {

		modified += word + " ";
	    }
	}

	modified = modified
	    .replaceAll("will", "")
	    .replaceAll(" am ", "")
	    .replace(".", "")
	    .replaceAll("\\s+", " ")
	    .trim();
	return modified;
    }
}