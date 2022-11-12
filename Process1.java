import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class Process1 {
	public static final String processId = "P1";
	public static final int processNum = 1;
	public static int[] ports = {7654,8765,9876};
	public static String[] events = {"event1","event2","event3"};
	public static volatile ArrayList<Event> eventBuffer = new ArrayList<>();
	public static volatile HashMap<String, HashSet<String>> acknowledgementBuffer = new HashMap<>();
	private static int logicalTime = 0;
	private static int applicationDeliveredEvents = 0;

	public static Comparator<Event> compareEvents = (e1, e2)->{
		return e1.getProcessNum() > e2.getProcessNum() ? 1: -1;
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

	public static Event createEvent(boolean acknowledgement,String eventId){
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
		
		Event event = createEvent(false,events[processNum-1]);
		sendEventToProcess(event);

	}

	public static void manageConnections(){
		try {
			ServerSocket server_soc = new ServerSocket(ports[processNum-1]);
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
		while(applicationDeliveredEvents != ports.length){
			if(!eventBuffer.isEmpty()){
				Event event = eventBuffer.get(0);
				if(checkAcknowledgement(event)){
					Event sendingEvent = createEvent(true,event.getEventId());
                    sendEventToProcess(sendingEvent);
					if(acknowledgementBuffer.containsKey(event.getEventId())){
						acknowledgementBuffer.get(event.getEventId()).add(processId);
					}else{
						HashSet<String> processDetails = new HashSet<>();
						processDetails.add(processId);
						acknowledgementBuffer.put(event.getEventId(), processDetails);
					}
				}
			}
		}
	}

	public static void orderedDelivery(){
        while(applicationDeliveredEvents != ports.length){
            if(!eventBuffer.isEmpty()){
                Event event = eventBuffer.get(0);
                if(acknowledgementBuffer.containsKey(event.getEventId())){
					if(acknowledgementBuffer.get(event.getEventId()).size() == ports.length){
						System.out.println(event.getProcessId() + ": " + event.getEventId());
		                applicationDeliveredEvents += 1;
						eventBuffer.remove(0);
					}
				}
            }
        }
	}

	public static void sendEventToProcess(Event event){
        try {
			if(!event.isEventAcked()) logicalTime += 1;
			event.setLogicalTime(logicalTime);
			Socket socket;
			ObjectOutputStream oos;
			for(int port : ports){
				socket = new Socket("127.0.0.1",port);
				oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(event);

				// close socket and output stream
				oos.close();
				socket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean checkAcknowledgement(Event event){
		if(!acknowledgementBuffer.isEmpty() && acknowledgementBuffer.containsKey(event.getEventId()) && acknowledgementBuffer.get(event.getEventId()).contains(processId)){
			return false;
		} else if(event.getProcessNum() == processNum){
			return true;
		} else if(acknowledgementBuffer.containsKey(events[processNum-1]) && acknowledgementBuffer.get(events[processNum-1]).size() == ports.length){
			return true;
		} else if(logicalTime == event.getLogicalTime() && processNum > event.getProcessNum()) {
			return true;
		} else if(logicalTime > event.getLogicalTime()){
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		Process1 totalOrder = new Process1();
		totalOrder.init();

		try {
			if(Process1.createConnectionsThread != null){
				Process1.createConnectionsThread.join();
			}
			if(Process1.orderedDeliveryThread != null){
				Process1.orderedDeliveryThread.join();
			}
			if(Process1.eventAcknowledgementThread != null){
				Process1.eventAcknowledgementThread.join();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Process "+Process1.processId+" ended!");

	}

}