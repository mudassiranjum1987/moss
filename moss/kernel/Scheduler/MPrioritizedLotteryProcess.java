package moss.kernel.Scheduler;

import java.util.List;

import moss.kernel.MProcess;

public class MPrioritizedLotteryProcess extends MProcess implements IPrioritizedLotteryProcess, IPriorityProcess {
	//Constructor
	public MPrioritizedLotteryProcess()
	{
		super();
		SetPriority(ProcessPriorityEnum.Medium);
	}
	public MPrioritizedLotteryProcess(MProcess parentProcess)
	{
		super(parentProcess);
		SetPriority(ProcessPriorityEnum.Medium);
	}
	
	//Public Methods
	@Override
	public void SetTickets(List<Integer> tickets) {
		m_tickets = tickets;
	}

	@Override
	public List<Integer> GetTickets() {
		return m_tickets;
	}
	
	@Override
	public void SetPriority(ProcessPriorityEnum processPriority) {
		priority = processPriority;
		
	}
	@Override
	public ProcessPriorityEnum GetPriority() {
		return priority;
	}

	//Private Fields
	private List<Integer> m_tickets;
	
}