import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class MaintainOrder1 {
	public static final String processId = "P0";
	public static final int processNum = 0;
	public static int[] ports = {4444,5555,6666};
	public static String[] events = {"a", "b", "c"};
	private static final int NUM_PROCESSES = ports.length;
	public static volatile ArrayList<Event> eventBuffer = new ArrayList<>();
	public static volatile HashMap<String, HashSet<String>> acknowledgementBuffer = new HashMap<>();
	private static int logicalTime = 0;
	private static int applicationDeliveredEvents = 0;

	public static Comparator<Event> compareEvents = (e1, e2)->{
		if(e1.getLogicalTime() != e2.getLogicalTime()){
			if(e1.getLogicalTime() > e2.getLogicalTime()){
				return 1;
			}
		}else{
			if(e1.getProcessNum() > e2.getProcessNum()){
				return 1;
			}
		}

		return -1;
	};

	public static Thread createConnectionsThread  = (new Thread(){
		@Override
		public void run(){
			manageConnections();
			return;
		}
	});

	public static Thread orderedDeliveryThread = (new Thread(){
		@Override
		public void run(){
			orderedDelivery();
			return;
		}
	});

	
    static Thread eventAcknowledgementThread = (new Thread() {
        @Override
        public void run() {
            sendAcksForProcess();
            return;
        }
    });

	public Event createEvent(boolean acknowledgement,String eventId){
		Event event = new Event();
		event.setEventAcked(acknowledgement);
		event.setEventId(eventId);
		event.setLogicalTime(logicalTime);
		event.setProcessNum(processNum);
		event.setProcessId(processId);
		return event;
	}

	public void init(){

		System.out.println("Output of " + processId);

		createConnectionsThread.start();
		orderedDeliveryThread.start();
		eventAcknowledgementThread.start();
		
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		Event event = createEvent(false,events[processNum]);
		sendEvent(event);

	}

	public static void manageConnections(){
		try {
			ServerSocket server_soc = new ServerSocket(ports[processNum]);
			while(applicationDeliveredEvents != ports.length){
				Socket soc = server_soc.accept();
				ObjectInputStream ois = new ObjectInputStream(soc.getInputStream());
				Event event = (Event)ois.readObject();
				soc.close();
				storeEventDetails(event);
			}
			if(!server_soc.isClosed()){
				server_soc.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	public synchronized static void storeEventDetails(Event event){
			if(event.isEventAcked()){
				if(acknowledgementBuffer.containsKey(event.getEventId())){
					acknowledgementBuffer.get(event.getEventId()).add(event.getProcessId());
				}else{
					HashSet<String> processDetails = new HashSet<>();
                	processDetails.add(event.getProcessId());
                	acknowledgementBuffer.put(event.getEventId(), processDetails);
				}
			}else{
				eventBuffer.add(event);
				Collections.sort(eventBuffer, compareEvents);
			}
	}

	public static void sendAcksForProcess(){
		while(applicationDeliveredEvents != NUM_PROCESSES){
			if(!eventBuffer.isEmpty()){
				Event event = eventBuffer.get(0);
				if(canWeSendAckToThisGuy(event)){
					Event myEvent = new Event();
					myEvent.setEventAcked(true);
					myEvent.setEventId(event.getEventId());
					myEvent.setLogicalTime(logicalTime);
					myEvent.setProcessNum(processNum);
					myEvent.setProcessId(processId);
					sendEvent(myEvent);
					
					if(acknowledgementBuffer.containsKey(event.getEventId())){
						acknowledgementBuffer.get(event.getEventId()).add(processId);
					}else{
						HashSet<String> sender_set = new HashSet<>();
						sender_set.add(processId);
						acknowledgementBuffer.put(event.getEventId(), sender_set);
					}
				}
			}
		}
	}

	public static void orderedDelivery(){
		while(applicationDeliveredEvents != NUM_PROCESSES){
			if(!eventBuffer.isEmpty()){
				Event event = eventBuffer.get(0);
				if(acknowledgementBuffer.containsKey(event.getEventId())){
					if(acknowledgementBuffer.get(event.getEventId()).size() == NUM_PROCESSES){
						deliverEvent(event);
						eventBuffer.remove(0);
					}
				}
			}
		}
	}

	public static void sendEvent(Event event){
		try {
			if(!event.isEventAcked()){
				logicalTime += 1;
			}
			event.setLogicalTime(logicalTime);
			Socket socket;
			ObjectOutputStream obj_op;
			for(int port : ports){
				socket = new Socket("127.0.0.1",port);
				obj_op = new ObjectOutputStream(socket.getOutputStream());
				obj_op.writeObject(event);

				obj_op.close();
				socket.close();
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static boolean canWeSendAckToThisGuy(Event event){
		String myEvent = events[processNum];
		if(!acknowledgementBuffer.isEmpty() && acknowledgementBuffer.containsKey(event.getEventId())){
			if(acknowledgementBuffer.get(event.getEventId()).contains(MaintainOrder1.processId)){
				return false;
			}
		}
		if(event.getProcessNum() == processNum){
			return true;
		}
		if(acknowledgementBuffer.containsKey(myEvent) && acknowledgementBuffer.get(myEvent).size() == NUM_PROCESSES){
			return true;
		}
		if(logicalTime == event.getLogicalTime()){
			if(processNum > event.getProcessNum()){
				return true;
			}
		}else if(logicalTime > event.getLogicalTime()){
			return true;
		}

		return false;
	}

	public static void deliverEvent(Event event){
		System.out.println("Delivered: "+processId + ":" + event.getProcessId() + "." + event.getEventId());
		applicationDeliveredEvents += 1;
	}

	public static void main(String[] args) {
		MaintainOrder1 totalOrder = new MaintainOrder1();
		totalOrder.init();

		try {
			if(MaintainOrder1.createConnectionsThread != null){
				MaintainOrder1.createConnectionsThread.join();
			}
			if(MaintainOrder1.orderedDeliveryThread != null){
				MaintainOrder1.orderedDeliveryThread.join();
			}
			if(MaintainOrder1.eventAcknowledgementThread != null){
				MaintainOrder1.eventAcknowledgementThread.join();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Process "+MaintainOrder1.processId+" ended!");

	}

}