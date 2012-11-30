package moss.kernel.Scheduler;

public interface IPriorityProcess {
	/*
	 * Set process priority
	 */
	void SetPriority(ProcessPriorityEnum priority);
	
	/*
	 * Get process priority
	 */
	ProcessPriorityEnum GetPriority();
}
