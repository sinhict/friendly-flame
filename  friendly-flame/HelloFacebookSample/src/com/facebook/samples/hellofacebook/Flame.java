package com.facebook.samples.hellofacebook;

import android.graphics.Color;
import android.test.IsolatedContext;

//class to controll the status of the flame

/**
 * FRIENDLY FLAME COLOR MODES and codes:
 * 
 * green (G) = accepted
 * red (R) = declined
 * orange (O) = maybe (mixed from yellow and red LED)
 * yellow (Y) = outgoingness (brightness is changing depending on outgoingness)
 * 
 * @author Kahochan
 *
 */
public class Flame {
	
	//blue chip, red chip and white rfid tags
	String acceptRSP = "01005E0D7010";
    String declineRSP = "3C00CEB1F3C0";
    String maybeRSP = "0300B12C7030";
	
	//to calculate the outgoingness of a user
    public float[] calculateOutgoingness() {
    	
    	//outgoingness color is yellow
    	//intensity is defined by the outgoingness of the user
    	//and calculate from his events
    	float[] hsv = new float[3];
    	
    	//change yellow to HSV value
    	android.graphics.Color.RGBToHSV(255, 255, 0, hsv);
    	    	
    	int attended = 20;
    	int declined = 1;
    	int not_replied = 1;
    	int maybe = 1;
    	int all_events = 1;
    	
    	//calculate outgoingness of user
    	//formula:
    	// ((attended events - declined events - not replied/2)/all events * 2) + 0.5
    	//this formula also takes not replied events into accounts and
    	//give them a little penality
    	float outgoingness = ((attended - declined - (not_replied/2)) / all_events * 2) + 0.5f;
    	
    	//float outgoingness = 0.8f;
    	//calculate outgoingness by events;
    	// hsv[2] = outgoingness;
    	hsv[2] = outgoingness;
    	
    	String outgoingnessColor = changeHSVToString(hsv);
    	
    	//send color to Arduino and change login background color
    	changeColor(outgoingnessColor);
    	return hsv;
    }
    
    //method to change HSV color to string with format Y+colorValue 
    //colorValue lies between 0 and 255
    public String changeHSVToString(float[] color) {
    		float brightness = color[2];
    		double brightnessConverted = 0;
    		if (brightness == 1) {
    			brightnessConverted = 255;
    		}
    		else {
    			brightnessConverted = brightness * 256.0;
    		}
    			StringBuffer yellowColor = new StringBuffer("Y");
    			yellowColor.append(brightnessConverted);
    			return yellowColor.toString();
    }
    
    //method to send color changes to Arduino flame when no brightness is needed
    public void changeColor(String color) {
    	
    	//string format:
    	//first letter - Color (G, R, Y, O)
    	//second letter - brightness value (only for Y), e.g. Y128 -> Yellow with Brightness 128
    	
    	//TODO call method to send string to Arduino
    	
    }
    
    //method for processing RFID reader input for decisions
    //IDs:
    //blue chip -> accept
    //red chip -> decline
    //white card -> maybe
    
    public void rspEvent(String rfid) {
    	
    	//TODO receive input from Arduino, will be RFID ID tag
    	String decision = "";
    	
    	if (rfid.equals(acceptRSP)) {
    		decision = "accept";
    	}
    	if (rfid.equals(declineRSP)) {
    		decision = "decline";
    	}
    	if (rfid.equals(maybeRSP)) {
    		decision = "maybe";
    	}
    			
    	//TODO sent decision to FB event
    }
    
    

}