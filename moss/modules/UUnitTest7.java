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

public class UUnitTest7 implements MUserProcess{
	public int main (String argv[], MEnv envp){
	int numberOfProcessesInUnitTest=2;
	
	int pidA = MPosixIf.forkexecc ("/bin/processA", argv);
	int pidB = MPosixIf.forkexecc ("/bin/processB", argv);
	int pidC = MPosixIf.forkexecc ("/bin/processC", argv);
	
	MPosixIf.setPriority(pidA, ProcessPriorityEnum.Low);
	MPosixIf.setPriority(pidB, ProcessPriorityEnum.Medium);
	MPosixIf.setPriority(pidC, ProcessPriorityEnum.High);
	
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


