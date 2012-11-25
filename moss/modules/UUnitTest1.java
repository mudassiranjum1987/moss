package moss.modules;

import moss.user.*;


/**
 * this class creates several instances of itself at different priorities, that
 * loop a few (10) thousand times printing out every 2000 loops.  If the priority
 * mechanism works, the higher-priority processes should complete before the
 * lower-priority ones even print out their first line.
 */

public class UUnitTest1 implements MUserProcess{
	public int main (String argv[], MEnv envp){
	int processFailIndicator=0;//Initialize
	
	processFailIndicator= MPosixIf.forkexecc ("/bin/processA", argv);
	processFailIndicator= MPosixIf.forkexecc ("/bin/processB", argv);
	
	
	if (processFailIndicator < 0) {
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + "Failed to complete Unit Test." +"\n");
		MPosixIf.exit (1);
	}
			
		MPosixIf.exit(0);
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


