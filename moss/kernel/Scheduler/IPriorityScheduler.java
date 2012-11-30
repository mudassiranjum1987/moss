package moss.kernel.Scheduler;

public interface IPriorityScheduler {
	/*
	 * Sets the priority of specific process within scheduler list.
	 */
	Boolean setProcessPriority(int pid, ProcessPriorityEnum priority);
}
