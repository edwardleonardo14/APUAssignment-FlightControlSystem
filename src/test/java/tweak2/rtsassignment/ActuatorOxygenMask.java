/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tweak2.rtsassignment;

import tweak.rtsassignment.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author User
 */
public class ActuatorOxygenMask {
    
}

class MaskLogic implements Runnable {
    
    ConnectionFactory cf;
    CountDownLatch cycleCount;
    CountDownLatch emergencyCount;

    public MaskLogic(ConnectionFactory cf, CountDownLatch cycleCount, CountDownLatch emergencyCount) {
        this.cf = cf;
        this.cycleCount = cycleCount;
        this.emergencyCount = emergencyCount;
    }
    
    String receiverKey = "controlToMachine";
    String machineKey = "CAB";
    
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
                    System.out.println("MASK: DATA RECEIVED FROM FCS: "+ data);
                    try{
                        Thread.sleep(500);
                    }
                    catch (Exception e) {}
                    adjustment(data);
                    cycleCount.countDown();
                }, x->{});
            }
            catch (Exception e){
                System.out.println("MASK: ERROR, CANNOT RECEIVE DATA FROM FCS.");
            }
        }
        catch(Exception e) {
            System.out.println("MASK: ERROR, NO CONNECTION TO FCS.");
        }
    }
    
    public void adjustment(String data){
        if(data.equals("breach")){
            System.out.println(("MASK: CABIN PRESSURE LOSS. OXYGEN MASK DROPPING."));
            emergencyCount.countDown();
        }
        else if (data.equals("ok"))
            System.out.println(("MASK: NO ADJUSTMENT NEEDED."));
    }
}