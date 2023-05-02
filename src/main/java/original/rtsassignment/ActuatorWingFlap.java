/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package original.rtsassignment;

import com.rabbitmq.client.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author User
 */
public class ActuatorWingFlap {
    
}

class WingLogic implements Runnable {
    
    ConnectionFactory cf;
    CountDownLatch cycleCount;
    CountDownLatch emergencyCount;

    public WingLogic(ConnectionFactory cf, CountDownLatch cycleCount, CountDownLatch emergencyCount) {
        this.cf = cf;
        this.cycleCount = cycleCount;
        this.emergencyCount = emergencyCount;
    }

    String receiverKey = "controlToMachine";
    String machineKey = "ALT";
    
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
                    System.out.println("WING: DATA RECEIVED FROM FCS: "+ data);
                    try{
                        Thread.sleep(500);
                    }
                    catch (Exception e) {}
                    adjustment(data);
                    cycleCount.countDown();
                }, x->{});
            }
            catch (Exception e){
                System.out.println("WING: ERROR, CANNOT RECEIVE DATA FROM FCS.");
            }
        }
        catch(Exception e) {
            System.out.println("WING: ERROR, NO CONNECTION TO FCS.");
        }
    }
    
    public void adjustment(String data){
        if(data.equals("tooHigh"))
            System.out.println(("WING: RAISING FLAP."));
        else if(data.equals("slightlyHigh"))
            System.out.println(("WING: RAISING FLAP SLIGHTLY."));
        else if (data.equals("tooLow"))
            System.out.println(("WING: LOWERING FLAP."));
        else if (data.equals("slightlyLow"))
            System.out.println(("WING: LOWERING FLAP SLIGHTLY."));
        else if (data.equals("emergency")){
            System.out.println(("WING: RAISING FLAP FOR EMERGENCY."));
            emergencyCount.countDown();
        }
        else if (data.equals("ok"))
            System.out.println(("WING: NO ADJUSTMENT NEEDED."));
    }
}