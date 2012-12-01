package moss.kernel.Scheduler;

public interface IPrioritizedLotteryScheduler {
	void SetTicketsToAssign(ProcessPriorityEnum processPriority, int ticketCount);
}
