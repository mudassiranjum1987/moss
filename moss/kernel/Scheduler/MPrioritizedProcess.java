package moss.kernel.Scheduler;

import moss.kernel.MProcess;

public class MPrioritizedProcess extends MProcess implements IPriorityProcess {
	//Constructor
	public MPrioritizedProcess()
	{
		super();
		SetPriority(ProcessPriorityEnum.Medium);
	}
	public MPrioritizedProcess(MProcess parentProcess)
	{
		super(parentProcess);
		SetPriority(ProcessPriorityEnum.Medium);
	}
	
	//Public Methods
	@Override
	public void SetPriority(ProcessPriorityEnum processPriority) {
		priority = processPriority;
		
	}
	@Override
	public ProcessPriorityEnum GetPriority() {
		return priority;
	}
}
