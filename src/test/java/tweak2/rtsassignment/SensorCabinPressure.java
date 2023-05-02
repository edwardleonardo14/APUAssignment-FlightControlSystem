/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tweak2.rtsassignment;

import tweak.rtsassignment.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Random;

/**
 *
 * @author User
 */
public class SensorCabinPressure {

}

class CabinLogic implements Runnable {
    
    ConnectionFactory cf;
    boolean emergencyState;

    public CabinLogic(ConnectionFactory cf, boolean emergencyState) {
        this.cf = cf;
        this.emergencyState = emergencyState;
    }

    Random rand = new Random();
    String senderKey = "sensorToControl";
    @Override
    public void run() {
        //generate data
        String data = "CAB-ok";
        if(emergencyState){
            data = "CAB-breach";
        }
        //send data
        sendData(senderKey, data);
        System.out.println("CABIN PRESSURE SENSOR: DATA TRANSMITTED TO FCS. CODE: "+data);
        
    }
    
    public void sendData(String key, String data){
        try(Connection con = cf.newConnection()){
            Channel ch = con.createChannel();
            ch.queueDeclare(key, false, false, false, null);
            ch.basicPublish("", key, false, null, data.getBytes());
        }
        catch (Exception e){
            System.out.println("CABIN PRESSURE SENSOR: ERROR, NO CONNECTION TO FCS.");
        }
    }
}