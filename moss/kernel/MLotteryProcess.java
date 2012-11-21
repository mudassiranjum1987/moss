package moss.kernel;

public class MLotteryProcess extends MProcess implements ILotteryProcess{
	//Constructor
	public MLotteryProcess()
	{
		super();
	}
	public MLotteryProcess(MProcess parentProcess)
	{
		super(parentProcess);
	}
	
	//Public Methods
	@Override
	public void SetTicket(int ticket) {
		m_ticket = ticket;
	}

	@Override
	public int GetTicket() {
		return m_ticket;
	}

	//Private Fields
	private int m_ticket;
}
