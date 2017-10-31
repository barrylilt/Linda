/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package linda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;

public class LindaSpeechlet implements Speechlet {
	private static final Logger log = LoggerFactory.getLogger(LindaSpeechlet.class);

	/**
	 * The slots defined in Intent.
	 */
	private static final String SLOT_DRUG = "drug";
	
	private static final String SLOT_PHASE = "phase";
   
	private static final String SLOT_GENDERTYPE = "gendertype";
	
	private static final String SLOT_SPONSOR = "sponsor";
	
	private static final String SLOT_STATE = "state";
	
	private static final String SLOT_STUDYTYPE = "studytype";
		
	private static final String SLOT_STATUS = "status";
	
	private static final String SLOT_CONDITION = "condition";
	
	


	private ConnectionUtil connUtil;

	@Override
	public void onSessionStarted(final SessionStartedRequest request, final Session session) throws SpeechletException {
		log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

		initializeComponents();
	}

	private void initializeComponents() {
		if (connUtil == null) {
			connUtil = new ConnectionUtil();
		}
	}

	@Override
	public SpeechletResponse onLaunch(final LaunchRequest request, final Session session) throws SpeechletException {
		log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

		String speechOutput = "Good Morning Jane. How can I help you?";

		String repromptText = " ";

		// Here we are prompting the user for input
		return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true, false);
	}

	@Override
	public SpeechletResponse onIntent(final IntentRequest request, final Session session) throws SpeechletException {
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

		initializeComponents();

		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;
		
	   
		if ("goodmorning".equals(intentName)){
			return getGoodMorning(intent, session);
		}else if ("plate".equals(intentName)){
			return getPlate(intent, session);
		}else if ("confirmplate".equals(intentName)){
			return getConfirmation(intent, session);
		}else if ("summary".equals(intentName)) {
			return getSummary(intent, session);
		} else if ("chart".equals(intentName)) {
			return getChart(intent, session);
		} else if ("similarTrials".equals(intentName)) {
			return getSimilarTrials(intent, session);
		} else if ("extendTrend".equals(intentName)) {
			return getExtendTrend(intent, session);
		} else if ("HearMore".equals(intentName)) {
			return getMoreHelp();
		} else if ("DontHearMore".equals(intentName)) {
			PlainTextOutputSpeech output = new PlainTextOutputSpeech();
			output.setText("Thanks, Goodbye");
			return SpeechletResponse.newTellResponse(output);
		} else if ("AMAZON.HelpIntent".equals(intentName)) {
			return getHelp();
		} else if ("AMAZON.StopIntent".equals(intentName)) {
			PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
			outputSpeech.setText("Bye,  Hope to see you soon!");
			return SpeechletResponse.newTellResponse(outputSpeech);
		} else if ("AMAZON.CancelIntent".equals(intentName)) {
			PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
			outputSpeech.setText("Goodbye! ");
			return SpeechletResponse.newTellResponse(outputSpeech);
		} else if (intentName==null){
			String speechOutput="Can you please repeat";
			
			String repromptText="You can ask things like, "
					+ "Give me the number of total studies in phase one <break time=\"0.2s\" /> "
					+ "  <break time=\"0.2s\" /> " + " <break time=\"1s\" /> ";
								
			return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true, false);			
		}else {
			// Reprompt the user.
			String speechOutput = "Can you please repeat "+ " <break time=\"0.8s\" /> ";

			String repromptText = "You can ask things like, "
					+ "Give me the number of total studies in phase one <break time=\"0.5s\" /> "
					+ "  <break time=\"0.5s\" /> " + " <break time=\"0.5s\" /> ";
					
			return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true, false);

		}
		   
	}

	@Override
	public void onSessionEnded(final SessionEndedRequest request, final Session session) throws SpeechletException {
		log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

	}
	
		
	/**
	 * Creates Dynamic Query for Total Enrollment Count based on sponsor
	 * 
	 * @param sponsor 
	 * 
	 * @return String totalEnrollmentCount
	 * 
	 */
	private String executeEnrollmentCountQuery(String sponsor, String state) {
		String totalEnrollmentQuery="";
		// base Query 
		if (state==null) totalEnrollmentQuery = "select count(*) from ClinicalTrials_Dataset where Sponsor_or_Collaborators like '%" + sponsor + "%'";
		else if (state!=null ) totalEnrollmentQuery = "select count(*) from ClinicalTrials_Dataset where Sponsor_or_Collaborators like '%" + sponsor + "%' and State ='"+state+"'";
		// execute the query on server connection
		String totalEnrollmentCount = connUtil.executeQuery(totalEnrollmentQuery);

		return totalEnrollmentCount;

	}
	/**
	 * Creates Dynamic Query for Total Trials based on gender
	 * 
	 * @param gendertype 
	 * 
	 * @return String totalTrialsCount
	 * 
	 */
	private String executeTotalTrialsGenderQuery(String gendertype) {

		// base Query 
		String totalTrialsGenderQuery = "select count(*) FROM ClinicalTrials_Dataset where Gender='" + gendertype + "'";

		// execute the query on server connection
		String totalTrialsCount = connUtil.executeQuery(totalTrialsGenderQuery);

		return totalTrialsCount;
		
		
	}
	
	private String executeTotalTrialsCondition(String sponsor, String condition) {
        String totalTrialsQuery="";
		// base Query
               
        if(sponsor!=null && condition!=null)
        totalTrialsQuery="select count(*) from ClinicalTrials_Dataset where Conditions like '%" + condition + "%' and Sponsor_or_Collaborators like '%" + sponsor + "%'";
       
        else if(sponsor==null && condition!=null)
        totalTrialsQuery="select count(*) from ClinicalTrials_Dataset where Conditions like '%" + condition + "%'";
		// execute the query on server connection
		String totalTrialsCount = connUtil.executeQuery(totalTrialsQuery);

		return totalTrialsCount;

	}
	
	/**
	 * Creates Dynamic Query for Total Trials based on study type
	 * 
	 * @param studytype 
	 * 
	 * @return String totalTrialsCount
	 * 
	 */
	private String executeTotalTrialsQuery(String studytype, String sponsor, String condition) {
        String totalTrialsQuery="";
		// base Query
        
        if(sponsor==null && condition==null && studytype!=null )
	    totalTrialsQuery = "select count(*) from ClinicalTrials_Dataset where Study_Types='" + studytype + "'";
        else if(sponsor!=null && studytype!=null && condition==null)
        totalTrialsQuery = "select count(*) from ClinicalTrials_Dataset where Study_Types='" + studytype + "' and Sponsor_or_Collaborators like '%" + sponsor + "%'";            
        else if(sponsor!=null && studytype!=null && condition!=null)
        totalTrialsQuery="select count(*) from ClinicalTrials_Dataset where Conditions like '%" + condition + "%' and Sponsor_or_Collaborators like '%" + sponsor + "%' and Study_types = '" + studytype + "'";
		// execute the query on server connection
		String totalTrialsCount = connUtil.executeQuery(totalTrialsQuery);

		return totalTrialsCount;

	}
	
	
	 
	 /**
	 * Creates Dynamic Query for Total Studies based on phase
	 * 
	 * @param phase 
	 * 
	 * @return String totalStudiesCount
	 * 
	 */
	private String executeTotalStudiesQuery(String phase, String sponsor) {
        String totalStudiesQuery="";
		// base Query 		
		if(sponsor==null)
		totalStudiesQuery = "select count(*) FROM ClinicalTrials_Dataset where Phases='" + phase + "'";

		else if(sponsor!=null)
			totalStudiesQuery = "select count(*) FROM ClinicalTrials_Dataset where Phases='" + phase + "' and Sponsor_or_Collaborators like '%" + sponsor + "%'";	
		                        
		// execute the query on server connectionrj
		String totalStudiesCount = connUtil.executeQuery(totalStudiesQuery);

		return totalStudiesCount;

	}

	/**
	 * Creates Dynamic Query for Recruitment based on status
	 * 
	 * @param status
	 * 
	 * @return String recruitmentCount
	 * 
	 */

	private String executeRecruitmentQuery(String status, String sponsor) {
		String recruitmentQuery="";
		// base Query
		if(sponsor!=null) recruitmentQuery = "select count(*) from ClinicalTrials_Dataset where Recruitment like '"+ status +"%' and Sponsor_or_Collaborators like '%" + sponsor + "%'";
		else if (sponsor==null) recruitmentQuery = "select count(*) from ClinicalTrials_Dataset where Recruitment like '"+ status +"%'";

		String recruitmentCount = connUtil.executeQuery(recruitmentQuery);

		return recruitmentCount;

	}
	
	
	
	/**
	 * Creates a Dynamic Query for Total Enrollment Count based on the Sponsor, executes
	 * the Query and returns the result.
	 *
	 * @param intent
	 *            - the intent for the request
	 * @param session
	 * @return SpeechletResponse spoken and visual response for the given intent
	 * @throws SpeechletExxception
	 * 
	 */
		
	private SpeechletResponse getGoodMorning(final Intent intent, final Session session)
			throws SpeechletException {
		// Simple Display Card
				SimpleCard card = new SimpleCard();
				card.setTitle(":: Plate ::");
																		     
				String finalSpeechOut = "";

				String finalCardOut = "";

				finalSpeechOut = "You have several items today. <break time=\"0.2s\" /> <say-as interpret-as=\"cardinal\"> " + "Would you like me to review the top ones for Clinical Trials?"
						+ "</say-as>";

				finalCardOut = "You have several items today. Would you like me to review the top ones for Clinicaal Trials" ;

				card.setContent(finalCardOut);

				String repromptText = " ";

				return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
						true, card);
				
			}

	
	/**
	 * Creates a Dynamic Query for Total Trials based on the Gender, executes
	 * the Query and returns the result.
	 *
	 * @param intent
	 *            - the intent for the request
	 * @param session
	 * @return SpeechletResponse spoken and visual response for the given intent
	 * @throws SpeechletExxception
	 * 
	 */
		
	private SpeechletResponse getPlate(final Intent intent, final Session session)
			throws SpeechletException {
		// Simple Display Card
				SimpleCard card = new SimpleCard();
				card.setTitle(":: Plate ::");
								
				String answer = "You have several items today. Would you like me to review the top ones for Clinical Trials" ;

				String finalSpeechOut = "";

				String finalCardOut = "";
				
				finalSpeechOut = answer + "<break time=\"0.2s\" /> <say-as interpret-as=\"cardinal\"> " 
						+ "</say-as>";

				finalCardOut = answer;

				card.setContent(finalCardOut);

				String repromptText = " ";

				return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
						true, card);			
			}


	private SpeechletResponse getSimilarTrials(final Intent intent, final Session session)
			throws SpeechletException {
		// Simple Display Card
				SimpleCard card = new SimpleCard();
				card.setTitle(":: Milestones Trend ::");
				
				String drug = intent.getSlot(SLOT_DRUG) != null ? intent.getSlot(SLOT_DRUG).getValue() : "";
				String phase = intent.getSlot(SLOT_PHASE) != null ? intent.getSlot(SLOT_PHASE).getValue() : "";
				
				if(drug!=null && phase !=null){
					
				 String answer="OK. Showing now";					
				  				    
				 String  finalSpeechOut = "";

				String finalCardOut = "";
				
				finalSpeechOut = answer + "<break time=\"0.2s\" /> <say-as interpret-as=\"cardinal\"> " 
						+ "</say-as>";

				finalCardOut = answer ;

				card.setContent(finalCardOut);

				String repromptText = "";

				return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
						true, card);
				}
				else return getHelp();
			}
	/**
	 * Creates a Dynamic Query for Total Trials based on the Study Type, executes
	 * the Query and returns the result.
	 *
	 * @param intent
	 *            - the intent for the request
	 * @param session
	 * @return SpeechletResponse spoken and visual response for the given intent
	 * @throws SpeechletExxception
	 * 
	 */
	
	
	private SpeechletResponse getConfirmation(final Intent intent, final Session session)
			throws SpeechletException {
		// Simple Display Card
				SimpleCard card = new SimpleCard();
				card.setTitle(":: Clinical Trials Items ::");
									
			    String answer="Last night we got data for 6 of our 25 open studies."
			    		  +"In the new data we see one study which requires your immediate attention. Would you like a summary?";
													    
				String  finalSpeechOut = "";

				String finalCardOut = "";
				
				finalSpeechOut = answer + "<break time=\"0.2s\" /> <say-as interpret-as=\"cardinal\"> " 
						+ "</say-as>";

				finalCardOut = answer;

				card.setContent(finalCardOut);

				String repromptText = "";

				return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
						true, card);				
			}

	/**
	 * Creates a Dynamic Query for Total Studies based on the Phases, executes
	 * the Query and returns the result.
	 *
	 * @param intent
	 *            - the intent for the request
	 * @param session
	 * @return SpeechletResponse spoken and visual response for the given intent
	 * @throws SpeechletExxception
	 * 
	 */	
	
	private SpeechletResponse getSummary(final Intent intent, final Session session)
			throws SpeechletException {

		// Simple Display Card
		SimpleCard card = new SimpleCard();
		card.setTitle(":: Summary ::");
				      
		String answer="According to the study data update from EDC, "
		+"the study xyz-0123 indication for melonoma has timing classification which has changed from behind schedule to seriously behind schedule for the milestone CSR Final.It is estimated to be 3 months delayed.";
						
		String finalSpeechOut = "";

		String finalCardOut = "";
		
		finalSpeechOut = answer + "<break time=\"0.9s\" /> <say-as interpret-as=\"cardinal\"> " 
				+"Would you like to see the trend for the milestones on your TV?"
				+ "</say-as>";

		finalCardOut = answer;

		card.setContent(finalCardOut);

		String repromptText = " ";

		return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
				true, card);		
	}

	/**
	 * Creates a Dynamic Query for Recruitment based on the
	 * Status(Withdrawn/Completed etc.. ), executes the Query and returns the
	 * result
	 *
	 * @param intent
	 *            the intent for the request
	 * @param session
	 * @return SpeechletResponse spoken and visual response for the given intent
	 * @throws SpeechletException
	 * 
	 */
	private SpeechletResponse getChart(final Intent intent, final Session session)
			throws SpeechletException {

		// Simple Display Card
		SimpleCard card = new SimpleCard();
		card.setTitle(":: Milestone Trend Chart :: ");

		String drug = intent.getSlot(SLOT_DRUG) != null ? intent.getSlot(SLOT_DRUG).getValue() : "";
			    
		if(drug!=null){
			String answer="";
			
			answer = "Sure. It is now displayed on your TV monitor."+
			"Would you also like to extend the trend across the  portfolio of similar trials? ";
					
		String finalSpeechOut = answer  + "<break time=\"0.8s\" /> <say-as interpret-as=\"cardinal\"> "
				 + "</say-as>";

		String finalCardOut = answer;

		card.setContent(finalCardOut);

		String repromptText = " ";
	    
		return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
				true, card);
	    }
	    else
	    	return getHelp();
	    			
		}

	private SpeechletResponse getExtendTrend(final Intent intent, final Session session)
			throws SpeechletException {

		// Simple Display Card
		SimpleCard card = new SimpleCard();
		card.setTitle(":: Trend ::");
				      
		String answer="I am now displaying the trend across 3 studies for compound XYZ-9876, all in Phase III, "
				+"which completed in the last 3 years.";	
						
		String finalSpeechOut = "";

		String finalCardOut = "";
		
		finalSpeechOut = answer + "<break time=\"0.9s\" /> <say-as interpret-as=\"cardinal\"> " 
				
				+ "</say-as>";

		finalCardOut = answer;

		card.setContent(finalCardOut);

		String repromptText = " ";

		return newAskResponse("<speak>" + finalSpeechOut + "</speak>", true, "<speak>" + repromptText + "</speak>",
				true, card);		
	}
	
	/**
	 * Instructs the user on how to interact with this skill.
	 */
	private SpeechletResponse getHelp() {

		String speechOutput = "Can you please repeat <break time=\"0.2s\" />";

		String repromptText = "Can you please repeat <break time=\"0.2s\" />";				

		return newAskResponse("<speak>" + speechOutput + "</speak>", true, "<speak>" + repromptText + "</speak>", true, false);
	}

	/**
	 * Provides more help on how to interact with this skill.
	 */
	private SpeechletResponse getMoreHelp() throws SpeechletException {

		String speechOutput = "Waiting for your query!";

		String repromptText = "Here is a samples question <break time=\"0.2s\" />"
				+ "Give me the number of active trials for a sponsored company <break time=\"0.3s\" /> ";
		
		

		// Here we are prompting the user for input
		return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true, false);
	}

	/**
	 * Wrapper for creating the Ask response from the input strings.
	 * 
	 * @param stringOutput
	 *            the output to be spoken
	 * @param isOutputSsml
	 *            whether the output text is of type SSML
	 * @param repromptText
	 *            the reprompt for if the user doesn't reply or is
	 *            misunderstood.
	 * @param isRepromptSsml
	 *            whether the reprompt text is of type SSML
	 * @param displayCard
	 *            the display text to be sent to device
	 * @return SpeechletResponse the speechlet response
	 */
	private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml, String repromptText,
			boolean isRepromptSsml, Card displayCard) {
		OutputSpeech outputSpeech, repromptOutputSpeech;
		if (isOutputSsml) {
			outputSpeech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
		} else {
			outputSpeech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
		}

		if (isRepromptSsml) {
			repromptOutputSpeech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) repromptOutputSpeech).setSsml(repromptText);
		} else {
			repromptOutputSpeech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
		}
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(repromptOutputSpeech);
		return SpeechletResponse.newAskResponse(outputSpeech, reprompt, displayCard);
	}

	/**
	 * Wrapper for creating the Ask response from the input strings.
	 * 
	 * @param stringOutput
	 *            the output to be spoken
	 * @param isOutputSsml
	 *            whether the output text is of type SSML
	 * @param repromptText
	 *            the reprompt for if the user doesn't reply or is
	 *            misunderstood.
	 * @param isRepromptSsml
	 *            whether the reprompt text is of type SSML
	 * @return SpeechletResponse the speechlet response
	 */
	private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml, String repromptText,
			boolean isRepromptSsml, boolean shouldEndSession) {
		OutputSpeech outputSpeech, repromptOutputSpeech;
		if (isOutputSsml) {
			outputSpeech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
		} else {
			outputSpeech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
		}

		if (isRepromptSsml) {
			repromptOutputSpeech = new SsmlOutputSpeech();
			((SsmlOutputSpeech) repromptOutputSpeech).setSsml(repromptText);
		} else {
			repromptOutputSpeech = new PlainTextOutputSpeech();
			((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
		}
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(repromptOutputSpeech);
		
		//added code for Session Handling 
		SpeechletResponse response=new SpeechletResponse();
		  response.setShouldEndSession(shouldEndSession);
		  response.setOutputSpeech(outputSpeech);
		  response.setReprompt(reprompt);
		  return response;
		
		  //previous code
		//return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
	}
}
