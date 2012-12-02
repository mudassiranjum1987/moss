package moss.kernel.Scheduler;

public enum ProcessPriorityEnum {
	High(100),
	Medium(50),
	Low(20);
	
	//Constructors
	ProcessPriorityEnum(int priorityValue) {
		m_priorityValue = priorityValue;
	}
	
	//Public Method
	public int getPriorityValue() {
		return m_priorityValue;
	}
	public void setPriorityValue(int priorityValue) {
		m_priorityValue = priorityValue;
	}
	
	//Private field
	private int m_priorityValue;
}
