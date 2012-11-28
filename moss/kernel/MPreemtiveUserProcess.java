package moss.kernel;


public class MPreemtiveUserProcess extends MProcess{
	//Constructor
	public MPreemtiveUserProcess()
	{
		super();
	}
	public MPreemtiveUserProcess(MProcess parentProcess)
	{
		super(parentProcess);
	}

	public synchronized void TimerStart() {
		if (m_threadTimer != null) {
			synchronized (m_threadTimer) {
				if (m_threadTimer.isAlive())
					m_threadTimer.setIsAlive(true);
				else
					{
						m_threadTimer = new MPreemtiveProcessTimer(this);
						m_threadTimer.setIsAlive(true);
						m_threadTimer.start();
					}
			}
		}
		else
		{
			m_threadTimer = new MPreemtiveProcessTimer(this);
			m_threadTimer.setIsAlive(true);
			m_threadTimer.start();
		}
	}
	
	public synchronized void TimerStop(){
		if (m_threadTimer != null) {
			if (m_threadTimer.isAlive()){
					m_threadTimer.setIsAlive(false);
			}
		}
	}
	
	//Private Fields
	MPreemtiveProcessTimer m_threadTimer;
}
