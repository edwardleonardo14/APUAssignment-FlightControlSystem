/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tweak2.rtsassignment;

import tweak.rtsassignment.*;
import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author User
 */
public class FlightControlSystem {
    public static void main(String[] args) {
        
        //determine the amount of cycle the simulation will done
        //it will be multiplied by 4, so all sensor will be done as per count below
        int count = 3;
        long startTime;
        long endTime;
        double elapsedTime;
        int core = Runtime.getRuntime().availableProcessors();
        
        ConnectionFactory cf = new ConnectionFactory();
        ScheduledExecutorService sensor = Executors.newScheduledThreadPool(core);
        ScheduledExecutorService emergency = Executors.newScheduledThreadPool(core);
        CountDownLatch cycleCount = new CountDownLatch(count*4);
        CountDownLatch emergencyCount = new CountDownLatch(6);
        
        startTime = System.currentTimeMillis();
        FCSLogic fcs = new FCSLogic(cf, cycleCount);
        WingLogic w = new WingLogic(cf, cycleCount, emergencyCount);
        TailLogic t = new TailLogic(cf, cycleCount, emergencyCount);
        EngineLogic e = new EngineLogic(cf, cycleCount, emergencyCount);
        MaskLogic m = new MaskLogic(cf, cycleCount, emergencyCount);
        LandingLogic l = new LandingLogic(cf, emergencyCount);
          
        new Thread(fcs).start();
        new Thread(w).start();
        new Thread(t).start();
        new Thread(e).start();
        new Thread(m).start();
        new Thread(l).start();

        sensor.scheduleAtFixedRate(new AltitudeLogic(cf, false), 1000, 5000, TimeUnit.MILLISECONDS);
        sensor.scheduleAtFixedRate(new CabinLogic(cf, false), 1500, 5000, TimeUnit.MILLISECONDS);
        sensor.scheduleAtFixedRate(new SpeedLogic(cf, false), 2000, 5000, TimeUnit.MILLISECONDS);
        sensor.scheduleAtFixedRate(new DirectionLogic(cf, false), 2500, 5000, TimeUnit.MILLISECONDS);
        
        while(true)
        {
            if(cycleCount.getCount() == 0){
                System.out.println("FCS: NORMAL SIMULATION ENDS.");
                sensor.shutdownNow();
                break;
            }
        }
        System.out.println("FCS: SIMULATING CABIN LOSS AND EMERGENCY LANDING.");
        emergency.scheduleAtFixedRate(new CabinLogic(cf, true), 0, 50000, TimeUnit.MILLISECONDS);
        emergency.scheduleAtFixedRate(new SpeedLogic(cf, true), 2000, 50000, TimeUnit.MILLISECONDS);
        emergency.scheduleAtFixedRate(new DirectionLogic(cf, true), 2000, 50000, TimeUnit.MILLISECONDS);
        emergency.scheduleAtFixedRate(new AltitudeLogic(cf, true), 2000, 10000, TimeUnit.MILLISECONDS);
        
        while(true)
        {
            if(emergencyCount.getCount() == 0){
                System.out.println("FCS: PLANE LANDED. SIMULATION ENDED");
                endTime = System.currentTimeMillis();
                elapsedTime = (double) (endTime-startTime)/1000;
                System.out.println("FCS: SIMULATION TIME: "+elapsedTime+" SEC.");
                System.out.println("FCS: SYSTEM SHUT DOWN.");
                System.exit(0);
            }
        }
    }
    
}

class FCSLogic implements Runnable {
    
    ConnectionFactory cf;
    CountDownLatch cycleCount;

    public FCSLogic(ConnectionFactory cf, CountDownLatch cycleCount) {
        this.cf = cf;
        this.cycleCount = cycleCount;
    }

    String receiverKey = "sensorToControl";
    String senderKey = "controlToMachine";
    @Override
    public void run() {
        System.out.println("FCS: SYSTEM INITIATED.");
        receiveData();
        try{
            cycleCount.await();
        }
        catch (Exception e) {}
    }
    
    public void receiveData(){
        try{
            Connection con = cf.newConnection();
            Channel ch = con.createChannel();
            ch.queueDeclare(receiverKey, false, false, false, null);
            try{
                ch.basicConsume(receiverKey, true, (x, msg)->{
                    
                    String message = new String(msg.getBody(),"UTF-8");
                    String[] decoded = dataDecoding(message);
                    try{
                        Thread.sleep(500);
                    }
                    catch (Exception e) {}
                    sendData(decoded[0], decoded[1]);
                }, x->{});
            }
            catch (Exception e){
                System.out.println("FCS: ERROR, CANNOT RECEIVE DATA FROM SENSOR.");
            }
        }
        catch (Exception e){
            System.out.println("FCS: ERROR, NO CONNECTION TO SENSOR.");
        }
        
    }
    
    public String[] dataDecoding(String original){
        String[] decoded = original.split("-");
        return decoded;
    }
    
    public void sendData(String key, String data){
        try(Connection con = cf.newConnection()){
            
            Channel chan = con.createChannel();
            chan.exchangeDeclare(senderKey, "topic");
            chan.basicPublish(senderKey, key, false, null, data.getBytes());
            if(key.equals("ALT"))
                System.out.println("FCS: DATA TRANSMITTED TO THE WING. DATA: "+data);
            else if(key.equals("CAB"))
                System.out.println("FCS: CABIN PRESSURE REPORTED: "+data);
            else if (key.equals("SPD"))
                System.out.println(("FCS: DATA TRANSMITTED TO THE ENGINE. DATA "+data));
            else if (key.equals("DRT"))
                System.out.println(("FCS: DATA TRANSMITTED TO THE TAIL. DATA "+data));
            else if (key.equals("LAND"))
                System.out.println(("FCS: DATA TRANSMITTED TO THE LANDING GEAR. DATA "+data));
        }
        catch (Exception e){}
    }
}
