package moss.kernel.Scheduler;

import java.util.List;

import moss.kernel.MProcess;

public class MPrioritizedLotteryProcess extends MProcess implements IPrioritizedLotteryProcess{
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
	public void SetPriority(ProcessPriorityEnum priority) {
		m_processPriority = priority;
		
	}
	@Override
	public ProcessPriorityEnum GetPriority() {
		return m_processPriority;
	}

	//Private Fields
	private List<Integer> m_tickets;
	private ProcessPriorityEnum m_processPriority;
	
}