/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tweak.rtsassignment;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author User
 */
public class ActuatorEngine {
    
}

class EngineLogic implements Runnable {
    
    ConnectionFactory cf;
    CountDownLatch cycleCount;
    CountDownLatch emergencyCount;

    public EngineLogic(ConnectionFactory cf, CountDownLatch cycleCount, CountDownLatch emergencyCount) {
        this.cf = cf;
        this.cycleCount = cycleCount;
        this.emergencyCount = emergencyCount;
    }
    
    String receiverKey = "controlToMachine";
    String machineKey = "SPD";
    
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
                    System.out.println("ENGINE: DATA RECEIVED FROM FCS: "+ data);
                    try{
                        Thread.sleep(500);
                    }
                    catch (Exception e) {}
                    adjustment(data);
                    cycleCount.countDown();
                }, x->{});
            }
            catch (Exception e){
                System.out.println("ENGINE: ERROR, CANNOT RECEIVE DATA FROM FCS.");
            }
        }
        catch(Exception e) {
            System.out.println("ENGINE: ERROR, NO CONNECTION TO FCS.");
        }
    }
    
    public void adjustment(String data){
        if(data.equals("tooFast"))
            System.out.println(("ENGINE: DECREASING ENGINE POWER."));
        else if(data.equals("tooSlow"))
            System.out.println(("ENGINE: INCREASING ENGINE POWER."));
        else if (data.equals("ok"))
            System.out.println(("ENGINE: NO ADJUSTMENT NEEDED."));
        else if (data.equals("land")){
            emergencyCount.countDown();
            System.out.println(("ENGINE: TURNING OFF ENGINE."));
        }  
        else if (data.equals("emergency")){
            System.out.println("ENGINE: ADJUSTING ENGINE FOR EMERGENCY LANDING");
            emergencyCount.countDown();
        }
            
    }
}