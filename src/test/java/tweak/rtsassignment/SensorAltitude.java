/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tweak.rtsassignment;

import com.rabbitmq.client.*;
import java.util.Random;

/**
 *
 * @author User
 */
public class SensorAltitude {
    
}

class AltitudeLogic implements Runnable {
    
    ConnectionFactory cf;
    boolean emergencyState;
    int emergencyCycle = 0;

    public AltitudeLogic(ConnectionFactory cf, boolean emergencyState) {
        this.cf = cf;
        this.emergencyState = emergencyState;
    }
    
    Random rand = new Random();
    String senderKey = "sensorToControl";
    
    @Override
    public void run() {
        //generate data
        String data = generateData();
        if(emergencyState){
            data = generateEmergencyData();
        }
        //send data
        sendData(senderKey, data);
        System.out.println("ALTITUDE SENSOR: DATA TRANSMITTED TO FCS. CODE: "+data);
    }
    
    public String generateData(){
        String data;
        
        int no = rand.nextInt(4);
        
        switch (no) {
            case 0:
                data = "ALT-tooHigh";
                break;
            case 1:
                data = "ALT-slightlyHigh";
                break;
            case 2:
                data = "ALT-tooLow";
                break;
            case 3:
                data = "ALT-slightlyLow";
                break;
            default:
                data = "ALT-ok";
                break;
        }
        return data;
    }
    
    public String generateEmergencyData(){
        String data;
        
        switch (emergencyCycle) {
            case 0:
                data = "ALT-emergency";
                emergencyCycle++;
                break;
            case 1:
                data = "LAND-open";
                emergencyCycle++;
                break;
            default:
                data = "SPD-land";
                break;
        }
        return data;
    }
    
    public void sendData(String key, String data){
        try(Connection con = cf.newConnection()){
            Channel ch = con.createChannel();
            ch.queueDeclare(key, false, false, false, null);
            ch.basicPublish("", key, false, null, data.getBytes());
        }
        catch (Exception e){
            System.out.println("ALTITUDE SENSOR: ERROR, NO CONNECTION TO FCS.");
        }
    }
}
