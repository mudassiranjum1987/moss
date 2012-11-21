package moss.kernel;

public interface ILotteryProcess {
	/*
	 * Set ticket number to a process
	 */
	void SetTicket(int ticket);
	/*
	 * Get ticket number of current process	
	 */
	int GetTicket();
}