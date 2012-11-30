package moss.kernel.Scheduler;

import java.util.List;


public interface IPrioritizedLotteryProcess {
	/*
	 * Set ticket number to a process
	 */
	void SetTickets(List<Integer> ticket);
	/*
	 * Get ticket number of current process	
	 */
	List<Integer> GetTickets();
}
