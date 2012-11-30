package moss.modules;

import moss.user.*;
import moss.kernel.Scheduler.*;

/*
 * Base unit test class specifically for PrioritiedLottery scheduler.
 */
public abstract class UnitTestPrioritiedLotteryBase implements MUserProcess
{
	//Public Methods
	
	public int main (String argv[], MEnv envp)
	{
		int pid;
		
		if (argv.length == 3) {	//Child Process
			String processName = argv[1];
			String processPriority = argv[2];
			
			for (int i=0; i< getNumberOfProcesses(); i++) {
				for (int j=0; j<5; j++) {
					MPosixIf.writestring (MPosixIf.STDOUT, processName + " PID:" + MPosixIf.getpid() + " Priority:" + processPriority + " at " + i + " stage \n");
					MPosixIf.reschedule ();
				}
			}
			MPosixIf.writestring (MPosixIf.STDOUT, processName + " PID:" + MPosixIf.getpid() + " I'm done, exiting!\n");
		} else {	//Parent process
			for (int i=0; i<getNumberOfProcesses(); i++) {
				
				String processName = GetProcessName(i);
				ProcessPriorityEnum processPriority = GetProcessPriority(i);
				
				//Create processes
				pid = MPosixIf.forkexecc(argv[0], new String[] {argv[0], processName, processPriority.toString()});
				if (pid < 0) {
					MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " (" + MPosixIf.getpid() + "): failed to start instance of self\n");
					MPosixIf.exit (1);
				}
				
				//Set Process Priority
				Boolean isPrioritySet = MPosixIf.setPriority(pid, processPriority);
				if (!isPrioritySet) {
					MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " (" + MPosixIf.getpid() + "): failed to set priority\n");
					MPosixIf.exit (1);
				}
			}
			
			/* now wait for them (this is nicer than just exiting
			 * and letting the init-task scoop them up)
			 */
			for (int x=0; x<getNumberOfProcesses();) {
				int ra[];

				ra = MPosixIf.wait (false);
				if (ra != null) {
					x += (ra.length >> 1);
				}
			}
		}

		return 0;
	}


	/**
	 * signal handler
	 *
	 * @param signo signal number
	 * @param sigdata signal specific data
	 */
	public void signal (int signo, Object sigdata)
	{
		return;
	}

	//Protected Methods
	
	/*
	 * Total number of processes
	 */
	protected abstract int getNumberOfProcesses();
	/*
	 * Name of each process
	 */
	protected abstract String[] getProcessNameList();
	/*
	 * Priority of each process
	 */
	protected abstract ProcessPriorityEnum[] getProcessPriorityList();
	
	//Private Methods
	
	private String GetProcessName(int i) {
		return getProcessNameList()[i];
	}
	
	private ProcessPriorityEnum GetProcessPriority(int i) {
		return getProcessPriorityList()[i];
	}
}