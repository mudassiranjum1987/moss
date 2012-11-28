package moss.kernel;

import java.io.Console;

import moss.kernel.Scheduler.IPreemtiveScheduler;
import moss.user.MPosixIf;

public class MPreemtiveProcessTimer extends Thread{
	//Construction
	public MPreemtiveProcessTimer(MProcess parentProcess) {
		m_parentProcess = parentProcess;
	}
	
	public void run(){
		
		while (true){
			//System.out.println("MPreemtiveProcessTimer Started Name:" + m_parentProcess.getName() + " ID:" + m_parentProcess.getId());
			try {
				sleep(getQuantum());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//synchronized (this) 
			{
				
				Boolean isAlive = false;
				isAlive = getIsAlive();
			
				synchronized (getParentProcess()) {		
					if (!isAlive) {
						//System.out.println("MPreemtiveProcessTimer Ending Name:" + m_parentProcess.getName() + " ID:" + m_parentProcess.getId());
						return;
					}
					getParentProcess().suspend();
				}
				
				synchronized (getParentProcess()) {
				//Reschedule here
				if (MKernel.getScheduler().IsProcessAvailable()){
					int cpu = MProcessor.currentCPU(getParentProcess());
					getParentProcess().syscall = "[resched]";
					MKernel.add_to_run_queue (getParentProcess());
					//Schedule Here
					MKernel.lock.claim_write ();
		
					if (MKernel.current[cpu] == null) {
						MKernel.panic ("MKernel::schedule().  current[cpu] is null!");
					}	
					
					/* pick a process off the run-queue */
					MProcess old_p = getParentProcess();
					MProcess new_p = MKernel.getScheduler().GetNextProcess();
					MKernel.processors[cpu].set_process (new_p);
					
					MKernel.current[cpu] = null;
		
					MKernel.lock.release_write ();
		
					if (new_p != old_p) {
						/* wake new (if not idling), sleep old */
						/*synchronized (old_p)*/ {
							if (new_p != null) {
								synchronized (new_p) {
									new_p.notify ();
								}
							}
							try {
								old_p.wait ();
								old_p.resume();
							} catch (InterruptedException e) {
								MKernel.panic ("MKernel::schedule().  interrupted: " + e.getMessage());
							}
						}
					}
		
					/* when a thread wakes up here, it is old_p */
					cpu = MProcessor.currentCPU(getParentProcess());
					MKernel.lock.claim_write ();
					MKernel.current[cpu] = old_p;
					/* ensure it is properly detached from any queue */
					old_p.state = MProcess.TASK_RUNNING;
					MKernel.lock.release_write ();
					//Schedule End Here
					
					MProcess.sync_process_signals (getParentProcess());
					getParentProcess().syscall = null;
				}
				else 
				{
					getParentProcess().resume();
				}
				}
				//end	
				}
				
			}
	}
	public synchronized Boolean getIsAlive() {
		return m_isAlive;
	}
	public synchronized void setIsAlive(Boolean isAlive) {
		m_isAlive = isAlive;
	}
	//Private Methods
	private MProcess getParentProcess(){
		return m_parentProcess;
	}
	private long getQuantum(){
		//return ((IPreemtiveScheduler)MKernel.getScheduler()).getQuantum();
		return 100;
	}
	
	//Private Fields
	private Boolean m_isAlive = true;
	private MProcess m_parentProcess;
}
