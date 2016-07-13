package gov.uspto.bulkdata.corpusbuilder;

import java.io.IOException;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.uspto.bulkdata.PatternMatcher;
import gov.uspto.bulkdata.PatternXPath;
import gov.uspto.patent.PatentReaderException;
import gov.uspto.patent.PatentType;
import gov.uspto.patent.model.classification.Classification;
import gov.uspto.patent.model.classification.ClassificationType;
import gov.uspto.patent.model.classification.CpcClassification;
import gov.uspto.patent.model.classification.UspcClassification;

/**
 * Match Patents by only looking at Classifications stored in each Patent's XML.
 * 
 * @author Brian G. Feldman (brian.feldman@uspto.gov)
 *
 */
public class MatchClassificationXPath implements CorpusMatch<MatchClassificationXPath> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MatchClassificationXPath.class);

	private final List<Classification> wantedClasses;
	private PatternMatcher matcher;
	private String xmlDocStr;

	private PatentType patentType;

	public MatchClassificationXPath(List<Classification> wantedClasses){
		this.wantedClasses = wantedClasses;
	}

	@Override
	public void setup() throws XPathExpressionException{
		matcher = new PatternMatcher();

		List<Classification> cpcClasses = Classification.getByType(wantedClasses, ClassificationType.CPC);
		for (Classification cpcClass : cpcClasses) {
			String CPCXpathStr = buildCPCxPathString((CpcClassification) cpcClass);
			LOGGER.debug("CPC xPath: {}", CPCXpathStr);
			PatternXPath CPC = new PatternXPath(CPCXpathStr);
			matcher.add(CPC);
		}

		List<Classification> uspcClasses = Classification.getByType(wantedClasses, ClassificationType.USPC);
		for (Classification uspcClass : uspcClasses) {
			String UspcXpathStr = buildUSPCxPathString((UspcClassification) uspcClass);
			LOGGER.debug("USPC xPath: {}", UspcXpathStr);
			PatternXPath USPC = new PatternXPath(UspcXpathStr);
			matcher.add(USPC);
		}
	}

	@Override
	public MatchClassificationXPath on(String xmlDocStr, PatentType patentType) {
		this.xmlDocStr = xmlDocStr;
		this.patentType = patentType;
		return this;
	}

	@Override
	public boolean match() {
		return matcher.match(xmlDocStr);
	}

	@Override
	public String getLastMatchPattern() {
		return matcher.getLastMatchedPattern().toString();
	}	

	/**
	 * 
	 * Note matches on Patent Classification as well any Cited Patent Classifications (Citations are only publicly available within Grants).
	 * 
	 * @param uspcClass
	 * @return
	 * @throws XPathExpressionException
	 */
	public String buildUSPCxPathString(UspcClassification uspcClass) throws XPathExpressionException {
		StringBuilder stb = new StringBuilder();
		stb.append("//classification-national/main-classification");
		stb.append("[starts-with(.,'").append(uspcClass.getMainClass()).append("')]");
		return stb.toString();
	}

	/**
	 * 
	 * Build XPath Expression for CPC Classification lookup.
	 * 
	 * "//classifications-cpc/main-cpc/classification-cpc[section/text()='H' and class/text()='04' and subclass/text()='N' and main-group[starts-with(.,'21')]]"
	 * 
	 * @param cpcClass
	 * @return
	 * @throws XPathExpressionException
	 */
	public String buildCPCxPathString(CpcClassification cpcClass) throws XPathExpressionException {

		StringBuilder stb = new StringBuilder();
		stb.append("//classifications-cpc/main-cpc/classification-cpc");
		stb.append("[");
		stb.append("section/text()='").append(cpcClass.getSection()).append("'");
		stb.append(" and ");
		stb.append("class/text()='").append(cpcClass.getMainClass()).append("'");
		stb.append(" and ");
		stb.append("subclass/text()='").append(cpcClass.getSubClass()).append("'");
		stb.append(" and ");
		stb.append("main-group[starts-with(.,'").append(cpcClass.getMainGroup()).append("')]");
		stb.append("]");

		return stb.toString();
	}

}