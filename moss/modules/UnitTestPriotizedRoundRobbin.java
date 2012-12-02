package moss.modules;

import moss.Time.MProcessTiming;
import moss.kernel.Scheduler.ProcessPriorityEnum;
import moss.user.*;


/**
 * this class creates several instances of itself at different priorities, that
 * loop a few (10) thousand times printing out every 2000 loops.  If the priority
 * mechanism works, the higher-priority processes should complete before the
 * lower-priority ones even print out their first line.
 */

public class UnitTestPriotizedRoundRobbin implements MUserProcess{
	public int main (String argv[], MEnv envp){
	int numberOfProcessesInUnitTest=2;
	
	int pidA = MPosixIf.forkexecc ("/bin/processA", argv);
	int pidB = MPosixIf.forkexecc ("/bin/processB", argv);
	int pidC = MPosixIf.forkexecc ("/bin/processC", argv);
	
	
	/*
	 * Round robbin process can have the priority values from 1-100. 100 means high priorty and 1 is the lowest priority process.
	 * The values assigned by ProcessPriorityEnum doesn't matter (below we are assignin Medium). Only the PriorityValue will be consider
	 * in round robbin (This might be a bit stricky, I could implement it in much better way but I didn't want to change exsisting code 
	 * MPosixIf.setpriority etc). If you don't set the PriorityValue, following value will be assigned as per ProcessPriorityEnumType
	 * High = 100
	 * Medium = 50
	 * Low = 20
	 * 
	 * Each time a process is scheduled. Its priority will be decreased by 1 and in this way the high priorty process will not 
	 * starve the low priority processes.
	 */
	ProcessPriorityEnum ProcessAPriority = ProcessPriorityEnum.Medium;
	ProcessAPriority.setPriorityValue(40);
	
	ProcessPriorityEnum ProcessBPriority = ProcessPriorityEnum.Medium;
	ProcessBPriority.setPriorityValue(39);
	
	ProcessPriorityEnum ProcessCPriority = ProcessPriorityEnum.Medium;
	ProcessCPriority.setPriorityValue(38);
	
	MPosixIf.setPriority(pidA, ProcessAPriority);
	MPosixIf.setPriority(pidB, ProcessBPriority);
	MPosixIf.setPriority(pidC, ProcessCPriority);
	
	if (pidA < 0 || pidB <0 || pidC<0) {
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + "Failed to complete Unit Test." +"\n");
		MPosixIf.exit (1);
	}
			
	/* now wait for them (this is nicer than just exiting
	 * and letting the init-task scoop them up)
	 */
	for (int x=0; x<numberOfProcessesInUnitTest;) {
		int ra[];

		ra = MPosixIf.wait (false);
		if (ra != null) {
			x += (ra.length >> 1);
		}
	}
		MProcessTiming.resetGlobalTime();
		//MPosixIf.exit(0);
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

}


