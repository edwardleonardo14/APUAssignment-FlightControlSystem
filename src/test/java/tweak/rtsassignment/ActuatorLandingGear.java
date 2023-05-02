/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tweak.rtsassignment;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author User
 */
public class ActuatorLandingGear {
    
}

class LandingLogic implements Runnable {
    
    ConnectionFactory cf;
    CountDownLatch emergencyCount;

    public LandingLogic(ConnectionFactory cf, CountDownLatch emergencyCount) {
        this.cf = cf;
        this.emergencyCount = emergencyCount;
    }
    
    String receiverKey = "controlToMachine";
    String machineKey = "LAND";
    
    @Override
    public void run() {
        receiveData();
    }
    
    public void receiveData(){
        try{
            Connection con = cf.newConnection();
            Channel ch = con.createChannel();
            
            ch.exchangeDeclare(receiverKey, "topic");
            
            String qName = ch.queueDeclare().getQueue();
            
            ch.queueBind(qName, receiverKey, machineKey);
            try {
                ch.basicConsume(qName, true, (x, msg)->{
                    String data = new String(msg.getBody(),"UTF-8");
                    System.out.println("LANDING GEAR: DATA RECEIVED FROM FCS: "+ data);
                    try{
                        Thread.sleep(500);
                    }
                    catch (Exception e) {}
                    adjustment(data);
                }, x->{});
            }
            catch (Exception e){
                System.out.println("LANDING GEAR: ERROR, CANNOT RECEIVE DATA FROM FCS.");
            }
        }
        catch(Exception e) {
            System.out.println("LANDING GEAR: ERROR, NO CONNECTION TO FCS.");
        }
    }
    
    public void adjustment(String data){
        if(data.equals("open")){
            System.out.println(("LANDING GEAR: OPENING LANDING GEAR."));
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {}
            System.out.println("LANDING GEAR: LANDING GEAR OPENED.");
            emergencyCount.countDown();
        }
            
    }
}