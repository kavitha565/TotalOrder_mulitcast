import java.util.Comparator;
import java.io.Serializable;


public class Event implements Serializable, Comparator<Event>{
	long serialVersionUID = 1001626620L;
	int processNum;
	String processId;
	String eventId;
	boolean eventAck = false;
	int logicalTime;
	
	public int getProcessNum() {
		return processNum;
	}

	public void setProcessNum(int processNum) {
		this.processNum = processNum;
	}

	public String getProcessId() {
		return processId;
	}
	
    public void setProcessId(String processId) {
		this.processId = processId;
	}

    public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public boolean isEventAcked() {
		return eventAck;
	}
	public void setEventAcked(boolean eventAck) {
		this.eventAck = eventAck;
	}

	public int getLogicalTime() {
		return logicalTime;
	}

	public void setLogicalTime(int logicalTime) {
		this.logicalTime = logicalTime;
	}

	@Override
	public int compare(Event e1, Event e2) {
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
	}
	
	
}