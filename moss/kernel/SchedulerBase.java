package moss.kernel;

public abstract class SchedulerBase implements IScheduler {

	//Constructor

	public SchedulerBase() {
		// TODO Auto-generated constructor stub
	}

	//Public Methods

	@Override
	public Boolean AddProcess(MProcess process) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean RemoveProcess(MProcess process) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MProcess GetNextProcess() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean IsProcessAvailable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Boolean Schedule() {
		/* we enter this with the "current" Thread */
		MProcess old_p, new_p;
		int cpu = MProcessor.currentCPU ();

		MKernel.lock.claim_write ();

		if (MKernel.current[cpu] == null) {
			MKernel.panic ("MKernel::schedule().  current[cpu] is null!");
		}	
		if (!IsProcessAvailable()) {
			/* nothing else to run, make processor idle */
			old_p = MKernel.current[cpu];
			new_p = null;
			MKernel.processors[cpu].set_process (null);
		} else {
			/* pick a process off the run-queue */
			old_p = MKernel.current[cpu];
			new_p = GetNextProcess();
			MKernel.processors[cpu].set_process (new_p);
		}
		MKernel.current[cpu] = null;

		MKernel.lock.release_write ();

		if (new_p != old_p) {
			/* wake new (if not idling), sleep old */
			synchronized (old_p) {
				if (new_p != null) {
					synchronized (new_p) {
						new_p.notify ();
					}
				}
				try {
					old_p.wait ();
				} catch (InterruptedException e) {
					MKernel.panic ("MKernel::schedule().  interrupted: " + e.getMessage());
				}
			}
		}

		/* when a thread wakes up here, it is old_p */
		cpu = MProcessor.currentCPU ();
		MKernel.lock.claim_write ();
		MKernel.current[cpu] = old_p;
		/* ensure it is properly detached from any queue */
		old_p.state = MProcess.TASK_RUNNING;
		MKernel.lock.release_write ();
		return true;
	}
}
